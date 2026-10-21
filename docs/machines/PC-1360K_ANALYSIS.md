# PC-1360K initial local observation (2026-10-17)

> 旧URL向けの案内です。本文は新しい場所へ移動しました。
> This page preserves an older URL. Follow the link below for the document.

[docs/analysis/pc1360k/PC-1360K_ANALYSIS.md](../analysis/pc1360k/PC-1360K_ANALYSIS.md)

[ドキュメント目次 / Documentation index](../README.md)

## Reproduction

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#reproduction)

## 0x3600 and F-port

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#0x3600-and-f-port)

## Read-path limitation requiring investigation

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#read-path-limitation-requiring-investigation)

## Next questions

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#next-questions)

## Follow-up: READ semantics and row-table candidates

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#follow-up-read-semantics-and-row-table-candidates)

## Follow-up: confirmed firmware arithmetic and selector data dependency

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#follow-up-confirmed-firmware-arithmetic-and-selector-data-dependency)

## Local firmware comparison

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#local-firmware-comparison)

## First hardware probe: user-reported result

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#first-hardware-probe-user-reported-result)

## Second hardware result: DP-dependent READ established in tested setup

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#second-hardware-result-dp-dependent-read-established-in-tested-setup)

## READM hardware result and first table-data request

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#readm-hardware-result-and-first-table-data-request)

## Hardware table bytes and first glyph candidate

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#hardware-table-bytes-and-first-glyph-candidate)

## First glyph recovered and visually checked

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#first-glyph-recovered-and-visually-checked)

## WIDTH comparison and shared display boundary (2026-10-18)

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#width-comparison-and-shared-display-boundary-2026-10-18)

## 2026-10-18: 次の実機確認 — READの上位アドレス比較

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-次の実機確認--readの上位アドレス比較)

### 上位アドレス比較の実機結果を受領

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#上位アドレス比較の実機結果を受領)

## 2026-10-18: 連続8バイトと再読一致の実機確認を準備

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-連続8バイトと再読一致の実機確認を準備)

### 境界・再読確認の実機結果を受領

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#境界再読確認の実機結果を受領)

## 2026-10-18: ファイル回収用の256バイトリーダーを準備

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-ファイル回収用の256バイトリーダーを準備)

### 初回256バイトを実ファイルとして取得・照合

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#初回256バイトを実ファイルとして取得照合)

### 第2ブロックも既知の字形と一致

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#第2ブロックも既知の字形と一致)

### selector 1のブロック回収も照合成功

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#selector-1のブロック回収も照合成功)

## 2026-10-18: 4KiBの機械語一括回収を準備

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-4kibの機械語一括回収を準備)

### 初回4KiBの実機回収を照合成功

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#初回4kibの実機回収を照合成功)

### 第2の4KiBも取得済み字形と整合

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#第2の4kibも取得済み字形と整合)

### selector 1の高速版4KiB回収も照合成功

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#selector-1の高速版4kib回収も照合成功)

## 2026-10-18: ユーザー指示により全域の自動回収へ変更

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-ユーザー指示により全域の自動回収へ変更)

## 2026-10-18: 一括録音から論理128KiBの回収を完了

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-一括録音から論理128kibの回収を完了)

## 2026-10-18: 取得した論理ROMをStudioへ統合

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-取得した論理romをstudioへ統合)

## 2026-10-18: 変換処理で使われる選択値2・3の一括回収を準備

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-変換処理で使われる選択値23の一括回収を準備)

## 2026-10-18: 選択値2・3の実機データを回収

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-選択値23の実機データを回収)

## 2026-10-18: 選択値2・3をStudioへ統合、変換動作を確認

[移動先の節へ / Open this section](../analysis/pc1360k/PC-1360K_ANALYSIS.md#2026-10-18-選択値23をstudioへ統合変換動作を確認)
