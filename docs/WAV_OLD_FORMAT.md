# OLD系 WAV Encoder / Decoder 仕様

> 旧URL向けの案内です。本文は新しい場所へ移動しました。
> This page preserves an older URL. Follow the link below for the document.

[docs/reference/WAV_OLD_FORMAT.md](reference/WAV_OLD_FORMAT.md)

[ドキュメント目次 / Documentation index](README.md)

## 1. 文書の位置付け

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#1-文書の位置付け)

## 2. レイヤーと用語

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#2-レイヤーと用語)

## 3. OLD raw payload

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#3-old-raw-payload)

### 3.1 種別

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#31-種別)

### 3.2 ファイル名block

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#32-ファイル名block)

### 3.3 BASIC payload

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#33-basic-payload)

### 3.4 Binary payload

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#34-binary-payload)

### 3.5 bodyのニブルスワップとchecksum配置

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#35-bodyのニブルスワップとchecksum配置)

### 3.6 OLD checksum

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#36-old-checksum)

### 3.7 確定テストベクトル

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#37-確定テストベクトル)

## 4. raw byteからPCMへの変換

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#4-raw-byteからpcmへの変換)

### 4.1 信号単位

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#41-信号単位)

### 4.2 リーダー

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#42-リーダー)

### 4.3 1 raw byteのframing

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#43-1-raw-byteのframing)

## 5. WAV containerとDecode入力

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#5-wav-containerとdecode入力)

### 5.1 Encoder出力

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#51-encoder出力)

### 5.2 Decoder受理範囲

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#52-decoder受理範囲)

### 5.3 信号Decode

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#53-信号decode)

## 6. Decode時の検証と終端

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#6-decode時の検証と終端)

## 7. PGP実装境界

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#7-pgp実装境界)

### 7.1 Raw Decode解析ツール

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#71-raw-decode解析ツール)

### 7.2 将来のWAV Analyzer

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#72-将来のwav-analyzer)

## 8. 参照実装から意図的に持ち込まないもの

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#8-参照実装から意図的に持ち込まないもの)

## 9. 未確定事項と互換性確認

[移動先の節へ / Open this section](reference/WAV_OLD_FORMAT.md#9-未確定事項と互換性確認)
