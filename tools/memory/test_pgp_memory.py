"""Synthetic fixtures only; no ROM or user programs."""
import contextlib
import io
from pathlib import Path
import tempfile
import unittest

import pgp_memory as tool


class MemoryTests(unittest.TestCase):
    def test_round_trip_all_bytes_and_address_boundary(self):
        data = bytes(range(256))
        memory = tool.parse_dmp(tool.binary_to_dmp(data, 0xFF00))
        self.assertEqual(tool.ranges(memory), [(0xFF00, 0xFFFF)])
        self.assertEqual(tool.dmp_to_binary(memory), data)

    def test_full_address_space(self):
        data = bytes(range(256)) * 256
        self.assertEqual(tool.dmp_to_binary(tool.parse_dmp(tool.binary_to_dmp(data, 0))), data)

    def test_documented_input_variants(self):
        memory = tool.parse_dmp('\ufeff; comment\r9100:1112\r\n9000 00 01 02 03 # note\nA000 : FF EEFF\n9200 20 21 22 23:7A')
        self.assertEqual(tool.ranges(memory), [(0x9000, 0x9003), (0x9100, 0x9101), (0x9200, 0x9203), (0xA000, 0xA002)])
        self.assertEqual(memory[0xA001], 0xEE)

    def test_invalid_dmp(self):
        for text in ('', '# comment', '9000', '90000 00', '9000 0', '9000 GG',
                     '9000 01:XYZ', '9000 01:00:00', 'FFFF 0001', '9000 00\n9000 00'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                tool.parse_dmp(text)

    def test_gaps_require_explicit_fill(self):
        memory = tool.parse_dmp('9002 BB\n9000 AA')
        with self.assertRaises(ValueError):
            tool.dmp_to_binary(memory)
        self.assertEqual(tool.dmp_to_binary(memory, 0), b'\xaa\x00\xbb')
        self.assertEqual(tool.dmp_to_binary(memory, 255), b'\xaa\xff\xbb')

    def assert_hex_matches(self, text, expected):
        memory = {}
        records = text.splitlines()
        self.assertEqual(records[-1], ':00000001FF')
        for line in records[:-1]:
            record = bytes.fromhex(line[1:])
            self.assertEqual(sum(record) % 256, 0)
            count = record[0]
            self.assertEqual(len(record), count + 5)
            self.assertEqual(record[3], 0)
            self.assertLessEqual(count, 16)
            address = int.from_bytes(record[1:3], 'big')
            for offset, byte in enumerate(record[4:-1]):
                self.assertNotIn(address + offset, memory)
                memory[address + offset] = byte
        self.assertEqual(memory, expected)

    def test_intel_hex_known_record_and_checksums(self):
        self.assertEqual(tool.binary_to_hex(bytes([0, 1, 2, 3]), 0x9000),
                         ':049000000001020366\n:00000001FF\n')
        data = bytes(range(256))
        self.assert_hex_matches(tool.binary_to_hex(data, 0xFF00),
                                dict(enumerate(data, 0xFF00)))

    def test_intel_hex_full_space_and_sparse_dump(self):
        data = bytes(range(256)) * 256
        self.assert_hex_matches(tool.binary_to_hex(data, 0), dict(enumerate(data)))
        memory = tool.parse_dmp('FFFF FF\n9000 00 01\n9004 04')
        self.assert_hex_matches(tool.memory_to_hex(memory), memory)

    def test_invalid_hex_input(self):
        for data, start in ((b'', 0), (b'ab', 0xFFFF), (b'x', -1), (b'x', 65536)):
            with self.subTest(start=start), self.assertRaises(ValueError):
                tool.binary_to_hex(data, start)

    def test_hex_cli_and_overwrite_protection(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source, dump, target = root / 'old.bin', root / 'data.dmp', root / 'data.hex'
            source.write_bytes(bytes([0, 1, 2, 3]))
            with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                args = ['bin-to-hex', str(source), '--start', '0x9000', '-o', str(target)]
                self.assertEqual(tool.main(args), 0)
                expected = target.read_bytes()
                self.assertEqual(tool.main(args), 1)
                self.assertEqual(target.read_bytes(), expected)
                self.assertEqual(tool.main(['bin-to-dmp', str(source), '--start', '0x9000', '-o', str(dump)]), 0)
                self.assertEqual(tool.main(['dmp-to-hex', str(dump), '-o', str(target), '--force']), 0)
                self.assertEqual(target.read_bytes(), expected)
                source_before = source.read_bytes()
                self.assertEqual(tool.main(['bin-to-hex', str(source), '--start', '0', '-o', str(source), '--force']), 1)
                self.assertEqual(source.read_bytes(), source_before)
                dump.write_text('FFFF 00 01')
                self.assertEqual(tool.main(['dmp-to-hex', str(dump), '-o', str(target), '--force']), 1)
                self.assertEqual(target.read_bytes(), expected)

    def test_invalid_binary(self):
        for data, start in ((b'', 0), (b'ab', 0xFFFF), (bytes(65537), 0)):
            with self.assertRaises(ValueError):
                tool.binary_to_dmp(data, start)

    def test_cli_outputs_and_preserves_input(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / '旧 program.bin'
            target = root / '出力.dmp'
            result = root / 'round trip.bin'
            source.write_bytes(b'\x00\xff\x80')
            with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                args = ['bin-to-dmp', str(source), '--start', '0x9000', '-o', str(target)]
                self.assertEqual(tool.main(args), 0)
                original = target.read_bytes()
                self.assertEqual(tool.main(args), 1)
                self.assertEqual(target.read_bytes(), original)
                self.assertEqual(tool.main(args + ['--force']), 0)
                self.assertEqual(tool.main(['info', str(target)]), 0)
                self.assertEqual(tool.main(['dmp-to-bin', str(target), '-o', str(result)]), 0)
                self.assertEqual(result.read_bytes(), source.read_bytes())
                self.assertEqual(tool.main(['bin-to-dmp', str(source), '--start', '0', '-o', str(source), '--force']), 1)
                alias = root / 'alias.bin'
                alias.symlink_to(source)
                self.assertEqual(tool.main(['bin-to-dmp', str(source), '--start', '0', '-o', str(alias), '--force']), 1)
                self.assertEqual(source.read_bytes(), b'\x00\xff\x80')

    def test_failed_validation_does_not_touch_output(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / 'bad.dmp'
            target = Path(directory) / 'existing.bin'
            source.write_text('9000 ZZ')
            target.write_bytes(b'keep')
            with contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(tool.main(['dmp-to-bin', str(source), '-o', str(target), '--force']), 1)
            self.assertEqual(target.read_bytes(), b'keep')


if __name__ == '__main__':
    unittest.main()
