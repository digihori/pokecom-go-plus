#!/usr/bin/env python3
"""Standalone raw binary / PGP DMP / Intel HEX converter (Python 3.10+, standard library only)."""
import argparse
from pathlib import Path
import re
import sys


def number(text):
    """Decimal by default; hexadecimal requires 0x (also accepts leading zeros)."""
    try:
        value = int(text, 16 if text.lower().startswith('0x') else 10)
    except ValueError:
        raise argparse.ArgumentTypeError('use decimal or 0x-prefixed hexadecimal')
    if not 0 <= value <= 0xFFFF:
        raise argparse.ArgumentTypeError('value must be in 0..65535')
    return value


def parse_dmp(text):
    memory = {}
    for line_no, original in enumerate(text.removeprefix('\ufeff').splitlines(), 1):
        line = re.split(r'[;#]', original, maxsplit=1)[0].strip()
        if not line:
            continue
        def fail(message):
            raise ValueError(f'line {line_no}: {message}')
        match = re.fullmatch(r'([0-9a-fA-F]{4})(?=\s|:)(.*)', line)
        if not match:
            fail('expected four hexadecimal address digits followed by whitespace or colon')
        start = int(match[1], 16)
        body = match[2].strip()
        if body.startswith(':'):
            body = body[1:].strip()
        if ':' in body:
            body, checksum = body.split(':', 1)
            if not re.fullmatch(r'[0-9a-fA-F]{2}', checksum.strip()):
                fail('optional checksum must contain two hexadecimal digits')
        compact = ''.join(body.split())
        if not compact or len(compact) % 2 or not re.fullmatch(r'[0-9a-fA-F]+', compact):
            fail('expected complete hexadecimal bytes')
        data = bytes.fromhex(compact)
        if start + len(data) > 0x10000:
            fail('data exceeds 0xFFFF')
        for offset, byte in enumerate(data):
            address = start + offset
            if address in memory:
                fail(f'overlapping address 0x{address:04X}')
            memory[address] = byte
    if not memory:
        raise ValueError('DMP contains no data')
    return memory


def ranges(memory):
    result = []
    for address in sorted(memory):
        if result and address == result[-1][1] + 1:
            result[-1] = (result[-1][0], address)
        else:
            result.append((address, address))
    return result


def validate_binary(data, start):
    if not 0 <= start <= 0xFFFF:
        raise ValueError('start address must be in 0..65535')
    if not data:
        raise ValueError('binary file is empty')
    if start + len(data) > 0x10000:
        raise ValueError('binary data exceeds 0xFFFF at the specified start address')


def binary_to_dmp(data, start):
    validate_binary(data, start)
    return ''.join(f'{start + offset:04X} ' + ' '.join(f'{b:02X}' for b in data[offset:offset + 16]) + '\n'
                   for offset in range(0, len(data), 16))


def memory_to_hex(memory):
    """Intel HEX data records (00) and EOF (01), within the 16-bit address space."""
    if not memory or any(not 0 <= address <= 0xFFFF or not 0 <= byte <= 255
                         for address, byte in memory.items()):
        raise ValueError('Intel HEX requires nonempty 16-bit addressed byte data')
    lines = []
    for start, end in ranges(memory):
        for address in range(start, end + 1, 16):
            data = bytes(memory[at] for at in range(address, min(address + 16, end + 1)))
            record = bytes([len(data), address >> 8, address & 255, 0]) + data
            lines.append(':' + record.hex().upper() + f'{(-sum(record)) & 255:02X}')
    return '\n'.join(lines) + '\n:00000001FF\n'


def binary_to_hex(data, start):
    validate_binary(data, start)
    return memory_to_hex({start + offset: byte for offset, byte in enumerate(data)})


def dmp_to_binary(memory, fill=None):
    spans = ranges(memory)
    if len(spans) > 1 and fill is None:
        raise ValueError('DMP has gaps; specify --fill explicitly to create a contiguous binary')
    return bytes(memory.get(address, fill) for address in range(spans[0][0], spans[-1][1] + 1))


def save_output(source, target, data, force):
    if source.resolve() == target.resolve() or (target.exists() and source.samefile(target)):
        raise ValueError('output must not refer to the input file')
    # Exclusive creation prevents accidental overwrite, including existing symlinks.
    with target.open('wb' if force else 'xb') as stream:
        stream.write(data)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    for name in ('bin-to-dmp', 'bin-to-hex', 'dmp-to-bin', 'dmp-to-hex', 'info'):
        command = commands.add_parser(name)
        command.add_argument('input', type=Path)
        if name != 'info':
            command.add_argument('-o', '--output', required=True, type=Path)
            command.add_argument('--force', action='store_true', help='overwrite an existing output (never the input)')
        if name in ('bin-to-dmp', 'bin-to-hex'):
            command.add_argument('--start', required=True, type=number, help='load address, e.g. 0x9000')
        if name == 'dmp-to-bin':
            command.add_argument('--fill', type=number, help='gap fill byte, e.g. 0xFF (0..255)')
    args = parser.parse_args(argv)
    try:
        if args.command in ('bin-to-dmp', 'bin-to-hex'):
            # Read at most one byte beyond the maximum permitted image size.
            with args.input.open('rb') as stream:
                data = stream.read(0x10001)
            writer = binary_to_dmp if args.command == 'bin-to-dmp' else binary_to_hex
            output = writer(data, args.start).encode('utf-8')
        else:
            memory = parse_dmp(args.input.read_text(encoding='utf-8-sig'))
            if args.command == 'info':
                spans = ranges(memory)
                print(f'Data: {len(memory)} bytes; contiguous ranges: {len(spans)}')
                for start, end in spans:
                    print(f'0x{start:04X}..0x{end:04X}: {end - start + 1} bytes')
                print(f'Gap bytes: {spans[-1][1] - spans[0][0] + 1 - len(memory)}')
                return 0
            if args.command == 'dmp-to-hex':
                output = memory_to_hex(memory).encode('utf-8')
            else:
                if args.fill is not None and args.fill > 255:
                    raise ValueError('--fill must be in 0..255')
                output = dmp_to_binary(memory, args.fill)
        save_output(args.input, args.output, output, args.force)
        print(f'Saved {args.output} ({len(output)} bytes)')
        return 0
    except (OSError, ValueError) as error:
        print(f'Error: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
