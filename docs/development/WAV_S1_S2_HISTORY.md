# S1／S2 WAVのfixture・実装順履歴

2026-10-22に現行ガイドから分離した計画・検証履歴です。
各節の「現在」「未実装」「後続」は記録時点を表し、現在の対応範囲を示しません。
現行の説明は[WAV_S1_S2_FORMAT.md](../reference/WAV_S1_S2_FORMAT.md)を参照してください。
元の本文を保持し、当時の未確認事項を後から完了扱いにはしていません。

## 8. 検証に追加するfixture

- S1 Binary: body長`1`、`119`、`120`、`121` bytes、開始address境界、実機保存WAV
- S1 BASIC: bodyが119／120／121 bytesとなるsource、半角カナ、末尾checksum、物理trailer `FF`
- S2 BASIC: type `27`／`37`、行参照、`ON ... GOTO/GOSUB`、半角カナ／漢字、末尾checksum、物理trailer
- 各形式: PGP Encoderと実機、`pcwav`のraw byte列およびPCM比較
- S2 Binary: S1共用codecの境界・checksum試験に加え、S2機種での実機往復WAV


## 9. 段階1の合成検証データ（2026-10-20）

[test-data/wav/s1-s2](../../test-data/wav/s1-s2/README.md)に28件の固定vectorを用意した。
logical byte列から独立に組み立てた採用期待値と、固定commitのpcwavが出す参照値を分けて保存する。
PGPの製品codecを使って期待値を生成していない。参照値の再採取だけに外部pcwav／Perlが必要であり、
通常の検証と将来のCoreテストには固定JSONを使う。

- Binary: 1／119／120／121／239／240／241 bytes、終端やtypeと同じ値を含むbody、開始`0000`／`4000`／`FFFF`。
- S1／S2 BASIC: logical body 119／120／121／239／240／241 bytes、quote／REM、カナ、S2漢字。
- S2: `ON ... GOTO`の行番号列、`THEN`の行参照、同じbodyでtype `27`／`37`を比較。
- 共通信号: `00..FF`全256 bytes、leader込みPCMのsample数とSHA-256。
- 各payload: raw全byteと、BASICの物理trailerを含めたPCMのsample数・SHA-256。

Binaryの参照値は旧tailを含むため、差分を認識する対照データであり、無条件の成功期待値ではない。
実機ROM・録音由来のbyte列は追加していない。

### 後続で追加する失敗系・最大値の検証

製品codec追加時、上記の固定正常vectorへ決定論的な破損を加えて失敗段階を検証する。

| 対象 | 条件 | 期待する扱い |
| --- | --- | --- |
| name／metadata／chunk／tail | checksumまたは対象dataを1 bit反転 | 位置付き`CHECKSUM_MISMATCH`、通常Program出力不可 |
| header／body／終端 | 切詰め、`FF`欠落、余剰raw | 切詰め・終端・余剰の診断。PCM/rawは保持 |
| Binary範囲 | 空body、先頭FFFF＋2 bytes、先頭0000＋65536 bytes | 前2つは拒否。最後はmetadataのFFFFを65536として扱う |
| Binary入力 | 複数領域・gap | 連結・zero-fillせず拒否 |
| BASIC行 | 不正長・行末0D欠落・行番号範囲・未知byte | 転送構造と文字変換の診断を分ける |
| 信号 | 無音、振幅差、別sample rate、先頭ずれ、途中欠落 | 同期検出・診断とraw位置対応を検証 |

## 10. 実装順序と今回の区切り

1. 仕様と合成検証データ整理：完了。
2. S1/S2共通信号のEncode／Decode：完了。OLDの19-unit処理を変更せず、S1の16-unit処理を追加する。
3. s1bin：payloadとStudio接続・合成検証は完了。PGP Encoderでの実機往復は未確認。
4. s1basic：機種別BASICの転送body adapterとStudio接続・合成検証が完了。実機往復は未確認。
5. s2basic：機種別語彙、行参照、日本語、typeのStudio接続・合成検証が完了。実機往復は未確認。
6. UI・診断・操作ガイド・OLD回帰の仕上げ。

WAVはStudio専用とし、Player、音声デバイスI/O、ROM CSAVE/CLOAD hook、Decodeからの直接ロードは追加しない。
既存のBASIC Codecを再利用するが、S2のPC-1360系／PC-1470U系などの語彙を一律に扱わない。
