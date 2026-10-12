# SC61外部アセンブラ連携の調査

## 2026-10-12: macOS実行仕様の確認

対象はユーザーがmacOSでビルドしたSC61860 CROSS ASSEMBLER Rev 0.04a。
参照元はユーザー提供のReadme.txt、SC61860.c / SC61860.h、sc61、FENSCERのソース・DMP・LST。
公開元: https://hd61700.yukimizake.net/SC61860/
外部資料・実行ファイルは変更せず、検証用入力と生成物は一時ディレクトリに置いた。
PGPのビルド依存には追加しない。

| 検証 | 結果 |
|---|---|
| 最小ソースの正常終了 | 成功メッセージ、DMP/LST生成、終了コード2 |
| FENSCER + FENSCER_MAP.bin | 終了コード2、生成DMPはユーザー提供DMPとバイト単位で一致 |
| 不明な命令BOGUS | 終了コード17、LINE:2 ERROR CODE:017、以前のDMPはそのまま残る |
| 未定義シンボルCALL MISSING | エラーログとLSTを生成、DMPなし、終了コード0 |
| 空白・日本語を含むソースの絶対パス | 正常にDMP/LST生成 |
| 相対パスのINCBIN | ファイルが作業ディレクトリにあれば成功、作業ディレクトリを変えると失敗 |

ログは検証したケースでは標準出力に出て、標準エラーは空だった。
出力は入力パスの拡張子を.dmp / .lstへ置換した場所に生成される。
ソースのmain / InitAsmもこの命名と終了値の返却を確認できる。
Windows版の終了コード・文字コード・パス処理、およびINCLUDEの相対参照は未検証。

## 取り込み方針

- 外部コマンド実行・ログ収集はDesktopの共通処理とし、SC61の成功判定と出力解析は個別adapterで扱う。
- 終了コードだけで成功／失敗を決めない。このmacOS版は成功2、失敗0の例がある。
- 成功メッセージ `ASSEMBLY COMPLETE, NO ERRORS FOUND` と今回生成された有効なDMPを確認する。
- タイムスタンプは補助情報にできるが、時刻の精度や失敗時のLST更新があるため単独の成功判定には使わない。
- 以前のDMPが残っていても今回の成果物として取り込まない。既存出力の退避・復元や実行用コピーの方法は、相対参照とユーザーの成果物を保つ形で実装時に確定する。
- 相対INCBINのため、作業ディレクトリの選択を明確にする。初期候補は元ソースの親ディレクトリ。
- DMP本体はPGP形式と互換。末尾のDOS EOF文字0x1Aを許容する必要がある。
- LSTはまず原文表示し、行番号・アドレス・機械語・シンボル解析を段階的に追加する。
- PGP CoreにSC61実行処理やファイルAPIを持ち込まない。

## 次の作業

Studio共通設定、外部実行処理、SC61成功判定、DMP取り込みを実装し、既存の配置検査とWorkspace / Build & Loadへ接続する。

## 第2段階: 共通設定と外部実行処理

SettingsのAssembler欄に内蔵／外部の選択、実行ファイル、引数テンプレート、タイムアウトを追加した。
設定はStudio全体で保存する。引数の`{source}`は引用符の解析後に絶対パスへ置換する。
SC61の例は実行ファイルにsc61の絶対パス、引数に`{source}`。作業ディレクトリはソースの親。

同じ欄の接続テストにテスト用ソースの絶対パスを入力して実行する。
SC61はそのソースの隣のDMP/LSTを生成・上書きするため、テストコピーを使う。
実行はIO coroutine上で行い、終了時に標準出力・標準エラーをまとめたログと生の終了コードを表示する。
表示ログは先頭1 MiBまで。実行中はキャンセル可能で、設定画面を閉じても実行をキャンセルする。
タイムアウトとキャンセル時は子プロセスを終了する。シェルを介さず引数列で起動する。

この段階の設定は接続テスト用で、Workspace / Build / Quick Assembleの変換処理はまだ内蔵を使う。
SC61成功判定、既存生成物の管理、DMP取り込みは第3・4段階で接続する。
Windowsのログ文字コードは未確認で、現段階の表示はUTF-8。

## 第3段階: SC61 DMP取り込み

接続テストはSC61用adapterを通し、成功ログと新規DMPを確認する。
元ソースを一時フォルダのinput.asmへコピーし、元ソースの親を作業ディレクトリにして実行する。
SC61の生成物は一時フォルダに隔離され、元ソース隣の既存DMP/LSTには触れない。
成功・失敗・キャンセル後に一時フォルダを削除する。主ソース名を参照する構文がある他ツールの互換性は対象外。
相対INCBINは実際のSC61で確認済み。INCLUDEとWindowsは未確認。

Desktopの共通DMP loaderで末尾の単一DOS EOF文字を許容する（途中に現れる場合はエラー）。
取り込み後は既存MemoryPlacementValidatorでROM／未割当／物理ミラー重複を検査し、成功時はbyte数と範囲を表示する。
稼働中のBASICとの衝突と複数ソース間の重複は、後続のBuild / Load統合で既存処理へ接続する。
Build / Quick Assembleはこの段階でも内蔵のままで、接続テストはメモリ書込みを行わない。

実装したadapterによるユーザー提供macOS SC61/FENSCERの検証結果:
- PC-1350の配置記述では13,568byteを取り込み成功（PC-1350でのゲーム実行を検証した意味ではない）。
- PC-1261では0x6800が未割当なので拒否。元DMPは0x4000..0x74FFまであり、形式互換と機種への配置可否は別。
- 自動テストで成功コード2、失敗コード0、欠落／不正DMP、末尾EOF、ROM／ミラー検査、相対入力、既存成果物の保全と一時出力の削除を確認する。

## 第4段階: Workspace / Build / Load / Quick Assemble

Studioの保存した共通設定を各操作の開始時に取得し、内蔵またはSC61 adapterを選ぶ。
WorkspaceとBuildは同じcompileConfigured経路を使い、Buildはその結果を再利用して全入力を検査する。
失敗時に診断のためのSC61再実行は行わない。成功時のログはWorkspaceで表示し、失敗ログは診断に含める。
外部処理はcoroutine / IOで実行し、同時のAssembly / Buildを防ぎ、Workspaceにキャンセルを設ける。
実行途中でProjectやロード先runnerが切り替わった場合は、古い成果物を新しい対象へLoadしない。

Buildは既存DesktopProjectBuilderの複数入力重複・BASIC配置検査へ接続し、全入力成功後に保存する。
Build & Loadは既存DesktopProjectArtifactLoaderを使用し、現在のBASIC領域との衝突も再検証して一括Loadする。
Quick Assembleも同じ選択で変換し、外部の場合は現在機種の配置検査を通してDMP保存する（Loadはしない）。

LST解析は第5段階。外部成果物のListingには未取り込みの説明を表示し、Symbol Mapは未提供。
内部のbyte所有情報はDMPから取得するため、LST解析なしでも複数Assembly入力の重複を検出できる。
外部の行番号付き診断も第5段階で追加する。

検証: JDK 21の全体buildと既存／追加テストを実行。
外部結果のBuildとエミュレータLoad、失敗後の旧出力保持、Assembly同士／DMPとの重複拒否、内蔵への切替を自動確認。
Windows版SC61とGUIの手動操作は未検証。

## 第5段階: LST / Symbol / Diagnostic

SC61の一時LSTを削除前に取り込み、WorkspaceのListing表示とbuild/*.lst保存に原文を使用する。
一時ソースのヘッダパスは元ソースへ置換する。通常の空白整形と/tab形式、機械語の継続行を解析する。
原文にはサイクル数・IFによる行番号の飛び・INCBIN等も保持される。
LSTと参照ソースはUTF-8を優先し、デコード失敗時はWindows-31Jへfallbackする。
LSTが欠落・解析不能でもDMPのBuild / Loadは利用でき、Listingには未取り込みの説明を出す。

末尾のMAP LISTをSymbol Mapへ変換し、大小文字を維持する。
表示された宣言からEQU定数とラベルを区別し、元ファイルと行番号を付ける。
NOLIST等で宣言が不明なシンボルは分類・定義位置を確定できず、位置を?として表示する。

エラーログのLINE / ERROR CODEと印字行から元ソースを照合し、WorkspaceのOpen at lineへ接続する。
実際のmacOS SC61でINCLUDE成功と失敗を確認したところ、LST/ログにはINCLUDE内のファイル名が出ず、行番号がファイルごとに再開する。
元ソースと参照INCLUDEの同じ行番号・内容を照合し、一意に決まる場合だけファイル・行を採用する。
曖昧な場合は行番号を補わずログを保持し、ボタンをOpen sourceとする。
参照はSC61と同じ元ソース親基準で解決し、最大256ファイル、各2 MiBまでとする。LSTは16 MiBまで。

DMPの全セグメントからbyte所有権を検査するよう変更した。
INCBIN、NOLIST等でLSTのbyte列が省略されても重複検査をすり抜けない。
添付FENSCERのLSTは6,533 numbered rows、955 symbolsを解析し、955 symbolの元定義位置を特定できた。
642行目のaddress=430C、bytes=74 D8、cycles=4、DF_VERSIONのEQU値=9967を確認した。

自動検証: 空白／TAB整形、継続byte、Windows-31Jコメント、EQU/label、INCLUDE診断と曖昧性、
LST/MapのBuild保存、LSTのbyte列欠落時の重複拒否、外部エラーの行番号伝達。
JDK 21の全体buildとテストを実行。Windows実行とGUIの手動確認は後続。

## 第6段階: macOS GUI確認と利用手順

2026-10-12、JDK 21でdesktopApp:runと最新macOSアプリのcreateDistributableを実行。
操作確認は更新したアプリパッケージで行い、既存ROMを利用したPC-1261の検証用Projectを一時フォルダに作成した。
IF、INCLUDE、INCBIN、.H/.L、&B、単引用符文字列、CASE1/CASE2/DLを含む19byteの入力で確認した。

確認結果:
- SettingsでExternalとユーザーのsc61絶対パスを保存し、設定画面を開き直して保持を確認。
- 接続テスト: 19byteを取り込み、DMP範囲と成功ログを表示。
- Workspace: Assembleの成功、ログ、Memory/Disassembly/LST/Symbol表示、Buildでmain.dmp/main.lst/main.mapの保存。
- Build & Load: PC-1261へ0 BASIC / 19 machine-code bytesのロード成功を実画面で確認。
- BOGUSを13行目に追加し、Build & Load失敗、以前のプレビューとロード済み版の保持を確認。保存済み3出力のSHA-256は変更なし。
- Open at lineでVS Codeが元main.asmの13行目・1列目を開いたことを確認。
- Quick Assemble: DMP保存成功、Build出力とアドレス・byte一致。
- Internalへ切替・Apply後のAssembleは4行目IFで内蔵の診断になり、共通設定の反映を確認。検証後はExternal/sc61に戻してApplyした。

GUI確認でDMPの8byte行境界をまたぐCASE1が分断される表示を発見し、Disassemblyの表示時だけ隣接セグメントを連結する修正を行った。
命令境界とアドレスgapを扱う回帰テストを追加し、JDK 21の全体buildとmacOS配布パッケージ生成が成功。
修正後のGUI再確認は未実施だが、隣接行をまたぐCASE1とgapを保持することは回帰テストで確認。

Windows版SC61.exe Rev 0.04a (PE32)も一時Wine 11.0環境で確認した。
- 初期化と実行は専用WINEPREFIXで行い、ユーザーの通常Wine設定を変更しない。
- 空白を含むフォルダで、上記19byte入力と相対INCLUDE/INCBINが成功。
- macOS版と生成されたアドレス・byteが一致（DMPの改行・checksum表記等のテキスト差は除く）。
- 成功code=2、不明命令code=17、未定義operandエラーcode=0、失敗後の旧DMP保持を確認。
- WineでのSC61実行はWindows上のStudio GUI確認を代替しない。Windows実機、日本語パス、実際のWindowsログ文字コードは残る。

利用者向け設定・操作・制約とWindows実機のチェックリストはEXTERNAL_ASSEMBLER.mdに記録した。
FENSCERについては形式/adapterを検証済みだが、PC-1261での出力範囲0x6800以降の配置問題とゲーム実行は未解決・未検証。

## BuildとLoadの配置検査を分離（2026-10-12ユーザー方針）

ユーザーは少なくともBuildで0x6800以降を許容し、Loadは別扱いと指定した。
StudioのAssemble / Build / Quick Assembleは対象機種の書込み範囲・物理ミラー・BASIC配置で成果物を拒否しない。
同一論理アドレス重複、不正DMP、16bit範囲、SC61実行エラーは引き続き検出する。
Build & Loadは全成果物を保存した後、既存のprogram loaderで機種の配置と現在のBASIC領域を検査する。
Loadの検査・原子的な書込みは維持し、未割当部分の切り捨てやRAM拡張は行わない。
内部のcompile / buildには明示的なLoad向け事前検査モードを残し、回帰テストに使用する。

実際のSC61とユーザー提供FENSCERのコピーで、PC-1261 ProjectのBuildが13,568byte / 955 symbolsで成功し、DMP/LST/Mapを保存した。
0x6800の出力を含むBuild成功と、Loadが部分書込みなしで拒否する回帰テストを追加。
JDK 21全体build、テスト、macOS配布アプリの生成を確認。
以前の第3〜6段階にある配置エラーの記録は当時の挙動で、現行のBuildでは解消している。

## Windows CIの日本語引数検証（2026-10-13）

外部連携の初回Windows CIはコンパイル成功後、DesktopExternalAssemblerTestの日本語パスを含むログ照合で失敗した。
まず子プログラムのUTF-8出力を明示し、ホスト既定のstdout/stderr文字コードがwindows-1252でもテスト出力が変わらないようにした。
これだけではWindows CIは失敗したため、Gradleの失敗例外を全文表示し、照合項目を識別できるメッセージを追加した。
詳細ログでは、子JVMのargvにある日本語が?へ変換される一方、作業ディレクトリの日本語は保持されていた。

Windowsのこの実行テストにはUnicode引数を扱うPowerShell fixtureを使い、UTF-8ログ、終了コード2、元の日本語ソース引数の完全一致を検証する。
作業ディレクトリはその場所へのmarkerファイル作成で確認し、短いパス表記と正規パス表記の違いに依存しない。
macOS/LinuxではJava fixtureを使用し、タイムアウト・キャンセルのJava fixtureはWindowsでも継続する。
日本語引数の検査を削除したり、Windowsでテストをskipしたりはしていない。
これはテスト用プログラムの修正で、SC61本体やStudioの実行処理は変更していない。
