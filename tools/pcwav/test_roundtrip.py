"""Smoke-test bundled pcwav with synthetic data; requires Python 3.10+ and Perl."""
from pathlib import Path
import subprocess
import tempfile
import unittest

TOOLS = Path(__file__).resolve().parent


class RoundTripTests(unittest.TestCase):
    def test_modes(self):
        with tempfile.TemporaryDirectory(prefix='pcwav-test-') as directory:
            root = Path(directory)
            for mode in ('oldbasic', 's1basic', 's2basic', 'oldbin', 's1bin'):
                with self.subTest(mode=mode):
                    binary = mode.endswith('bin')
                    source = root / ('input.bin' if binary else 'input.bas')
                    source.write_bytes(bytes(range(256)) if binary else b'10 PRINT "TEST OK"\n20 END\n')
                    wav, output = root / 'test.wav', root / 'output'
                    extra = ['TEST', '0x9000'] if binary else ['TEST']
                    for script, args in (
                        ('pcwav-encode.pl', [mode, str(source), str(wav), *extra]),
                        ('pcwav-decode.pl', [mode, str(wav), str(output)]),
                    ):
                        result = subprocess.run(['perl', str(TOOLS / script), *args],
                                                cwd=root, capture_output=True, text=True)
                        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                    if binary:
                        self.assertEqual(output.read_bytes(), source.read_bytes())
                    else:
                        text = output.read_text(encoding='utf-8')
                        self.assertEqual(text, '10:PRINT "TEST OK"\n20:END\n')


if __name__ == '__main__':
    unittest.main()
