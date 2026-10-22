# S1／S2 WAVの合成参照vector

2026-10-20、WAV対応の段階1（仕様・検証データ整理）で作成した。
製品のS1/S2 codecの実装完了を意味しない。

## 由来

- `vectors.json`：独自に作った連番・パターンbyteと短いBASICソース28件。ROMや録音を含まない。
- 参照値：同一作者のpcwav、commit `14e1ef53596f0d8791b6d14965061f63846d11a7`を読み取り専用で実行。
- 採用期待値：仕様から独立に書いた`generate.py`で構成。PGP製品codecから生成していない。
- スクリプトやfixtureを製品の実行時・ビルド時の外部依存にはしない。

各caseには入力、logical byte列、採用するraw全byte、pcwavのraw全byte、
双方のPCM sample数とSHA-256を記録する。PCMはunsigned 8-bit、mono、8000Hz、leader込み。
BASICではpayload末尾の後に物理trailer `FF`を1 byte追加してPCMを計算する。
`signal-all-bytes`は共通信号だけを確認する`00..FF`全256 bytesで、payloadとして解析しない。

## 採用値と参照値を混同しない

- `expectedRawHex`／`expectedPcm*`：PGP実装で採用する期待値。
- `referenceRawHex`／`referencePcm*`：指定commitのpcwavの出力そのもの。
- S1 Binaryのpartial tailは、実機確認済みの「partial raw＋最初のFF」と、pcwavの旧方式「FFのみ」で異なる。
  参照値は差を検出する対照として保存し、PGP Decoderの無条件成功例として使用しない。
- 2026-10-22訂正：Binaryの120の倍数も最後の120 bytesにはchunk checksumを付けず、tailへ含める。
- BASICは最後のchunkが120 bytesでもchunk checksumを付けず、その120 bytesをtail計算へ含める。
- `s2basic-type37`は同じsource/bodyのtypeを変更する例で、password文字列を設定する例ではない。

期待値の根拠と未確認範囲は[参照動作仕様](../../../docs/reference/WAV_S1_S2_FORMAT.md)を参照。
特にBinaryの最大長・他機種の実機往復まで確認済みという意味ではない。

## 固定データの検証

Python 3の標準ライブラリだけで検証できる。pcwavとPerlは不要。

```sh
python3 test-data/wav/s1-s2/generate.py --verify
```

これはfixtureの整合性検査であり、Kotlin codecの動作試験とは別。
段階2の`S1SignalVectorTest`（desktopTest）では固定JSONのrawとPCM期待値を使い、独立した期待値と製品出力を比較する。
commonTestでは同期・破損・切詰め・位置情報等を検証する。
checksum反転、欠落、不正長等の失敗系は、仕様書の表に沿って製品codec追加時に検証する。

## 参照値の再採取

pcwavとPGPを同じ親ディレクトリへ置く場合の例。参照srcに変更がある場合やcommitが異なる場合は停止する。
外部リポジトリは変更しない。実行にはPython 3とPerl（JSON::PP／Digest::SHA等の標準モジュール）が必要。

```sh
python3 test-data/wav/s1-s2/generate.py --pcwav ../pcwav
```

参照実装と独立生成したraw・PCMを比較してから`vectors.json`を書き出す。
Binaryでは旧partial tailと旧最終full chunk構造以外に差があれば停止する。変更時はfixtureの差分を確認して採用する。

段階3では同じ固定JSONのBinary 8件を`S1BinaryCodec`と照合し、旧形式の8件を拒否する（partial tailは`CHECKSUM_MISMATCH`、旧最終full chunkは終端不正となる）ことも`S1SignalVectorTest`で確認する。

段階4の`S1SignalVectorTest`でS1 BASIC 8件の製品payload出力・Decodeも固定rawと照合する。

段階5ではS2 BASIC 11件の機種別ソース変換とpayloadを固定期待値に照合し、type 27／37、
漢字・カナ、ON行参照リスト、THEN、REM／quoteを検証する。

2026-10-22追記：120／240 bytesの採用raw、PCM長とSHA-256を実機規則へ訂正した。
旧pcwav 0.5の参照値は比較・拒否テスト用として保持する。
pcwav 0.5.2（`156bb6a2ed09df3ac03c6e7ccc42e96c87cc406d`）との読み取り専用比較で、
全28件の独立生成raw／PCMが一致した。ROM／実機録音はfixtureへ含めていない。
