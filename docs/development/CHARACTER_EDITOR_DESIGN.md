# Character Editorの設計・将来候補・検証

2026-10-22に現行ガイドから分離した計画・検証履歴です。
各節の「現在」「未実装」「後続」は記録時点を表し、現在の対応範囲を示しません。
現行の説明は[CHARACTER_EDITOR.md](../guides/CHARACTER_EDITOR.md)を参照してください。
元の本文を保持し、当時の未確認事項を後から完了扱いにはしていません。

## 責務境界

`core/commonMain`は不変な`DotPattern`、フォーマット定義、byte列との相互変換、テキストの解析と出力を
担当する。UI toolkit、ファイル、クリップボード、Emulator Sessionには依存しない。

`desktopApp`はCompose state、グリッド描画、pointer座標変換、ドラッグ操作、入力エラー表示、
選択可能な出力表示、独立ウィンドウを担当する。

幅、高さ、列／行優先、LSB／MSB方向は`DotPatternFormat`で定義する。新しい形式はこの定義と、必要なら
機種固有の出力変換を追加し、グリッドUIへ機種別のbyte処理を埋め込まない。

## 将来項目

- Undo／Redo
- 複数文字と文字セットの編集
- パターン単位のCopy／Paste
- ファイルおよびバイナリ入出力
- Emulator LCDプレビューとメモリへの反映
- Projectへの追加
- 機種ごとに確認したユーザー定義文字プロファイル
- シフト、反転、回転などの編集操作

## テスト

Coreではbit方向、byte順、往復変換、未使用bit、数値解析、BASIC／Assembler出力を検証する。
Desktopでは編集状態、入力失敗時のパターン保持、pointer座標からセルへの変換を検証する。
