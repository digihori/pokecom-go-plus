# PGP BASIC Text Format

## Purpose

PGP BASIC Text Formatは、ポケットコンピュータのBASICプログラムをUTF-8テキストとして
保存・交換するための形式である。通常文字に加え、機種依存の特殊文字と任意の内部byteを
可逆に表現する。

## Text encoding and line endings

- 文字コードはUTF-8とする
- UTF-8 BOMは入力時に許可し、先頭から除去する
- CRLF、CR、LFは入力時にLFへ正規化する
- BASIC行はテキストの行単位で表現する

## Escapes

| Canonical form | Meaning |
|---|---|
| `\\` | backslash character |
| `\PI` | pi symbol |
| `\SQR` | square-root symbol |
| `\EX` | exponent symbol |
| `\BX` | block symbol |
| `\xNN` | exact byte value from `00` through `FF` |

Named escapes are semantic. Their internal code may differ by machine, so a machine profile resolves them
when encoding or decoding. The Unicode characters `π` and `√` are accepted as input aliases for `\PI` and
`\SQR`; canonical text output uses the named escapes.

Raw-byte escapes are not semantic. For example, `\xFC` always requests byte `0xFC`; it does not mean
“square root.” A source containing raw-byte escapes can therefore be machine-dependent even when two
machines belong to the same CPU generation.

Unknown, incomplete, and malformed escapes are errors. Parsers report their one-based line and column and
must not silently replace them.

## Loading backends

The ROM keyboard backend can load ordinary characters and named symbols that exist on the target keyboard.
For example, PC-1245 resolves `\SQR` to the consecutive taps `SHIFT`, `DOT`. Raw bytes and symbols without a
keyboard operation require the direct tokenizer backend.

The loader must report an unsupported element instead of silently changing the program. Backend selection
may be automatic after parsing, but source text parsing itself is machine-independent.

For PC-1245 ROM keyboard loading, the current semantic mappings are:

| Element | Key taps |
|---|---|
| `\PI` | `SHIFT`, `0` |
| `\SQR` | `SHIFT`, `DOT` |
| `\EX` | `SHIFT`, `PLUS` |

`\BX`, `\xNN`, and characters absent from the PC-1245 keyboard are rejected with their source line and
column. A direct tokenizer backend will handle them in a later phase.

For compatibility with common pocket-computer source listings, a colon immediately following the line
number is normalized to one space by the PC-1245 ROM keyboard compiler. Whitespace may occur between the
line number and colon. Colons in the BASIC statement body remain unchanged.

```basic
10:PRINT "A"   -> 10 PRINT "A"
20 PRINT A:B   -> 20 PRINT A:B
```
