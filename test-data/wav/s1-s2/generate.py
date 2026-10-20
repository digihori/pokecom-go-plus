"""Record synthetic reference vectors; no ROM, recording, or product-code dependency.

Re-record: python3 test-data/wav/s1-s2/generate.py --pcwav ../pcwav
Verify committed vectors: python3 test-data/wav/s1-s2/generate.py --verify
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

REVISION = '14e1ef53596f0d8791b6d14965061f63846d11a7'
DESTINATION = Path(__file__).with_name('vectors.json')


def swap(value):
    return (value >> 4) | ((value & 15) << 4)


def checksum(data):
    total = 0
    for value in data:
        low = total + (value & 15)
        total = ((low + 1) & 255) if low > 255 else low
        total = (total + (value >> 4)) & 255
    return swap(total)


def signal(raw):
    high = bytes([255, 0]) * 8
    low = bytes([255, 255, 0, 0]) * 4
    output = bytearray(high * 2048)
    for value in raw:
        units = [0] + [(value >> bit) & 1 for bit in range(4)] + [1, 0]
        units += [(value >> bit) & 1 for bit in range(4, 8)] + [1] * 5
        for unit in units:
            output.extend(high if unit else low)
    return bytes(output)


def basic_record(number, statement):
    assert 1 <= number <= 65279 and len(statement) + 1 <= 255
    return number.to_bytes(2, 'big') + bytes([len(statement) + 1]) + statement + b'\r'


def cases():
    result = []
    for n in [1, 119, 120, 121, 239, 240, 241]:
        result.append(dict(id=f's1bin-{n}', kind='s1bin', name='TEST',
                           address=65535 if n == 1 else 16384,
                           logicalHex=bytes((i * 37 + 18) & 255 for i in range(n)).hex()))
    result.append(dict(id='s1bin-marker-bytes', kind='s1bin', name='TEST', address=0,
                       logicalHex='ffff0067072737f5ff0d'))
    for family in [1, 2]:
        token = b'\xde' if family == 1 else b'\xfe\x60'
        for n in [119, 120, 121, 239, 240, 241]:
            letters = 'A' * (n - len(token) - 6)
            statement = token + b'"' + letters.encode() + b'"'
            body = basic_record(10, statement)
            assert len(body) == n
            result.append(dict(id=f's{family}basic-{n}', kind=f's{family}basic', name='TEST',
                               source=f'10 PRINT "{letters}"\n', logicalHex=body.hex(),
                               type=7 if family == 1 else 39))
        text = 'ｱﾊﾟ' if family == 1 else 'ｱﾊﾟ漢字'
        encoded = text.encode('cp932')
        if family == 1:
            encoded = b''.join(bytes([254, b]) for b in encoded)
        result.append(dict(id=f's{family}basic-kana', kind=f's{family}basic', name='TEST',
                           source=f'10 PRINT "{text}"\n',
                           logicalHex=basic_record(10, token + b'"' + encoded + b'"').hex(),
                           type=7 if family == 1 else 39))
        rem = b'\xd7' if family == 1 else b'\xfe\x59'
        result.append(dict(id=f's{family}basic-rem-quote', kind=f's{family}basic', name='TEST',
                           source='10 REM PRINT "a b"\n20 PRINT "PRINT a b"\n',
                           logicalHex=(basic_record(10, rem + b'PRINT "a b"') +
                                       basic_record(20, token + b'"PRINT a b"')).hex(),
                           type=7 if family == 1 else 39))
    body = basic_record(10, bytes.fromhex('fe5541fe2b1f00642c1f00c8'))
    body += basic_record(100, bytes.fromhex('fe5a')) + basic_record(200, bytes.fromhex('fe2b1f0064'))
    result.append(dict(id='s2basic-on-list', kind='s2basic', name='TEST', type=39,
                       source='10 ON A GOTO 100,200\n100 END\n200 GOTO 100\n', logicalHex=body.hex()))
    then = basic_record(10, bytes.fromhex('fe56413d31fe541f0064')) + basic_record(100, bytes.fromhex('fe5a'))
    result.append(dict(id='s2basic-then', kind='s2basic', name='TEST', type=39,
                       source='10 IF A=1 THEN 100\n100 END\n', logicalHex=then.hex()))
    protected = dict(result[-1], id='s2basic-type37', type=55)
    result.append(protected)
    result.append(dict(id='signal-all-bytes', kind='signal', rawHex=bytes(range(256)).hex()))
    return result


def payload(case):
    body = bytes.fromhex(case['logicalHex'])
    raw_body = bytes(map(swap, body))
    if case['kind'] == 's1bin':
        chars = case['name'].upper().encode()[::-1][:6]
        name = (b'\0' + chars).ljust(7, b'\0') + b'\xf5'
        meta = b'\0' * 4 + case['address'].to_bytes(2, 'big') + (len(body) - 1).to_bytes(2, 'big')
        out = bytearray(b'\x67' + name + bytes([checksum(name)]) + meta + bytes([checksum(meta)]))
        full = len(body) // 120
    else:
        name = case['name'].encode().ljust(7, b'\0')[::-1] + b'\xf5'
        out = bytearray(bytes([case['type']]) + name + bytes([checksum(name)]))
        full = (len(body) - 1) // 120
    for offset in range(full):
        chunk = raw_body[offset * 120:(offset + 1) * 120]
        out.extend(chunk)
        out.append(checksum(chunk))
    tail = raw_body[full * 120:]
    out.extend(tail)
    out.extend([255, 255, checksum(tail + b'\xff')])
    return bytes(out)


PERL_REFERENCE = r'''
use strict; use warnings; use JSON::PP; use Digest::SHA qw(sha256_hex);
use PCWAV::Binary::S1Encode; use PCWAV::Basic::S1Encode; use PCWAV::Basic::S2Encode;
local $/; my $cases = JSON::PP->new->utf8->decode(<STDIN>); my @result;
for my $c (@$cases) {
    my $payload;
    if ($c->{kind} eq 'signal') { $payload = pack('H*', $c->{rawHex}); }
    elsif ($c->{kind} eq 's1bin') {
        my @body = unpack('C*', pack('H*', $c->{logicalHex}));
        $payload = pack('C*', PCWAV::Binary::S1Encode::build_payload(addr=>$c->{address}, name=>$c->{name}, bytes=>\@body));
    } elsif ($c->{kind} eq 's1basic') {
        $payload = PCWAV::Basic::S1Encode::encode_s1_basic_text($c->{source}, $c->{name});
    } else { $payload = PCWAV::Basic::S2Encode::encode_s2_basic_text($c->{source}, $c->{name}, $c->{type}); }
    my $stream = $payload . ($c->{kind} =~ /basic/ ? "\xff" : '');
    my $pcm = PCWAV::Binary::S1Encode::payload_to_pcm(unpack('C*', $stream));
    push @result, {rawHex=>unpack('H*', $payload), pcmSamples=>length($pcm), pcmSha256=>sha256_hex($pcm)};
}
print JSON::PP->new->utf8->canonical->encode(\@result);
'''


def verify(vectors):
    expected_cases = cases()
    assert vectors['schemaVersion'] == 1
    assert vectors['referenceRevision'] == REVISION
    assert len(vectors['cases']) == len(expected_cases)
    differences = []
    for saved, original in zip(vectors['cases'], expected_cases):
        assert saved['id'] == original['id']
        for key, value in original.items():
            assert saved[key] == value, (saved['id'], key)
        canonical = bytes.fromhex(original['rawHex']) if original['kind'] == 'signal' else payload(original)
        assert saved['expectedRawHex'] == canonical.hex(), saved['id']
        recorded = bytes.fromhex(saved['referenceRawHex'])
        if original['kind'] == 's1bin':
            assert canonical[:-1] == recorded[:-1], saved['id']
            assert recorded[-1] == checksum(b'\xff'), saved['id']
        else:
            assert recorded == canonical, saved['id']
        if recorded != canonical:
            differences.append(saved['id'])
        trailer = b'\xff' if 'basic' in original['kind'] else b''
        for prefix, data in [('reference', recorded), ('expected', canonical)]:
            pcm = signal(data + trailer)
            assert saved[prefix + 'PcmSamples'] == len(pcm), saved['id']
            assert saved[prefix + 'PcmSha256'] == hashlib.sha256(pcm).hexdigest(), saved['id']
    print(f"Verified {len(expected_cases)} synthetic vectors; legacy binary tail differences: {', '.join(differences)}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pcwav', type=Path)
    parser.add_argument('--verify', action='store_true')
    args = parser.parse_args()
    if args.verify:
        verify(json.loads(DESTINATION.read_text(encoding='utf-8')))
        return
    if args.pcwav is None:
        parser.error('--pcwav is required to record reference vectors')
    root = args.pcwav.resolve()
    revision = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
    if revision != REVISION:
        parser.error('Reference revision differs; review the specification before re-recording')
    dirty = subprocess.check_output(['git', '-C', str(root), 'status', '--porcelain', '--untracked-files=no', '--', 'src'], text=True)
    if dirty.strip():
        parser.error('Reference src has local changes; use an unmodified checkout of the recorded revision')
    originals = cases()
    reference = json.loads(subprocess.check_output(['perl', '-I', str(root / 'src'), '-e', PERL_REFERENCE],
                                                  input=json.dumps(originals, ensure_ascii=False).encode()))
    vectors = dict(schemaVersion=1, referenceRevision=REVISION, cases=[])
    for case, ref in zip(originals, reference):
        canonical = bytes.fromhex(case['rawHex']) if case['kind'] == 'signal' else payload(case)
        pcm = signal(canonical + (b'\xff' if 'basic' in case['kind'] else b''))
        vectors['cases'].append(dict(case, expectedRawHex=canonical.hex(), referenceRawHex=ref['rawHex'],
                                    referencePcmSamples=ref['pcmSamples'], referencePcmSha256=ref['pcmSha256'],
                                    expectedPcmSamples=len(pcm), expectedPcmSha256=hashlib.sha256(pcm).hexdigest()))
    verify(vectors)
    DESTINATION.write_text(json.dumps(vectors, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
