# Technical Preview公開チェックリスト

この文書は、Pokecom GO PlusリポジトリをPrivateからPublicへ変更し、
**Pokecom GO Studio Technical Preview 0.1**として公開するための確認項目を管理する。
Technical Previewは完成版ではなく、PC-1245／1251ファミリーを使った基本動作を試せる開発途中版とする。

## 公開の必須条件

### 機能と安定性

- [x] PC-1245のROM起動、LCD、キー入力、BASIC、マシン語入出力、サウンドが動作する
- [x] PC-1250／1251／1255を共通ファミリーとして選択できる
- [x] macOSで実ROMを使った主要操作を確認する
- [x] macOS／Windows／LinuxのCIでCoreテストとDesktopビルドが成功する
- [ ] 公開候補コミットでmacOSの最終スモークテストを実施する
- [ ] Windowsで起動、ROM選択、キー入力、LCD、音声、ファイル入出力を手動確認する
- [ ] Linuxで起動、ROM選択、キー入力、LCD、音声、ファイル入出力を手動確認する
- [ ] 公開時点の既知問題を整理し、READMEとRelease Notesへ記載する

### 初めて利用する人向けの導線

- [ ] 新規clone環境でREADMEの手順だけを使い、起動まで到達できることを確認する
- [x] ROMを同梱しないことと、利用者自身が正当に入手する必要があることを明記する
- [ ] `.pgrom`作成手順を、画面遷移とエラー例を含む利用者向けガイドにする
- [ ] Pokecom GO従来形式ROMからの読み込み方法をガイドへ明記する
- [ ] Windows／Linuxで必要な追加パッケージがあればREADMEへ記載する
- [ ] Technical Previewの画面例をREADMEまたはRelease Notesへ掲載する

### 権利と由来

- [x] リポジトリのライセンスを明記する
- [ ] Pokecom GOから移植・再設計したコードと仕様の由来を最終確認する
- [ ] 画像、フォント、音声、テストデータに再配布できない資産が含まれていないことを確認する
- [ ] 依存ライブラリのライセンスと配布条件を確認する
- [ ] `PORTING_NOTES.md`の未記録項目を確認する

### リポジトリの安全確認

Publicへ変更すると現在のファイルだけでなくGit履歴も公開されるため、履歴全体を対象に確認する。

- [x] Git履歴にROM、ファームウェア、実機から抽出したデータが含まれていないことを確認する
- [x] Git履歴に秘密鍵、代表的なAPIキー、トークン、パスワードが含まれていないことを確認する
- [x] Gitコミットの作者・コミッターメールをGitHub noreplyアドレスへ変更する
- [x] 不要な大容量バイナリやローカル生成物が履歴に含まれていないことを確認する
- [x] `.gitignore`がROM、Golden Snapshot、ローカル設定、ビルド成果物を除外することを確認する
- [x] GitHub Actionsが`contents: read`に制限され、Secretsを参照していないことを確認する

2026-10-05の監査では、全reachable commitを対象にファイル名、blobサイズ、代表的な認証情報パターンを確認し、
ROM、秘密情報、大容量バイナリは検出されなかった。最大blobは約82KBの文書だった。
コミット作者・コミッターのメールアドレスは、公開前にGitHub noreplyアドレスへ統一した。

### リリース準備

- [ ] Technical Previewのバージョン表記とGit tagを決める
- [ ] Release Notesに対応機種、対応機能、既知制限、ROM非同梱を記載する
- [ ] 実行ファイルを配布するか、ソースからの起動だけにするかを決める
- [ ] 配布物を作る場合はmacOS／Windows／Linuxの生成・起動確認方法を決める
- [ ] 公開候補コミットで全CIが成功する
- [ ] 上記の必須確認後にリポジトリをPublicへ変更する

## 公開後でもよい項目

次の項目はプロジェクトの重要な目標だが、Technical Preview 0.1公開の必須条件にはしない。

- Pokecom GO Player（Android／iOS）の実装
- PC-126x、PC-13xx、PC-14xx系への対応
- Debugger、Assembler、Disassembler、Character Editor
- Intel HEX、Raw Binary、WAVの入出力
- バンクROM機種向けの完成版ROM Import Wizard
- ゲームパッド対応
- インストーラー、署名、公証、ストア配布

## 現時点の既知制限

- ROMイメージは配布しないため、利用者が対象機種のROMを用意する必要がある。
- macOSでは主要動作を確認済みだが、WindowsとLinuxはCI確認が中心である。
- ROM Importerの案内は暫定的で、特にバンクROM機種の入力操作は未完成である。
- ROM経由のBASIC入力には実機の1行入力長制限があり、長い行にはTokenizer方式が必要である。
- サウンド出力は初期実装であり、プログラムによって音の途切れや実機との差が残る可能性がある。
- PC-1245／1250の`0xb000..0xbfff`周辺のミラー仕様は再確認が必要である。
- PC-1251のユーザー登録可能な予約語ショートカットは再現していない。
