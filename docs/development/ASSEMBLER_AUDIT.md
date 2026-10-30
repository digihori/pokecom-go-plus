# SC61860アセンブラ調査・改善案

文書区分：調査・計画・実装／検証の記録（2026-10-22整理）。
過去の時制・件数・未確認事項を保持しています。現在の操作は[導入ガイド](../guides/GETTING_STARTED.md)、
[Androidヘルプ](../guides/PLAYER_HELP.md)、[iPhoneヘルプ](../guides/PLAYER_HELP_IOS.md)または対応する機能ガイドを参照してください。

2026-10-11 / main・2890cd5時点の調査記録。この調査時点では製品コードの変更・移植は行っていない。

追記: 以下は2890cd5時点の調査記録。段階1・2の実装内容と現在の制約は
[SC61860_ASSEMBLY.md](../guides/SC61860_ASSEMBLY.md)を参照。段階3（式・EQU・DW・文字列DB・DS・ALIGN）も実装済み。
段階4のロード済みソース表示は実装済み。残項目は[Assembler整備 第1段階](ASSEMBLER_STEP1.md)を参照。追加で`#`コメント、Workspaceの出力名・スクロール結果欄・
ロード状態表示を改善した。Quick Assembleの結果表示と共通処理は2026-10-31に実装。

## 現状

Coreは単一ファイルの2パスアセンブラ。命令表にはエンコード可能な234 opcodeエントリ、
134種類のニーモニックがある（LP 64コード、CAL 32コード、NOP別コードを含む）。
72/73/76/77/D7の5コードはUNDOCとして長さのみ登録し、アセンブル不可。
残り17コードは未定義。全命令の実機正確性を保証するテストはない。

8bit即値、16bit上位→下位の命令operand、LPの6bit埋込み、CALの13bit宛先、
通常の相対分岐の命令アドレス+1基準はCPU実装と整合する。
CASE1は件数とアドレスを24bit単一値で指定、CASE2は単独opcode。
分岐テーブルを構造化して生成・検証する支援はない。

| 構文 | 対応 | 制約 |
|---|---|---|
| ラベル | NAME:、同一行命令、前方参照、大小文字無視、重複検出 | 英字/underscore始まり。ローカル・数値・ファイル間参照なし |
| 数値 | 10進、0x/&/$の16進 | 2進、H接尾辞、文字リテラルなし。負数は解析できるが出力範囲で拒否 |
| 式・定数 | 数値または単独ラベル | 加減算、括弧、現在位置、HIGH/LOW、EQU/SETなし |
| ORG | 16bit数値、複数セグメント、論理重複検出 | 式不可。機種・bank非依存。ORG行のラベルは変更前位置に束縛 |
| DB | カンマ区切りbyte、ラベル参照 | 文字列、word、予約、整列なし |
| コメント | セミコロン、空行、BOM/改行正規化 | 引用文字列構文なし |
| その他 | ― | INCLUDE、INCBIN、条件、マクロ、セクション、リンクなし |

Core診断は最初の1件、行と英語messageのみ。不正な式と未定義名は区別しない。
列・診断コード・警告・関連定義位置なし。出力画像の重複等はline=0。
Workspaceは各ファイルの最初の診断を集め、ファイル・行・該当ソースを表示する。

## 正確性の問題

以下は一時Coreテストで現在の挙動を再現した。一時テストは削除済み。
修正時には正しい期待値の回帰テストとして追加する。

| 優先度 | 問題 | 再現例・影響 |
|---|---|---|
| P0 | TEST (6B) の即値定義欠落 | TEST 1はtakes no operand、TESTだけでは6Bのみ出力。CPUは次byteを読む。デコーダも長さを誤る |
| P0 | UNDOCの逆アセンブル出力が再入力不能 | 72 AA→UNDOC 0xAA→Failure |
| P0 | NOP別コードのバイト消失 | 68→NOPT→33、CD→NOPW→4D。成功してもbyteが変わる |
| P0 | 逆アセンブル末尾で範囲外先読み | 長さ判定前にoperandを読む。末尾LIDPでセグメント限定readerが配列境界例外 |
| P1 | 相対分岐の16bit折返し非対応 | FFFEのJRP 0000を拒否。CPU/デコーダでは表現できる |
| P1 | 末尾ラベルの境界 | FFFFのDB直後のラベルを10000として登録可能 |
| P1 | DB空要素を無視 | DB 1,,2,が成功する |

共通命令表だけの往復検証は表自体の誤りを見逃す。独立した期待opcode/長さ/operand表が必要。
外部資料にはDATA/MVWP、PTC/CASE1、DTC/CASE2等の別名があるが、現在はPGP名のみ受理。
33のNOPT扱いと資料のSTH扱いなど未公開動作差は、TEST欠落とは分けて調べる。
外部資料だけで未公開命令を推測実装しない。

## Studioと出力

| 経路 | 現在 | 不足・注意 |
|---|---|---|
| Quick Assemble | asm→Core→DMP保存。ロードしない | lst/mapなし。Workspaceと異なりStudioソースヘッダ前処理なし |
| Workspace Assemble | 各asmを独立コンパイル。Memory範囲、Disassembly、Listing、Map | ファイル間シンボル共有・重複検証なし。失敗時は直前成功プレビュー保持 |
| Build | 全ソース結合、論理重複検出、program.dmp/.lst/.map | 機種別ロード適合性は未検証。旧成果物の識別・出力一括更新を改善候補にする |
| Build & Load | ROM機種一致確認、BASIC→機械語ロード | BASIC成功後の機械語失敗は部分適用。BASIC/asm占有領域衝突をBuildで検出しない |
| デバッガ | 共通命令表、アドレスbreakpoint | Map利用、ソース行対応、ソースbreakpoint、entry指定の連携なし |

Listingはbyteを出した行のみ。ORG・ラベルのみ・コメント行は載らない。
Mapはファイル別ラベルを整列表示するもので、リンカのシンボル表ではない。
逆アセンブルはDB領域も命令解釈し、元のコード/データ区別を復元しない。
編集は外部エディタ。行指定はPGP_EDITOR設定に依存する。
ロードは実行開始位置やソースデバッグ情報を登録しない。

## 配置・機種・ROM・bank

AssemblerとAddressedMemoryImageは16bit論理アドレスのみで、機種・RAMモード・bankを持たない。
ロード時にSession/Machineが読取専用領域を検査する。BuildではRAM/LCDミラーによる
同一物理位置の衝突を検出しない。以下はPGPの実装範囲で、自由な配置先の推奨ではない。

| 機種 | RAM／ロード許可 | ROM・注意 |
|---|---|---|
| 1245 | loaderは8000..FFFF。busは広い互換write窓とミラー | 内部0000..1FFF、外部4000..7FFF。許可範囲と物理RAM容量は別 |
| 1250/1251/1255 | 標準RAM開始C000/B800/A000、終端C7FF。拡張A000..C7FF。LCD/ミラーも許可 | 内部0000..1FFF、外部4000..7FFF |
| 1261 | 2000..67FFを許可。LCD/ミラーを含む | 内部0000..1FFF、外部8000..FFFF |
| 1350 | 2000..7FFFを許可。LCDを含む | 内部0000..1FFF、外部8000..FFFF |
| 1360 | 制御2000..3FFF、RAM8000..FFFF | 内部0000..1FFF、ROM窓4000..7FFFは16KiB×8。3400書込でbank切替 |
| 1401 | RAM3800..47FF、LCD6000..67FF/7000..77FF | 内部0000..1FFF、外部8000..FFFF |

新規Assemblyテンプレートは全機種ORG C000。1261/1350/1401ではROMなのでLoadに失敗する。
ROM bank指定出力、ROMパッチ適用、bank付きシンボルはない。
ROMセット登録のbank対応とアセンブラのbank対応は別機能。
制御領域へのロードはLCD/bank等の副作用を持つ。

## テスト・検証

既存AssemblerTestは3件（代表往復、ラベル/Listing/Map、相対範囲外）、DecoderTestは6件、
DisassemblerTestは2件。末尾DB化のテストはreaderが範囲外も読めるため例外を検出しない。
DesktopProjectBuilderTestにはassembly結合、ヘッダと行番号、複数1360ソース、エラー行、成果物出力等がある。
CPU/機種の既存テストはあるが、命令表との全件一致・全UI経路の一致は保証しない。

追加優先:
1. 独立期待表と全256opcode往復。TEST、NOP、UNDOC、CASE、末尾、FFFF折返し。
2. operand上下限/外側、分岐offset 0/255/256、重複、空DB、BOM/改行。
3. 全機種テンプレート、ROM/LCD/制御/ミラー、標準/拡張RAM。
4. Quick/Workspace/Build結果一致、旧成果物、事前ロード検証と部分適用防止。
5. 拡張後の式優先順位、overflow、未定義、定数循環、前方参照・位置計算。

指定JDK 21でbuild :core:desktopTest成功（既存タスクの多くはUP-TO-DATE）。
一時再現テストは新規実行し成功。GUI操作、実機全命令照合、ROM由来データは今回使用していない。

## 実装順序の提案

| 段階 | 内部的な正確性 | ユーザー向け機能 |
|---|---|---|
| 1 最優先 | TEST、末尾read、opcode保存、境界/DB検証、独立期待表 | 対応範囲と往復保証を文書化 |
| 2 配置・ロード | 共通領域情報、事前検証、BASIC衝突/ミラー、部分適用防止 | 機種別テンプレート、RAM/ROM/LCD表示、配置診断 |
| 3 基本構文 | 式AST、シンボル種別、overflow/循環/前方参照規則 | EQU、加減算/括弧/HIGH/LOW、DW、文字列DB、予約/整列 |
| 4 診断・デバッグ | ファイル/行/列/コード付き複数診断、成果物/ソース対応 | Quick出力統一、symbol jump/breakpoint、entry指定 |
| 5 複数ファイル | INCLUDE方式か独立翻訳単位/リンク方式かを選択、循環検出 | 共通定義、ファイル間参照、必要ならINCBIN |
| 6 発展 | bank付きアドレス/成果物、展開処理設計 | CASE支援、マクロ、条件、ROM bank成果物 |

段階1は独立期待値と往復/operand境界、段階2は全機種のBuild/Loadと失敗時非変更を完了条件とする。
段階3ではDWのbyte順、予約領域が埋込みか穴か、現在位置記号と$16進の競合、再定義規則を先に決める。
初期案はSETより不変EQUを優先。段階5のファイル探索はHostで処理し、Coreには入力を明示供給する。
CoreへのOS/UI/ファイル/時刻依存やスレッド所有追加は不要。

最初の承認範囲は段階1・2を推奨。段階3以降は実際に書きたいプログラムを基準に仕様化する。
commit/push、ROM追加、別作業の保留事項は対象外。

## 根拠

Core debugのSc61860Assembler/InstructionSet/InstructionDecoder/Disassembler、CPU、各機種MemoryBusとSession。
DesktopのDesktopAssemblyWorkspace、DesktopProjectBuilder、DesktopProjectArtifactLoader、
DesktopProjectWorkspaceCreator、Mainと関連テスト。
補助資料: [utz82/SC61860-Instruction-Set](https://github.com/utz82/SC61860-Instruction-Set)
（2026-10-11閲覧）。未公開動作の記述は実機仕様の確定根拠として扱わない。
