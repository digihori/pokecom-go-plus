# iOS プログラムロード完了表示の統一

2026-11-01。サウンド改善を区切った後のユーザー依頼で、ロード後の表示をAndroid Playerへ揃える。

Androidの`AndroidProgramIo`と`ProgramDialogs`を確認し、成功時の「ロード完了」「ファイル名 をロードしました。」「閉じる」のみをiOSでも表示する。従来のCPU再開・RUN自動入力に関する長い説明と、再選択ボタンを含むロード画面は成功時に閉じる。

SwiftUIの親sheetとFiles pickerが閉じた後、親sheetのonDismissから完了alertを表示する。完了通知待ち・表示中も外部入力を受け取らず、閉じた後に復帰する。Resetや次のプログラム操作で古い完了通知を破棄する。CPUのロード後再開、エラー・取消・セーブの既存動作は維持する。

BASIC/DMP両方の成功・表示順序・外部入力の復帰・Reset後の古い通知抑止をhost単体テストに追加した。既存のロード/セーブUI試験を更新し、実際のFiles選択後にファイル名入りの小さい完了alertが出ること、再選択ボタンが残らないことを確認する。

検証：

- JDK 21の全体build成功（578 tasks、14実行）。
- Swift単体57件、失敗0。音声のライブデバイス依存1件は通常CIと同じく除外。結果は`iosApp/build/CI-TestResults-20261101-072558.xcresult`。
- 同じ初回実行のUI試験は、Filesの「最近使った項目」が空で既存のファイル選択helperが失敗した。ロード完了まで未到達であり、その実行全体を成功とはしない。
- テストhelperを、必要時にブラウズ→このiPhone内→Playerフォルダへ進むよう修正した。製品のFiles初期フォルダ方針は変更していない。
- 隔離Simulatorでロード/セーブ/情報画面のUI 1件を再実行し成功。BASIC、保存BASIC、DMP、保存DMPの4回のロードで、ファイル名入り完了alert・再選択ボタンなし・閉じた後のCPU実行を確認した。セーブ取消/保存と情報画面も成功。結果は`iosApp/build/CI-TestResults-20261101-073207.xcresult`。
- UI添付の「Compact program load completion」を抽出して目視し、機種画面上に見出し・ファイル名入りの一文・「閉じる」のみが表示されることを確認した。
- `git diff --check`成功。
