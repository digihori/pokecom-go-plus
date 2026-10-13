# SC61外部アセンブラを使う

Pokecom GO Studioは、内蔵アセンブラの代わりに、あおさんのSC61860 CROSS ASSEMBLER（SC61）を実行できる。
選択はStudio全体の設定で、Workspace、Build、Build & Load、Quick Assembleに共通する。
現時点の外部出力の取り込みはSC61用。yasm等の出力形式への対応は含まない。
SC61の入手・ビルドは利用者が行う。Studioには同梱しない。

## 設定

アプリメニューのSettingsを開き、Assembler欄に以下を入力する。

| 項目 | macOSの例 | Windowsの例（Windows上のStudioは未検証） |
|---|---|---|
| 選択 | External | External |
| Executable path (SC61) | `/Users/yourname/tools/sc61` | `C:\Tools\SC61\SC61.exe` |
| Arguments | `{source}` | `{source}` |
| Timeout in seconds | `60` | `60` |

`Apply assembler setting`で保存する。実行ファイル欄にはパスだけを記入し、パス全体を引用符で囲まない。
SC61は既定のDMP出力モードで使う。コンソールログはStudioに表示される。
`/b`、`/hex`、`/p`は付けない。これらは生成ファイル形式を変えるため、現在のDMP取り込みでは利用できない。

引数の`{source}`はStudioが実行用ソースの絶対パスに置き換える。省略時は末尾にソースパスを追加する。
空白のある固定引数は引用符で囲む。シェルを介さないので、リダイレクトやパイプは使えない。
必要ならSC61のラベル上書きオプションや`/tab`を追加できる。

```text
{source} /tab IS_USE_MAPFILE=0
```

SC61用のDMP取り込みでは、ソースを一時フォルダにコピーして実行する。
作業ディレクトリは元ソースの親フォルダなので、INCLUDE／INCBINはそこからの相対パスにする。
SC61が作る一時DMP／LSTは取り込み後に削除し、元ソース隣の既存DMP／LSTは変更しない。

## 最初の接続確認

ROMを登録した対象機種を選び、Test source file pathにソースの絶対パスを入力する。
`Run connection test`を押すと、成功時はbyte数、アドレス範囲、SC61ログを表示する。
これはメモリへロードせず、対象機種の書込み可能範囲では制限しない。配置検査はLoad時に行う。

最小の確認ソース例（PC-1261向け）:

```asm
ORG $4000
ENTRY: RTN
```

SC61 Rev 0.04aは、正常終了でコード2、エラーでコード0を返すケースがある。
Studioは終了コードだけで判断せず、成功ログと今回生成されたDMPを確認する。
失敗したソースの以前のDMPをロードすることはない。

## 開発時の操作

プロジェクトのAssembly入力に主ソースを登録し、Assembly Workspaceを開く。
INCLUDEで使う補助ソースを独立したAssembly入力として登録すると、別々にアセンブルされる。
INCLUDE／INCBINの変更が主ソースの変更表示に反映されない場合も、明示的に再アセンブルする。

| 操作 | 結果 |
|---|---|
| Assemble | ログ、Memory、Disassembly、LST、Symbol Mapをプレビューする |
| Build | 形式・16bit範囲・同一アドレス重複を検査し、全範囲を`build/`へ保存する。対象機種の書込み可否はLoad時に検査する |
| Build & Load | 同じビルド・保存後、対象機種の配置・物理ミラー・現在のBASIC領域を検査してロードする |
| Program → Quick Assemble | 指定ソースからDMPを保存する。ロードはしない |

Assembly入力が1つなら出力名はそのソース名になる。複数入力なら`program.*`を使う。
失敗時は最後の成功プレビュー・保存済み出力を保持し、その失敗結果をロードしない。
ロード成功は実行開始ではない。実行はポケコン側のキー操作で行う。
処理中はWorkspaceの`Cancel assembly`で中止できる。接続テストには別のCancelボタンがある。

LSTはSC61の原文を表示・保存し、EQUとラベルはSymbol Mapへ取り込む。
診断の`Open at line`はSettingsのExternal editor設定を使う。
SC61はINCLUDE内の行のファイル名を出さないため、内容と行番号で一意に特定できる場合だけ該当行を開く。
特定できない場合は`Open source`で主ソースを開き、ログで原因を確認する。

内蔵に戻す場合はSettings → AssemblerでInternalを選び、Applyする。
内蔵に切り替えてもSC61用ソースの方言は変換されない。

## 配置エラーの扱い

Assemble、Build、Quick Assembleは、対象機種のROMや未割当アドレスへの出力も許容する。
DMPの全データを維持し、16bit範囲、不正形式、同一アドレスの重複は引き続き検査する。
Load時には対象機種のROM、未割当アドレス、物理ミラー重複、BASICとの衝突を検査する。

今回のFENSCERはPC-1261のProjectでも`4000..74FF`の全13,568byteをBuild・保存できる。
現在のPC-1261では`6800`が未割当なので、Build & Loadは保存後にLoadエラーとなる。
未割当部分を切り捨てたり、配置検査を迂回してメモリに書き込んだりはしない。
Load失敗時は一部だけを書き込まず、以前のロード済み状態を保持する。

## 確認済みの範囲（2026-10-12）

- macOS / Apple Silicon、ユーザーがビルドしたSC61 Rev 0.04aで実画面の設定、接続テスト、Workspace、Build & Load、Quick Assemble、診断からVS Codeへの行移動を確認。
- Windows版SC61 Rev 0.04aはmacOSのWine 11.0で確認。19byteの検証ソースはmacOS版とアドレス・byteが一致し、成功2、不明命令17、未定義operandエラー0、失敗後の旧DMP保持を確認。
- Windows上のStudio、Windowsの日本語パス／文字コード、FENSCERのゲーム実行は未確認。

Windows実機での残る確認項目:

- [ ] SC61.exeのパスと引数を設定して保存・再起動後に保持される
- [ ] 接続テスト、Workspace、Build & Load、Quick Assembleが成功する
- [ ] 空白・日本語パス、INCLUDE／INCBIN、Windows-31JのLSTと診断を確認する
- [ ] エラー、タイムアウト、キャンセルで旧成果物をロードしない
- [ ] LST／Map表示、外部エディタへの該当行移動、Internalへの切り替えを確認する

調査・実装・検証の詳細は[SC61連携記録](SC61_INTEGRATION.md)を参照。


### Connection test and large previews

Run connection test directly tests the configured SC61 command even while Internal is selected.
Workspace/Build/Quick Assemble use the saved Internal/External selection; choose External and Apply
assembler setting to use SC61 there. Large Workspace results render visible lines only; Copy all
copies the complete result, and Build preserves the full DMP/LST/Map outputs.
