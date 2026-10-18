"""Synthetic tests only; no firmware, recordings or external tools required."""
import base64
import importlib.util
import json
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
import unittest
import wave
import zlib

ROOT = Path(__file__).parent
spec = importlib.util.spec_from_file_location("kanji_decode", ROOT / "decode.py")
decoder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(decoder)

# Independent PCWAV S1 encoder fixture: synthetic 00..FF repeated, with the
# hardware partial-tail checksum independently calculated as ED.
REFERENCE_FRAME = zlib.decompress(base64.b64decode(
    "eJxLZ4CAr44g8vwHAf7FW4SNkwVAPEaG/gIQLaBg4BCQUNAwYcGGAxcefGAUVDR0DEwsbJy4cOPBiw8/MgkpGTkFJRU1TVq06dClR5+YhZWNnYOTi5snL958+PLjzywiKiYuISklLVOWbDly5ckXVlFVU9fQ1NLWqUu3Hr369CubmJqZW1ha2ZO2acu2Hbv27Bu7uLq5e3h6efv05duPX3/+nUNCw8IjIqOiY8aKHSduvPjBKalp6RmZWdk5c+XOkzdf/uSS0rLyisqq6pq1atepW69+cUtrW3tHZ1d3z169+/Tt1795ZHRsfGJyanrmrNlz5s6bP7yyura+sbm1vXPX7j179+3fYj45PTu/uLy6vnnr9p279+4fv7y+vX98fn3//PX7z99//59WIXAePQjoFQK/0YOAXiGQQSgR0CoEUgglAlqFwAdSswG1QuANqdmAWiEwk9KCgNwQSKW0ICA3BIyoXRQSGwJ81C4KiQ2BKlpXBrhCII3WlQGuEJCmdWWAKwSu0bs6hIXAZnpXh7AQyKd3dQgLAR56V4ewEOCgd3UIC4EUeleHsBBIoHd1CAsBWXpXh7AQ4KR3dQgLgVJ6V4ewEJhI7+oQFgJ/6V0dwkJAi97VISwE0uhdHcJCYBG9q0NYCLyjd3UIC4Ff9K4OYSGQPmj7x///PwQAr/AS0g=="
))


LEGACY_FRAME = REFERENCE_FRAME
# For the synthetic 32-byte partial tail E0..FF (nibble-swapped), checksum with FF is ED (independently calculated by PCWAV::Common).
REFERENCE_FRAME = REFERENCE_FRAME[:-1] + bytes([0xed])

def packet(index):
    data = bytearray(next(decoder.s1_packets(REFERENCE_FRAME)))
    data[5] = index // 16
    data[6:8] = ((index % 16) * 4096).to_bytes(2, 'little')
    data[15] = index
    return data


def frame(data):
    result = bytearray(REFERENCE_FRAME[:19])
    raw = bytes(decoder.swap(v) for v in data)
    for start in range(0, len(raw), 120):
        chunk = raw[start:start + 120]
        result.extend(chunk)
        if len(chunk) == 120:
            result.append(decoder.checksum(chunk))
    result.extend([255, 255, decoder.checksum(raw[-32:] + b'\xff')])
    return bytes(result)


def wav_file(path, raw, width=1, channels=1, rate=8000, signal_channel=0):
    # Independent test transmitter using the PCWAV S1 wave specification.
    one, zero = b'\xff\x00' * 8, b'\xff\xff\x00\x00' * 4
    data = bytearray(one * 2048)
    for byte in raw:
        for part in (0, 4):
            data.extend(zero)
            for shift in range(part, part + 4):
                data.extend(one if byte & (1 << shift) else zero)
            data.extend(one * (1 if part == 0 else 5))
    if width == 2 or channels == 2:
        pcm = bytearray()
        for sample in data:
            for ch in range(channels):
                value = sample if ch == signal_channel else 128
                pcm.extend(bytes([value]) if width == 1 else struct.pack('<h', (value - 128) * 256))
        data = pcm
    with wave.open(str(path), 'wb') as audio:
        audio.setparams((channels, width, rate, 0, 'NONE', 'not compressed'))
        audio.writeframes(data)


class DecodeTests(unittest.TestCase):
    def test_independent_frame_and_hardware_tail(self):
        result = next(decoder.s1_packets(REFERENCE_FRAME))
        self.assertEqual(result[16:], bytes(range(256)) * 16)
        self.assertEqual(frame(result), REFERENCE_FRAME)
        for data in [LEGACY_FRAME, REFERENCE_FRAME[:-1], REFERENCE_FRAME[:100]]:
            with self.assertRaises(ValueError):
                list(decoder.s1_packets(data))
        changed = bytearray(REFERENCE_FRAME)
        changed[-4] ^= 1
        with self.assertRaises(ValueError):
            list(decoder.s1_packets(changed))
        changed = bytearray(REFERENCE_FRAME)
        changed[40] ^= 1
        with self.assertRaises(ValueError):
            list(decoder.s1_packets(changed))

    def test_complete_four_pages_out_of_order_and_safe_outputs(self):
        blocks = decoder.collect([b''.join(frame(packet(i)) for i in reversed(range(64)))], [0, 1, 2, 3])
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / 'roms'
            paths = decoder.write_images(blocks, [0, 1, 2, 3], output)
            self.assertEqual(len(paths), 4)
            for path in paths:
                self.assertEqual(path.read_bytes(), bytes(range(256)) * 256)
            metadata = json.loads((output / 'kanji-selector-3.json').read_text())
            self.assertEqual(metadata['size'], 65536)
            self.assertEqual(len(metadata['blocks']), 16)
            self.assertEqual(metadata['sum'], 8355840)
            decoder.write_images(blocks, [0, 1, 2, 3], output)
            paths[0].write_bytes(b'keep existing')
            with self.assertRaises(ValueError):
                decoder.write_images(blocks, [0, 1, 2, 3], output)
            self.assertEqual(paths[0].read_bytes(), b'keep existing')

    def test_missing_duplicate_unexpected_and_repeat_errors(self):
        frames = [frame(packet(i)) for i in range(16)]
        for raw, pages in [(b''.join(frames[:-1]), [0]),
                           (b''.join(frames + frames[:1]), [0]),
                           (b''.join(frames), [1]), (b''.join(frames), [0, 0])]:
            with self.assertRaises(ValueError):
                decoder.collect([raw], pages)
        for offset in [0, 5, 6, 8, 10, 13, 15, 50]:
            damaged = packet(0)
            damaged[offset] ^= 1
            with self.assertRaises(ValueError):
                decoder.parse_packet(damaged)

    def test_pcm8_pcm16_stereo_channel_and_truncated_wav(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'test.wav'
            for width, channels, channel in [(1, 1, 0), (2, 1, 0), (2, 2, 1)]:
                wav_file(path, REFERENCE_FRAME, width, channels, signal_channel=channel)
                raw = decoder.decode_wav(path, channel)
                self.assertIn(REFERENCE_FRAME, raw)
                self.assertEqual(next(decoder.s1_packets(raw)), packet(0))
                if channels == 2:
                    with self.assertRaises(ValueError):
                        list(decoder.s1_packets(decoder.decode_wav(path, 0)))
            path.write_bytes(path.read_bytes()[:-100])
            with self.assertRaises(ValueError):
                decoder.decode_wav(path, 1)
            wav_file(path, REFERENCE_FRAME, rate=48000)
            with self.assertRaisesRegex(ValueError, '16000 Hz'):
                decoder.decode_wav(path)

    def test_cli_full_page_and_failure_produces_no_output(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            source, output = root / 'page.wav', root / 'roms'
            # Full 64 KiB of synthetic data; no ROM fixture.
            wav_file(source, b''.join(frame(packet(i)) for i in range(32, 48)))
            args = [sys.executable, str(ROOT / 'decode.py'), str(source), '--pages', '2', '--output', str(output)]
            result = subprocess.run(args, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual((output / 'kanji-selector-2.bin').stat().st_size, 65536)
            bad = root / 'bad'
            result = subprocess.run(args[:-3] + ['3', '--output', str(bad)], capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse(bad.exists())

    def test_basic_keeps_verified_machine_code_and_page_loop(self):
        text = (ROOT / 'pc1360k-kanji-dump.bas').read_text()
        data = bytes(int(value.strip().removeprefix('&'), 16)
                     for line in text.splitlines() if ' DATA ' in line
                     for value in line.split(' DATA ')[1].split(','))
        self.assertEqual(len(data), 0x11c)
        import hashlib
        self.assertEqual(hashlib.sha256(data).hexdigest(), '20d81b9e210053978049655449faa00900505924482af9157a412e42d8d11005')
        self.assertIn('60 FOR K=&10*P TO &10*P+&F', text)
        for page in range(4):
            indices = range(16 * page, 16 * page + 16)
            self.assertEqual([(i // 16, (i % 16) * 4096) for i in indices],
                             [(page, address) for address in range(0, 65536, 4096)])


if __name__ == '__main__':
    unittest.main()
