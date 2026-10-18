"""Synthetic capture tests; fixture framed by the read-only pcwav S1 encoder."""
import base64
import importlib.util
from pathlib import Path
import unittest
import json
import subprocess
import sys
import tempfile
import zlib

spec = importlib.util.spec_from_file_location("extract_batch", Path(__file__).with_name("extract-batch.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

# No firmware: data bytes are 00..FF repeated 16 times.
REFERENCE_FRAME = zlib.decompress(base64.b64decode(
    "eJxLZ4CAr44g8vwHAf7FW4SNkwVAPEaG/gIQLaBg4BCQUNAwYcGGAxcefGAUVDR0DEwsbJy4cOPBiw8/MgkpGTkFJRU1TVq06dClR5+YhZWNnYOTi5snL958+PLjzywiKiYuISklLVOWbDly5ckXVlFVU9fQ1NLWqUu3Hr369CubmJqZW1ha2ZO2acu2Hbv27Bu7uLq5e3h6efv05duPX3/+nUNCw8IjIqOiY8aKHSduvPjBKalp6RmZWdk5c+XOkzdf/uSS0rLyisqq6pq1atepW69+cUtrW3tHZ1d3z169+/Tt1795ZHRsfGJyanrmrNlz5s6bP7yyura+sbm1vXPX7j179+3fYj45PTu/uLy6vnnr9p279+4fv7y+vX98fn3//PX7z99//59WIXAePQjoFQK/0YOAXiGQQSgR0CoEUgglAlqFwAdSswG1QuANqdmAWiEwk9KCgNwQSKW0ICA3BIyoXRQSGwJ81C4KiQ2BKlpXBrhCII3WlQGuEJCmdWWAKwSu0bs6hIXAZnpXh7AQyKd3dQgLAR56V4ewEOCgd3UIC4EUeleHsBBIoHd1CAsBWXpXh7AQ4KR3dQgLgVJ6V4ewEJhI7+oQFgJ/6V0dwkJAi97VISwE0uhdHcJCYBG9q0NYCLyjd3UIC4Ff9K4OYSGQPmj7x///PwQAr/AS0g=="
))


LEGACY_FRAME = REFERENCE_FRAME
# For the synthetic 32-byte partial tail E0..FF (nibble-swapped), checksum with FF is ED (independently calculated by PCWAV::Common).
REFERENCE_FRAME = REFERENCE_FRAME[:-1] + bytes([0xed])


class ExtractBatchTest(unittest.TestCase):
    def test_reference_s1_frame_and_duplicate(self):
        blocks = module.extract([REFERENCE_FRAME + REFERENCE_FRAME], raw_s1=True)
        self.assertEqual(list(blocks), [0])
        self.assertEqual(blocks[0][0], bytes(range(256)) * 16)
        self.assertEqual(blocks[0][1]['sum'], 522240)

    def test_corruption_and_truncation_fail(self):
        damaged = bytearray(REFERENCE_FRAME)
        damaged[40] ^= 1
        for data in (damaged, REFERENCE_FRAME[:-3], REFERENCE_FRAME[:100]):
            with self.assertRaises(ValueError):
                module.extract([data], raw_s1=True)

    def test_partial_tail_is_checked_and_legacy_requires_opt_in(self):
        damaged = bytearray(REFERENCE_FRAME)
        damaged[-4] ^= 1
        with self.assertRaises(ValueError):
            list(module.s1_packets(damaged))
        with self.assertRaises(ValueError):
            list(module.s1_packets(LEGACY_FRAME))
        self.assertEqual(len(module.extract([LEGACY_FRAME], raw_s1=True, allow_legacy_tail=True)), 1)

    def test_all_32_packet_identities(self):
        first = list(module.s1_packets(REFERENCE_FRAME))[0]
        packets = []
        for index in range(32):
            packet = bytearray(first)
            packet[5] = index // 16
            packet[6:8] = ((index % 16) * 4096).to_bytes(2, 'little')
            packet[15] = index
            packets.append(packet)
        self.assertEqual(len(module.extract([b''.join(packets)])), 32)

    def test_selector_23_cli_combines_only_requested_selectors_and_reports_missing(self):
        first = list(module.s1_packets(REFERENCE_FRAME))[0]
        packets = []
        for index in range(32, 64):
            packet = bytearray(first)
            packet[5] = index // 16
            packet[6:8] = ((index % 16) * 4096).to_bytes(2, 'little')
            packet[15] = index
            packets.append(packet)
        self.assertEqual(sorted(module.extract([b''.join(packets)])), list(range(32, 64)))
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / 'capture.bin'
            source.write_bytes(b''.join(packets))
            script = str(Path(__file__).with_name('extract-batch.py'))
            result = subprocess.run([sys.executable, script, str(source), '--selectors', '2', '3', '--output', str(root / 'out')], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual((root / 'out/read-s2-0000-ffff.bin').read_bytes(), bytes(range(256)) * 256)
            self.assertEqual((root / 'out/read-s3-0000-ffff.bin').stat().st_size, 65536)
            self.assertFalse((root / 'out/read-s0-0000-ffff.bin').exists())
            self.assertEqual(json.loads((root / 'out/manifest.json').read_text())['missingIndices'], [])
            result = subprocess.run([sys.executable, script, str(source), '--output', str(root / 'wrong')], capture_output=True, text=True)
            self.assertEqual(result.returncode, 1)
            self.assertFalse((root / 'wrong').exists())
            source.write_bytes(b''.join(packets[:-1]))
            result = subprocess.run([sys.executable, script, str(source), '--selectors', '2', '3', '--output', str(root / 'partial')], capture_output=True, text=True)
            self.assertEqual(result.returncode, 2, result.stderr)
            self.assertEqual(json.loads((root / 'partial/manifest.json').read_text())['missingIndices'], [63])
            self.assertFalse((root / 'partial/read-s3-0000-ffff.bin').exists())
            self.assertTrue((root / 'partial/read-s2-0000-ffff.bin').exists())

    def test_unmeasured_selector_outside_four_is_rejected(self):
        packet = bytearray(list(module.s1_packets(REFERENCE_FRAME))[0])
        packet[5] = 4
        packet[15] = 64
        with self.assertRaises(ValueError):
            module.parse_packet(packet)

    def test_packet_sum_identity_and_conflicts_fail(self):
        first = list(module.s1_packets(REFERENCE_FRAME))[0]
        damaged = bytearray(first)
        damaged[50] ^= 1
        with self.assertRaises(ValueError):
            module.extract([damaged])
        damaged[10:13] = sum(damaged[16:]).to_bytes(3, 'little')
        with self.assertRaises(ValueError):
            module.extract([first, damaged])
        damaged = bytearray(first)
        damaged[15] = 1
        with self.assertRaises(ValueError):
            module.extract([damaged])


if __name__ == '__main__':
    unittest.main()
