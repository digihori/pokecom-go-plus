# Character Editor

Pokecom GO StudioのCharacter Editorは、エミュレーター本体から独立したドットパターン作成ツールである。
Studioの`Tools > Character Editor`から開き、ROMや実行中のEmulator Sessionを必要としない。

## 現在の操作

現在のプロファイルは`5×7 column bytes (LSB top)`とする。

- 幅5、高さ7
- 列優先で1列を1 byteへ格納する
- 上端をbit 0、下端をbit 6とする
- bit 7は未使用とし、数値入力時に設定されていればエラーにする

これはPC-1245のLCD論理表現と同じ並びを使う汎用プロファイルであり、実機のユーザー定義文字形式を
確認済みとするものではない。機種固有の命令、文字コード、RAM配置は、仕様確認後に別プロファイルまたは
出力変換として追加する。

現在は、単一文字のドット編集、クリックとドラッグによる描画・消去、Clear、Invert、数値列との
双方向変換を提供する。出力は`&NN`、10進数、`$NN`、`0xNN`の4行を選択可能なテキストとして表示し、
必要な行または範囲を通常のコピー操作でクリップボードへコピーする。

数値入力はカンマまたは空白区切りとし、10進数、`0xNN`、`&NN`を受け付ける。入力途中の不完全な値で
編集中のパターンを失わないよう、`Apply to grid`が成功した場合だけグリッドへ反映する。

## 責務境界

この節は記録時点の内容です。[Character Editorの設計・将来候補・検証](../development/CHARACTER_EDITOR_DESIGN.md)へ分離しました。

## 将来項目

この節は記録時点の内容です。[Character Editorの設計・将来候補・検証](../development/CHARACTER_EDITOR_DESIGN.md)へ分離しました。

## テスト

この節は記録時点の内容です。[Character Editorの設計・将来候補・検証](../development/CHARACTER_EDITOR_DESIGN.md)へ分離しました。
