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
- [x] 公開候補コミットでmacOSの最終スモークテストを実施する
- [ ] Windowsで起動、ROM選択、キー入力、LCD、音声、ファイル入出力を手動確認する
- [ ] Linuxで起動、ROM選択、キー入力、LCD、音声、ファイル入出力を手動確認する
- [x] 公開時点の既知問題を整理し、READMEとRelease Notesへ記載する

### 初めて利用する人向けの導線

- [x] 新規clone環境でREADMEのビルド手順が成功することを確認する
- [x] ROMを同梱しないことと、利用者自身が正当に入手する必要があることを明記する
- [x] `.pgrom`作成手順を、画面遷移とエラー例を含む利用者向けガイドにする
- [x] Pokecom GO従来形式ROMからの読み込み方法をガイドへ明記する
- [x] Windows／Linuxで必要な追加パッケージがあればREADMEへ記載する
- [x] Technical Previewの画面例をREADMEまたはRelease Notesへ掲載する

2026-10-05にmacOS上の新規cloneと空のGradle user homeを使い、JDK 21で`./gradlew build`が
成功することを確認した。初回はGradle、Kotlin/Native、Compose等の取得に時間とディスク領域を要するため、
READMEへ注意事項を記載した。ROMを使うGUI操作は最終スモークテストで別途確認する。

同日に公開候補の作業ツリーで全KMP buildを実行し、PC-1245／1251の実ROMを指定してCoreとDesktopの
全テストを再実行した。続けてmacOS GUIでROM履歴の自動復元、LCD、物理／画面キー、BASIC読込み・実行、
音声、最大化・復元、ファイル選択後のフォーカスを確認し、主要操作に問題がないことを確認した。

### 権利と由来

- [x] リポジトリのライセンスを明記する
- [x] Pokecom GOから移植・再設計したコードと仕様の由来を最終確認する
- [x] 画像、フォント、音声、テストデータに再配布できない資産が含まれていないことを確認する
- [x] 依存ライブラリのライセンスと配布条件を確認する
- [x] `PORTING_NOTES.md`の由来記録を確認する

2026-10-05の監査では、Pokecom GOとpcwavのGit履歴上の作者がPGPと同じ権利者の識別情報だけであること、
tracked assetが再配布可能な合成JSONとGradle Wrapperに限られることを確認した。
PC-1251 Emulator由来の設計はMIT Licenseの参照元revisionと著作権表示を記録した。
Desktop runtimeのGradle metadataで確認した依存componentはApache License 2.0であり、
結果を`THIRD_PARTY_NOTICES.md`へ記載した。

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

- [x] Technical Previewのバージョン表記とGit tagを`v0.1.0-alpha.1`に決める
- [x] Release Notesに対応機種、対応機能、既知制限、ROM非同梱を記載する
- [x] ソースコードに加えて未署名のmacOS／Windows／Linux実行配布物を提供する方針にする
- [x] macOS／Windows／Linuxの配布物生成と起動確認方法を確立する
- [x] macOSで展開型`.app`とDMGを生成し、DMG内容とアプリ起動を確認する
- [x] 手動実行とrelease tagに対応した3環境のパッケージ生成workflowを実装する
- [x] GitHub Actionsを手動実行し、DMG／MSI／DEBと展開型archiveの生成を確認する
- [x] Linux x86_64で展開型tar.gzとDEBのインストール・起動・アンインストールを確認する
- [x] 展開型アプリとDMG／MSI／DEBへLICENSEと第三者通知をアプリresourceとして含める
- [x] 配布物ごとのSHA-256チェックサムを生成する
- [x] 公開候補コミットで全CIが成功する
- [x] 上記の必須確認後にリポジトリをPublicへ変更する

利用者向け名称は「Pokecom GO Studio Technical Preview 0.1」、Gradle project versionとGit tagは
`0.1.0-alpha.1`／`v0.1.0-alpha.1`とする。DMG／MSI／Debの`packageVersion`はjpackageの制約と
macOSがmajor version 0を拒否する点に合わせ、内部的に数値の`1.0.0`を使用する。
tagは公開候補の最終テストとCI成功後に作成する。
Technical Previewの配布物は配布用証明書で署名せず、macOS公証も行わないことと、
OSのセキュリティ警告が表示される可能性を明記する。macOSのjpackageはad-hoc署名を付与するが、
Developer ID署名の代わりにはならない。

2026-10-05にTemurin JDK 21の`jpackage`を使用し、arm64 macOS向けの展開型`.app`とDMGを生成した。
DMGを読み取り専用でマウントし、Pokecom GO Studioアプリ、PGPのMIT License、第三者通知を確認した。
アプリは起動後に即時終了せず、DMGのSHA-256生成も確認済みである。

同日にGitHub Actionsの配布workflowを`main`から手動実行し、macOS／Windows／Linuxの3ジョブが
すべて成功した。各環境についてnative package、展開型archive、SHA-256一覧を含むworkflow artifactが
作成され、14日間の保持期間が設定された。tagを使わない検証のため、GitHub Release作成jobは意図どおり
skipされた。Windowsでは展開型アプリ、Linux x86_64では展開型tar.gzとDEBについて手動起動を確認した。
Linux DEBのメニュー項目はカテゴリ未指定のため「その他」に登録される。ARM64 Linux配布物は未対応である。

公開候補`85ac732`では、pushにより起動したGitHub Actions CIのmacOS／Windows／Linuxジョブが
すべて成功した。Windowsの同梱ランタイム付きZIP／MSI、Linux x86_64のtar.gz／DEBは追加ランタイムを
手動導入せず起動できた。LinuxのDEB導入時に`apt`が要求する依存パッケージは通常のパッケージ管理へ委ねる。

2026-10-05にリポジトリをPublicへ変更した。未認証のHTTPアクセスでリポジトリ、README画像、
`v0.1.0-alpha.1` Prereleaseおよび9個の配布資産が公開されていることを確認した。Windows／Linuxの
全ファイル入出力確認はTechnical Preview公開後の継続確認とし、既知制限を維持する。

## 公開後でもよい項目

次の項目はプロジェクトの重要な目標だが、Technical Preview 0.1公開の必須条件にはしない。

- Pokecom GO Player（Android／iOS）の実装
- PC-126x、PC-13xx、PC-14xx系への対応
- Debugger、Assembler、Disassembler、Character Editor
- Intel HEX、Raw Binary import、WAVの入出力
- バンクROM機種向けの完成版ROM Import Wizard
- ゲームパッド対応
- インストーラー、署名、公証、ストア配布

## 現時点の既知制限

- ROMイメージは配布しないため、利用者が対象機種のROMを用意する必要がある。
- Apple Silicon MacのVMware上にあるWindows ARM64でx64版をエミュレーション実行し、ROM起動、
  キー入力、BASIC動作を確認した。この環境のDirect3D／OpenGLでは一部Materialボタンのhover描画が
  壊れたため、互換性を優先して当面はSoftware描画を既定とする。ネイティブWindows x64で再確認し、
  Compose／Skiko更新時にも既定値を再評価する。
- Linux x86_64では展開型tar.gz、音声再生、DEBのインストール・起動・アンインストールを確認済みである。
  全ファイル入出力の最終確認、ARM64配布物、メニューカテゴリ指定は未対応である。
- ROM Importerの案内は暫定的で、特にバンクROM機種の入力操作は未完成である。
- ROM経由のBASIC入力には実機の1行入力長制限があり、長い行にはTokenizer方式が必要である。
- サウンド出力は初期実装であり、プログラムによって音の途切れや実機との差が残る可能性がある。
  WindowsでBASICをBREAKした後もクリックノイズが続く問題には、無音遷移時にJava Soundの
  出力ラインをflush／closeする修正を加えた。Windows実機での再確認は必要である。
- PC-1245／1250の`0xb000..0xbfff`周辺のミラー仕様は再確認が必要である。
- PC-1251のユーザー登録可能な予約語ショートカットは再現していない。
