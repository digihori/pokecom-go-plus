#!/usr/bin/env python3
"""Decode PC-1360K Kanji dump WAV recordings into checked 64 KiB page images.

Python 3.10+, standard library only. No ROM data or pcwav installation needed.
Signal algorithm independently adapted from the read-only digihori/pcwav
RawDecode.pm / PcmNormalize.pm, reference commit 14e1ef53596f0d8791b6d14965061f63846d11a7.
S1 packet rules reuse our earlier extract-batch.py; tail checksums follow the
PC-1360K hardware captures (partial data + first FF), without a legacy bypass.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import sys
import wave

PACKET_SIZE = 4112


def swap(value):
    return (value >> 4) | ((value & 15) << 4)


def checksum(values):
    result = 0
    for value in values:
        result += value & 15
        if result > 255:
            result = (result + 1) & 255
        result = (result + (value >> 4)) & 255
    return swap(result)


def s1_packets(data):
    """Find target S1 frames; verify all complete 120-byte chunk checksums."""
    offset = 0
    found = 0
    while offset + 19 <= len(data):
        if data[offset] != 0x67:
            offset += 1
            continue
        name = data[offset + 1:offset + 9]
        meta = data[offset + 10:offset + 18]
        if checksum(name) != data[offset + 9] or checksum(meta) != data[offset + 18]:
            offset += 1
            continue
        address = int.from_bytes(meta[4:6], 'big')
        length = int.from_bytes(meta[6:8], 'big') + 1
        if address != 0xcff0 or length != PACKET_SIZE:
            offset += 1
            continue
        cursor = offset + 19
        body = bytearray()
        partial = b''
        while len(body) < length:
            count = min(120, length - len(body))
            chunk = data[cursor:cursor + count]
            if len(chunk) != count:
                raise ValueError(f'truncated S1 body at {offset}')
            body.extend(swap(value) for value in chunk)
            cursor += count
            if count < 120:
                partial = chunk
            if count == 120:
                if cursor >= len(data) or data[cursor] != checksum(chunk):
                    raise ValueError(f'S1 chunk checksum mismatch at {cursor}')
                cursor += 1
        expected_tail = checksum(partial + bytes([255]))
        if data[cursor:cursor + 2] != bytes([255, 255]) or cursor + 2 >= len(data):
            raise ValueError(f'S1 footer missing at {cursor}')
        if data[cursor + 2] != expected_tail:
            raise ValueError(f'S1 tail checksum mismatch at {cursor + 2}')
        yield bytes(body)
        found += 1
        offset = cursor + 3
    if not found:
        raise ValueError('no 4112-byte CFF0 S1 records found')


def parse_packet(packet):
    if len(packet) != PACKET_SIZE or packet[:5] != b'K136\x01':
        raise ValueError('invalid batch packet size, magic or version')
    selector = packet[5]
    address = int.from_bytes(packet[6:8], 'little')
    length = int.from_bytes(packet[8:10], 'little')
    total = int.from_bytes(packet[10:13], 'little')
    differences = int.from_bytes(packet[13:15], 'little')
    index = packet[15]
    if selector not in (0, 1, 2, 3) or address % 4096 or length != 4096:
        raise ValueError('invalid selector, block address or length')
    if index != selector * 16 + address // 4096:
        raise ValueError('block index disagrees with selector/address')
    body = packet[16:]
    if differences != 0 or sum(body) != total:
        raise ValueError(f'block {index}: repeat mismatch or byte sum mismatch')
    return index, body, {'index': index, 'selector': selector, 'address': address,
                         'length': length, 'sum': total, 'differences': differences,
                         'sha256': hashlib.sha256(body).hexdigest()}


def decode_wav(path, channel=0, progress=None):
    """Demodulate in bounded PCM chunks; preserve timing state between chunks."""
    output = bytearray()
    with wave.open(str(path), 'rb') as audio:
        rate, channels, width, frames = (audio.getframerate(), audio.getnchannels(),
                                        audio.getsampwidth(), audio.getnframes())
        if audio.getcomptype() != 'NONE' or width not in (1, 2):
            raise ValueError(f'{path.name}: export WAV as unsigned 8-bit or signed 16-bit PCM')
        if channels not in (1, 2) or channel >= channels:
            raise ValueError(f'{path.name}: choose an existing channel (0=mono/left, 1=right)')
        if not 4000 <= rate <= 32000:
            raise ValueError(f'{path.name}: unsupported rate {rate}; export WAV at 16000 Hz')
        if not frames:
            raise ValueError(f'{path.name}: empty WAV')
        # Circular delay is equivalent to pcwav's shift register. The old z[tap]
        # contains the sample tap samples earlier, not tap+1 samples earlier.
        tap = max(1, int(rate / 4000 + 0.5))
        delay = [0] * tap
        position = 0
        level = count = sync_count = gap = data = 0
        syncing, bit = True, 1
        period = rate // 500
        consumed = 0
        next_report = rate * 60
        while consumed < frames:
            block = audio.readframes(min(65536, frames - consumed))
            if not block or len(block) % (width * channels):
                raise ValueError(f'{path.name}: truncated PCM data')
            count_frames = len(block) // (width * channels)
            if width == 1:
                samples = ((v - 128) * 256 for v in block[channel::channels])
            else:
                samples = (v[channel] for v in struct.iter_unpack('<' + 'h' * channels, block))
            for sample in samples:
                difference = sample - delay[position]
                delay[position] = sample
                position += 1
                if position == tap:
                    position = 0
                if difference > 5120 or difference < -5120:
                    level = count = 0
                else:
                    if count > 3:
                        level = 1
                    count += 1
                if syncing:
                    if gap:
                        gap -= 1
                    elif not level:
                        syncing = False
                        gap = int(period * 1.5)
                    else:
                        sync_count += 1
                        if sync_count > 1000 and bit == 16:
                            bit, data = 1, 0
                else:
                    gap -= 1
                    if gap <= 0:
                        gap = period
                        if level:
                            data |= bit
                        bit *= 2
                        if bit == 256:
                            syncing, sync_count = True, 0
                            output.append(data)
                            bit, data = 1, 0
                        if bit == 16:
                            syncing, sync_count = True, 0
            consumed += count_frames
            if progress and consumed >= next_report:
                progress(f'{path.name}: {consumed / frames:.0%}')
                next_report += rate * 60
    return bytes(output)


def collect(raw_inputs, pages):
    """Reject incomplete sets, duplicate blocks and inconsistent packet metadata."""
    if len(pages) != len(set(pages)):
        raise ValueError('duplicate requested page')
    blocks = {}
    for raw in raw_inputs:
        for packet in s1_packets(raw):
            index, body, metadata = parse_packet(packet)
            if metadata['selector'] not in pages:
                raise ValueError(f'unexpected page {metadata["selector"]}; check --pages')
            if index in blocks:
                raise ValueError(f'duplicate block {index}; supply each recording only once')
            blocks[index] = body, metadata
    missing = [index for page in pages for index in range(page * 16, page * 16 + 16)
               if index not in blocks]
    if missing:
        details = ', '.join(f'page {i // 16}:0x{(i % 16) * 4096:04X}' for i in missing)
        raise ValueError(f'missing blocks: {details}; no ROM files written')
    return blocks


def write_images(blocks, pages, output):
    files = {}
    for page in pages:
        entries = [blocks[i] for i in range(page * 16, page * 16 + 16)]
        image = b''.join(body for body, _ in entries)
        name = f'kanji-selector-{page}'
        files[name + '.bin'] = image
        manifest = {'selector': page, 'size': len(image), 'sum': sum(image),
                    'sha256': hashlib.sha256(image).hexdigest(),
                    'blocks': [metadata for _, metadata in entries],
                    'physicalRomAssignment': 'unknown'}
        files[name + '.json'] = (json.dumps(manifest, indent=2) + '\n').encode()
    # Validate all pages and existing destinations before writing anything.
    if output.exists() and not output.is_dir():
        raise ValueError(f'output is not a directory: {output}')
    for name, content in files.items():
        target = output / name
        if target.exists() and (not target.is_file() or target.read_bytes() != content):
            raise ValueError(f'refusing to overwrite different output: {target}')
    output.mkdir(parents=True, exist_ok=True)
    for name, content in files.items():
        target = output / name
        if not target.exists():
            # Exclusive creation also avoids accidentally replacing a concurrent file.
            with target.open('xb') as stream:
                stream.write(content)
    return [output / f'kanji-selector-{page}.bin' for page in pages]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('inputs', type=Path, nargs='+', help='recorded PCM WAV files')
    parser.add_argument('--pages', type=int, choices=range(4), nargs='+', default=[0, 1, 2, 3],
                        help='expected page numbers (default: 0 1 2 3)')
    parser.add_argument('--channel', type=int, choices=(0, 1), default=0,
                        help='mono/left=0 (default), right=1; stereo channels are not mixed')
    parser.add_argument('--output', required=True, type=Path, help='destination directory')
    args = parser.parse_args(argv)
    try:
        # Feed one recording at a time; never load long PCM recordings into memory.
        raws = (decode_wav(path, args.channel,
                           lambda message: print(message, file=sys.stderr, flush=True))
                for path in args.inputs)
        blocks = collect(raws, args.pages)
        paths = write_images(blocks, args.pages, args.output)
        for path in paths:
            print(f'OK {path}: 65536 bytes, SHA-256 {hashlib.sha256(path.read_bytes()).hexdigest()}')
        return 0
    except (OSError, ValueError, wave.Error, EOFError) as error:
        print(f'ERROR: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
