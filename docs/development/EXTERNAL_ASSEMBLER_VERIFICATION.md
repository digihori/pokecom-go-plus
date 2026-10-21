# 外部アセンブラの検証・受入れ記録

2026-10-22に現行ガイドから分離した計画・検証履歴です。
各節の「現在」「未実装」「後続」は記録時点を表し、現在の対応範囲を示しません。
現行の説明は[EXTERNAL_ASSEMBLER.md](../guides/EXTERNAL_ASSEMBLER.md)を参照してください。
元の本文を保持し、当時の未確認事項を後から完了扱いにはしていません。

## 確認済みの範囲（2026-10-13更新）

- macOS / Apple Silicon、ユーザーがビルドしたSC61 Rev 0.04aで実画面の設定、接続テスト、Workspace、Build & Load、Quick Assemble、診断からVS Codeへの行移動を確認。
- Windows版SC61 Rev 0.04aはmacOSのWine 11.0で確認。19byteの検証ソースはmacOS版とアドレス・byteが一致し、成功2、不明命令17、未定義operandエラー0、失敗後の旧DMP保持を確認。
- Windowsユーザー確認で、SC61接続テストとBuild成功、修正後の「プログラム登録」成功が報告された。登録先の詳細は未確認。
- Windowsの設定再起動保持、日本語パス／文字コード、全操作の失敗・取消、Assembleハング解消の個別確認、FENSCERのゲーム実行は確認待ち。Build成功をLoadやゲーム実行成功とは扱わない。
- SC61初版の対応は2026-10-13に一区切りとした。2026-10-20にyasm.pl／汎用DMP adapterと4択の画面を追加。新adapterの確認範囲は下記参照。

Windows実機での残る確認項目:

- [ ] SC61.exeのパスと引数を設定して保存・再起動後に保持される
- [x] 接続テストとBuildが成功する（Windowsユーザー報告）
- [ ] Workspace Assembleのハング解消、Build & Load、Quick Assembleを個別確認する
- [ ] 空白・日本語パス、INCLUDE／INCBIN、Windows-31JのLSTと診断を確認する
- [ ] エラー、タイムアウト、キャンセルで旧成果物をロードしない
- [ ] LST／Map表示、外部エディタへの該当行移動、Internalへの切り替えを確認する

調査・実装・検証の詳細は[SC61連携記録](SC61_INTEGRATION.md)を参照。


### Connection test and large previews

Run connection test directly tests the configured SC61 command even while Internal is selected.
Workspace/Build/Quick Assemble use the saved tool selection; choose SC61 and Apply
assembler setting to use SC61 there. Large Workspace results render visible lines only; Copy all
copies the complete result, and Build preserves the full DMP/LST/Map outputs.


## 新adapterの確認範囲（2026-10-20）

- ユーザーのローカルYASM61860 1.02を変更せず実行し、終了コード0／不正命令の失敗を確認。
  アドレス境界内・非整列・行境界をまたぐ出力、複数ORGによる空き領域、不正命令を自動検証。
- 合成の別プロセスで、汎用の成功・失敗・出力なし・不正DMPと、以前の成果物を取り込まないことを確認。
- SC61の成功メッセージ・DMP・Listing取り込み、共有Runnerの終了・タイムアウト・キャンセルの回帰テストを維持。
- 旧設定の読込みと各ツール種別・引数・終了コードの保存復元は、メモリー内の設定ストアで検証する。
- macOSのローカル検証と、Windows／Linux実環境のyasm.pl・汎用ツール確認は区別する。
  外部ツールやユーザーの実ソースをCIへ追加しない。YASM実行テストは`PGP_YASM_SCRIPT`指定時だけ実行する。

```sh
PGP_YASM_SCRIPT=/path/to/yasm.pl ./gradlew :desktopApp:test --tests '*DesktopConfiguredExternalAssemblerTest'
```

参照：[YASM61860作者の説明](https://www.oit.ac.jp/labs/rd/rssrv/kobayashi-lab/~yagshi/old_web/misc/pocketcom/yasm-e.html)。
実行仕様はローカル1.02のコード・実測を優先し、外部ソースを製品へ組み込んでいない。

今回の最終ローカル検証：JDK 21で`build :core:desktopTest`成功。
Studio 323件（既存skip 1）、Core desktop 462件で失敗なし。
YASM実行テストはローカルスクリプト指定付きで実行した。
一時HOME・メモリー内Preferences・空の作業ディレクトリでStudioを起動し、
日英の4択部分は隔離描画で確認した。通常の設定／ROM Libraryは使わない。
本番設定画面での全操作・Windows／Linux実ツール・hosted CIの確認とは区別する。

### 同日の修正：YASMの出力指定がない設定

実行ファイルにyasm.plを直接指定し、引数が`{source}`だけの設定では、YASMは正常終了しても
DMPを標準出力へ出すため、Studioが`External assembler did not generate a DMP file`として拒否していた。
YASM選択時の`-d`設定をStudio側で補完・管理し、この既存設定のまま利用できるようにした。
汎用DMPとSC61の引数処理は変更しない。
修正後、ローカル1.02の直接起動＋`{source}`だけの設定で、ユーザーのpacman.asmの取り込みを確認。
出力指定なし／`-d {output}`／固定名の指定を含む回帰テストと、全体`build :core:desktopTest`が成功した。
これはアセンブル結果の取り込み確認であり、ゲーム実行の確認とは区別する。

## 今回のユーザー確認（2026-10-20）

使用するアセンブラの選択、内蔵／SC61／yasm.pl／汎用の設定タブと個別保存、
メイン画面・Workspaceでの使用対象表示、長いログの固定枠・スクロール表示を追加後、
ユーザーから「動作確認しました。問題ないと思います」との報告を受領した。
今回の外部アセンブラ対応を一区切りとする。
確認OS・各ツール・個別操作の内訳は未指定であり、全環境・全ツールの網羅確認とは区別する。
