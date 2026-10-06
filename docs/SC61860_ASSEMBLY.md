# SC61860 Assembler

Pokecom GO Studioには、SC61860向けの2パスAssemblerが組み込まれている。
外部エディタで編集した`.asm`をプロジェクトの`assembly`ソースとしてBuild & Loadできるほか、
Studioの`Assemble`操作で、ロードせずにPGP Memory Dump (`.dmp`)へ変換できる。

## 基本構文

```asm
ORG 0xC000

START: LII 0x12
       LIDP TABLE
       JRP DONE
TABLE: DB 0x01, &02, 3
DONE:  RTN
```

- コメントは`;`から行末まで。
- ラベルは英字または`_`で始め、英数字と`_`を使用できる。大文字・小文字は区別しない。
- `ORG`で出力開始アドレスを指定する。複数の`ORG`による複数セグメントにも対応する。
- `DB`はカンマ区切りで1バイトの値を配置する。
- 数値は10進数、`0x`、`&`、`$`接頭辞の16進数を使用できる。
- 相対分岐には絶対的な飛び先アドレスまたはラベルを書く。Assemblerが8bit相対値へ変換する。
- `LP`は`LP 0x00`から`LP 0x3F`として、命令コード内に埋め込む値を指定する。
- `CAL`の飛び先はSC61860の命令形式に従い`0x0000..0x1FFF`に限られる。

Disassemblerが生成する`.asm`は、このAssemblerで再び機械語へ変換できる。
アセンブルエラーには元ソースの行番号と理由が表示される。

