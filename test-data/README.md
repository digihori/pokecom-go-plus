# Test Data

このディレクトリには、PGP Coreの動作確認に使用する再配布可能なテストデータを置く。

予定する分類：

```text
test-data/
├── cpu/
├── machine/
├── basic/
└── display/
```

市販機のROMイメージや、再配布許可が確認できないデータを追加してはならない。
テストデータの由来と生成方法は、各データと同じディレクトリのREADMEへ記録する。

## WAV合成データ

[S1／S2参照vector](wav/s1-s2/README.md)に、独自BASIC／合成byteから生成した固定rawとPCM期待値を置く。
ROMや録音は含まず、製品codec追加前の仕様確認データとして管理する。

## Assembly acceptance sample

- [`assembly/pc1245-hello`](assembly/pc1245-hello/README.md): PC-1245 / built-in SC61860 assembler、
  HELLO WORLDのBuild・Load・表示・RTN確認。ROMは含まない。
