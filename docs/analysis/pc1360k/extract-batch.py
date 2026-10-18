#!/usr/bin/env python3
"""Extract PC-1360K batch packets from decoded payloads or pcwav raw S1 bytes.

S1 framing/checksum rules were independently described from the author's
read-only pcwav PCWAV/Binary/S1Encode.pm and Common.pm. No firmware included.
"""
import argparse
import hashlib
import json
from pathlib import Path

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


def s1_packets(data, allow_legacy_tail=False):
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
            if not (allow_legacy_tail and data[cursor + 2] == checksum([255])):
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


def extract(inputs, raw_s1=False, allow_legacy_tail=False):
    blocks = {}
    for data in inputs:
        if raw_s1:
            packets = s1_packets(data, allow_legacy_tail)
        else:
            if len(data) % PACKET_SIZE:
                raise ValueError('decoded payload length must be a multiple of 4112')
            packets = (data[i:i + PACKET_SIZE] for i in range(0, len(data), PACKET_SIZE))
        for packet in packets:
            index, body, metadata = parse_packet(packet)
            if index in blocks and blocks[index][0] != body:
                raise ValueError(f'conflicting duplicate block {index}')
            blocks[index] = (body, metadata)
    if not blocks:
        raise ValueError('no blocks')
    return blocks


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('inputs', nargs='+', type=Path)
    parser.add_argument('--raw-s1', action='store_true', help='input is pcwav raw output, not decoded payload')
    parser.add_argument('--allow-legacy-tail', action='store_true', help='accept old pcwav encoder tail excluding partial data; default validates hardware tail')
    parser.add_argument('--selectors', nargs='+', type=int, choices=range(4), default=[0, 1], help='expected selectors; use 2 3 for the second capture')
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    try:
        blocks = extract([path.read_bytes() for path in args.inputs], args.raw_s1, args.allow_legacy_tail)
        if len(args.selectors) != len(set(args.selectors)):
            raise ValueError('duplicate expected selector')
        unexpected = sorted({metadata['selector'] for _, metadata in blocks.values()} - set(args.selectors))
        if unexpected:
            raise ValueError(f'unexpected selectors {unexpected}; specify --selectors explicitly')
        expected_indices = [selector * 16 + offset for selector in args.selectors for offset in range(16)]
        outputs = {}
        for index, (body, metadata) in blocks.items():
            selector, address = metadata['selector'], metadata['address']
            outputs[f'read-s{selector}-{address:04x}-{address + 4095:04x}.bin'] = body
        for selector in args.selectors:
            indices = range(selector * 16, selector * 16 + 16)
            if all(index in blocks for index in indices):
                outputs[f'read-s{selector}-0000-ffff.bin'] = b''.join(blocks[index][0] for index in indices)
        missing = [index for index in expected_indices if index not in blocks]
        manifest = {'blocks': [blocks[index][1] for index in sorted(blocks)], 'missingIndices': missing,
                    'physicalRomAssignment': 'unknown', 'hardwareFOutput': 'not captured'}
        outputs['manifest.json'] = (json.dumps(manifest, indent=2) + '\n').encode()
        # Check every output before writing; never overwrite different existing captures.
        for name, content in outputs.items():
            path = args.output / name
            if path.exists() and path.read_bytes() != content:
                raise ValueError(f'refusing to overwrite different file: {path}')
        args.output.mkdir(parents=True, exist_ok=True)
        for name, content in outputs.items():
            (args.output / name).write_bytes(content)
        print(f'Validated {len(blocks)}/{len(expected_indices)} blocks; selectors: {args.selectors}; missing: {missing}')
        if missing:
            return 2
        print('Wrote complete 65536-byte selector images. Physical chip assignment remains unknown.')
        return 0
    except (OSError, ValueError) as error:
        parser.exit(1, f'{error}\n')


if __name__ == '__main__':
    raise SystemExit(main())
