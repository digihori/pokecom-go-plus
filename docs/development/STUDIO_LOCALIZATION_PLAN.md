# Studio 日本語／英語UI対応 — 作業整理

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

2026-10-14。初期計画と順次作業の実装・検証記録。最終結果と未確認事項は[検証記録](STUDIO_LOCALIZATION_VERIFICATION.md)を参照。
既存のROADMAPの言語方針を基準とし、将来項目を今回の必須実装へ混ぜない。

## 1. 今回の範囲

この節は当初依頼の範囲。設定からの言語上書きは、その後のユーザー追加指示により実装した（節17）。実行中切替は追加しない。

Pokecom GO Studio（macOS／Windows／Linux）のUIを日本語・英語に対応させる。
起動時にOSの言語設定から言語を選び、日本語なら日本語、その他は英語へフォールバックする。
日本語／英語をテストから明示注入できる構成にする。
設定からの言語上書き・実行中の言語切替UIは将来項目とし、今回は実装を前提にしない。

対象:

- メニューバー、メイン画面、各ウィンドウ・ダイアログ、設定、About、Help、確認・進捗・成功／失敗通知。
- ROM Libraryのfamily表示、登録／置換／削除、Create ROM Set、ファイル選択のアプリ側タイトル。
- Project、Build & Load、Assembly Workspace、外部Assembler／外部エディタ設定。
- Debugger、CPU／Memory／Trace、Breakpoint／Watch、停止理由と操作案内。
- BASIC／メモリ入出力、Quick Assemble、WAV Tool、Character Editor。
- Host input、ゲームパッド設定・接続状態・割当・検証表示。
- AI／MCPの設定・接続・権限等、Studio内に表示する状態や説明。
- アクセシビリティ用の操作説明・contentDescription等。

翻訳しないもの:

- BASIC／Assemblyのソース、命令・予約語、機種名・製品名、ポケコンのキー刻印、LCD内のROM出力。
- Project／ROM package／設定の保存key、機種ID、JSON field、schema ID、MCP tool名・プロトコル内容、URL、パス。
- ROMデータ、バイナリー・HEX表現、ユーザーのファイル名・Project名・コメント・プログラム。
- 外部Assemblerの生のstdout／stderr・原文診断、OSやデバイスから返る原文情報。アプリ側の説明を翻訳して添える。

Player全体のローカライズ、Player仮文言整理、機能追加・機種互換方針変更は対象外。
controllerUi等の共有部品を変更する場合は既存のAndroid等の表示とAPI互換を保つ。

## 2. 現状と実装方針

現状確認:

- desktopAppはKotlin/JVM＋Compose Desktop。画面・通知・エラーの文言がMain.ktと各feature内に直書きされている。
- Main.ktにはエラー型からmessageへ変換する処理があり、ここも翻訳対象になる。
- controllerUiのControllerSettingsには日本語の直書きがあり、Studioから使う共有画面を含めて言語を揃える必要がある。
- 現在、Studio用の文言リソースや統一したUI言語選択は見当たらない。

推奨する構成（実装時に小さな画面で検証して確定）:

1. desktopAppにUI言語選択と文字列取得を集約する。標準JVM ResourceBundle等の軽量な仕組みを第一候補とし、初期段階で外部翻訳サービスや大きな基盤を追加しない。
2. 英語を既定リソース、日本語を対応リソースとする。画面単位で安定した意味のkeyを付け、翻訳文自体を識別子にしない。
3. 機種名・パス・件数等は引数として渡す。文の断片を連結せず、語順を言語ごとに変えられる形式にする。改行・引用符・波括弧等のエスケープを検証する。
4. Localeの読取りはDesktop側の境界に置く。UI用言語を明示保持し、Locale.setDefaultのようなプロセス全体の変更でソース・数値・保存形式へ影響を広げない。
5. controllerUiには言語に依存するラベル集合等を渡せるようにし、OS APIやdesktopAppへの依存をcommonMainへ持ち込まない。共有画面は一つの言語で表示する。
6. Coreの型付きエラー・code・データは維持し、UI側の表示変換で翻訳する。構造化されていない内部reason文字列は棚卸しし、翻訳できる説明と元の詳細を分離する。Coreのエミュレーション処理へLocaleやUIリソース依存を追加しない。
7. キーボードショートカット・操作・設定保存は維持し、言語によるラベル長、フォント、折返し、幅・高さの不足を画面側で調整する。

OSファイルダイアログの標準ボタンはOS側の言語に従う。アプリ側タイトルと混同して、全表示をアプリが制御できるとは扱わない。

## 3. 作業順序

1. 文言の棚卸しと用語表
   - Main、各feature、controllerUi、hostInput由来の状態・エラーを調べる。
   - UI用／外部仕様の識別子／ユーザーデータ／生ログを分類する。
   - Project、Build、Load、ROM登録、Breakpoint、Watch等の日本語表記を統一する。
2. 言語・文言取得の基盤
   - OS言語選択、英語fallback、引数付き文言、テスト用言語注入を用意。
   - メインメニューと小さなダイアログで日本語／英語の表示・パッケージ内リソースを確認する。
3. 基本画面とファイル操作
   - メイン、設定、ROM、Project、BASIC／メモリ入出力と関連する確認・結果・失敗通知。
4. 各ツールと共有部品
   - Debugger、Assembler、WAV、Character Editor、ゲームパッド、MCPのUI。
   - エラー変換・状態ラベルも同じfeature単位で移行し、通知だけ英語が残る状態を避ける。
5. 漏れ・レイアウト・互換性確認
   - 日英リソースのkeyと引数の一致、直書きの残存を調べる。
   - 長い翻訳・パス・機種名、ウィンドウ縮小、ダイアログ・スクロール・メニューバーを確認する。
   - ドキュメントと検証結果を更新する。

巨大なMain.kt全体の構造変更や別機能のリファクタリングは同時に進めず、文言置換に必要な範囲で分割する。

## 4. 完了条件・検証

- 日本語環境で日本語、英語／未対応言語環境で英語が選ばれる。
- 対象画面のラベル・説明・通知・確認・エラーを日英で表示できる。
- 全keyと引数が一致し、未定義keyやformatter errorを検出できる。
- 機種ID、保存データ、Project／ROM package、MCP仕様、ソース、HEX／数値形式が言語切替で変わらない。
- エラーは代表的なROM不正・Project失敗・容量超過・Assembler失敗・MCP状態を確認する。
- 日本語／英語の主要画面を実表示で確認する。Macのローカル確認、Windows／Linuxの確認結果を区別して記録する。
- 共有部品の既存利用元に回帰がないことを確認する。
- JDK 21でAGENTS.md指定の全体buildとCore desktop tests、文言・表示変換に応じたStudio／共有UI testsを行う。

## 5. 次のスレッドへの引継ぎ

- 対象リポジトリは現在のpokecom-go-plus。最初にAGENTS.mdとgit statusを読み直す。
- 最終commitはce4afbf（1260／1402の容量別モデルとfamily登録表示）。
- その後の未commit変更として、1470Uの↕入力接続・テスト・関連文書がある。ローカライズへ混ぜて変更・取り消し・commitしない。
- TITLE機能の詳細はユーザー指示でペンディング。「↕︎」の絵文字表示も別の保留課題。LCD比率はユーザー確認済み。
- drawable-nodpiの作成中skin 6枚は変更・commit対象外。開始時の追加変更も保護する。
- ROM／ROM由来fixtureをGitへ追加しない。参照projectはread-only。Core commonMainへOS／UI／filesystem／時刻／thread APIを追加しない。
- packageVersionは1.3.1を維持。commit／pushは改めて明示指示があるまで行わない。
- 本文書とROADMAP／TASK_REVIEWを読み、今回はStudioの日英対応として進める。Player・RAM拡張・Project互換方針等は別件。

## 6. 実装進捗（2026-10-14）

全面対応は未完了。文言の所在・分類・用語表は[棚卸し](STUDIO_LOCALIZATION_INVENTORY.md)に記録。

- DesktopにStudioLanguage／StudioStringsを追加。標準JVM ResourceBundle、英語既定、日本語対応、MessageFormatの位置引数を採用。OS言語は起動時に一度取得し、ja以外は英語。プロセス既定Localeを変更しない。
- 言語を明示したStudioStringsをテストへ注入。メニュー・About／Help・設定主要説明・メイン基本操作・Projectの一部操作説明・Create ROM Setの画面文言・外部Assembler設定画面を移行。
- 共有ControllerSettingsにホスト注入用ControllerStringsを追加。日本語既定値でAndroid既存呼出しを維持。Studioの保存／利用不可通知もリソース化。
- 内部エラー変換、ROM管理・Project・Debugger・BASIC／メモリ入出力・Quick Assemble・WAV・Character Editor・MCP本文、残存説明・状態・アクセシビリティ・全画面レイアウトは継続作業。
- macOS/JDK 21: `./gradlew --gradle-user-home .gradle-local :desktopApp:test :core:desktopTest build` 成功。日英key・位置引数一致、フォーマット、引用済み波括弧、長いパス保持、OS言語選択、プロセスLocale不変、共有ラベル注入を検査。保存形式全体の言語別比較は未実施。
- 独立したLocalizationPreview（testソース）を追加。実設定、ROM Library、SDL、エミュレーターへアクセスせず日英小画面を表示する。macOSでプロセス起動まで確認したが、GUIツールがJavaのapp IDを選択できず、目視のレイアウト／メニュー／縮小検証は未完了。Windows／Linuxの実表示確認は未実施。
- Androidを含む全体build成功。共有部品の実表示・操作回帰は未実施。全体buildの初回ではAndroid lintがKotlin metadata版について診断を出したが、終了条件はローカライズテストのリソース読取ミスによる失敗。テスト修正後の再実行は成功。
- 初期の1470U変更、関連テスト・文書、スキン6枚を維持。Coreへローカライズ変更を加えていない。packageVersionは1.3.1。commit／pushは行っていない。

## 7. 順次作業ステップ1（2026-10-14）

棚卸し・用語の確定を完了。[判断規則・個別判断](STUDIO_LOCALIZATION_INVENTORY.md)に翻訳／原文維持／説明＋原文、文脈別用語、後続ステップを記録。移行済み日英リソースも確認した。今回の変更は文書のみ。

注意点として、CheckpointのstopReasonがUI表示とJSON保存に共用されていること、WAVのタブ文字列が分岐値を兼ねていること、WAV概要／レポート・Assembly成果物の表示と出力の境界、既定Project名の保存データとしての扱いを記録。既存Resetの翻訳がCheckpoint保存へ流れる可能性は未修正であり、互換性保護を後続実装の確認項目とする。

ステップ2〜10は未完了。小画面の表示・基盤検証から順に進める。今回build／testsは再実行せず、文書の差分・参照箇所と保護対象を確認。commit／pushなし。

## 8. 順次作業ステップ2（2026-10-14）

ResourceBundle＋位置引数の構成を採用。言語判定はDesktopの起動境界でJVMが提供するOS由来の表示Localeを一度読み、日本語は日本語、それ以外は英語。Core・共有commonMainにLocale依存を追加せず、Locale.setDefaultも使用しない。設定上書き／実行中切替UIは追加していない。

### 実装・自動検証

- StudioStringsの明示言語注入に加え、テスト専用bundleを注入し、日本語にないkeyが英語既定リソースへfallbackすることを確認。JVM既定言語への追加fallbackは無効化したまま。
- 日英key一致、重複key、引数index一致、語順変更、引用符・波括弧・改行、長いパスの原文保持、未定義keyの検出を検証。文言取得時に引数不足／過剰も検出する。
- CheckpointへUIのReset訳が流れる経路を修正。画面のdisplayNameと保存のcheckpointDescriptionを分離し、保存側は従来の英語表現を使用。Resetの英日表示と英語保存値、writerのstopReasonフィールドをテスト。後続の停止理由／Fault翻訳も保存経路へ流れないよう維持する。
- 小画面で見つかった共有コントローラーのABXY位置説明をホスト注入対象へ追加。日本語既定値は既存と同じ。英語の図内未割当は幅に収まる `None`、選択肢等は `Unassigned`。キー刻印・enum・保存値・図形配置は維持。
- `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render' :desktopApp:test :core:desktopTest build` 成功。StudioStringsTestは6件成功。全体buildには共有UIのAndroid／Desktopコンパイルと既存Player・hostInput testsを含む。共有Android実機の操作確認とは区別する。

### macOSでの表示検証と限界

- LocalizationPreviewはtestソースの独立画面。実設定・ROM Library・SDL・エミュレーターを生成しない。画面と画像レンダリングで同じプレビュー本文と実際のControllerSettingsを使い、Studioテーマを適用。
- `./gradlew :desktopApp:localizationPreview` で独立日英ウィンドウを起動可能。`--args='--render'` ではComposeのImageComposeSceneでPNGを生成し、GUIツールのJava app選択失敗に依存せずレイアウトを確認できる。新しいビルド時依存は追加していない。
- 出力: `desktopApp/build/localization-preview/{english,japanese}-{normal,narrow,dialog,menu}.png`。生成画像はbuild以下で、Gitへ追加しない。
- macOS/JDK 21で8枚をレンダリングして画像を目視確認。600×780の説明・長いパス・共有コントローラー図、420×620の縮小と折返し、480×420のAbout相当ダイアログとComposeメニュー項目を確認。英語図内の不自然なUnassigned改行と日本語位置説明の混在を修正し、再生成後の表示を確認。
- 縮小画像では下部はスクロール領域の外となる。ネイティブウィンドウのドラッグ縮小・実スクロール操作・OSネイティブMenuBarの操作は未確認。今回のメニュー画像はCompose DropdownMenuであり、ネイティブMenuBarの証拠として扱わない。
- 日英言語判定は明示Locale注入による自動検証。OS言語設定の実変更／複数OS環境による起動試験は未実施。Windows／Linuxの実表示・ネイティブメニュー操作も未実施。
- 全画面の保存データ・ソース・HEXの言語別比較、アクセシビリティ操作、実機ゲームパッド確認は後続ステップ。

ステップ2の基盤・独立小画面の画像確認を完了。OSネイティブ操作と他OS実表示の未検証項目を保持し、次はステップ3の基本画面仕上げ。1470U関連変更・スキン・実設定・ROM Libraryを維持し、packageVersionは1.3.1。commit／pushなし。

## 9. 順次作業ステップ3（2026-10-14）

メイン・設定・About／Helpの基本画面文言を仕上げた。後続のROM／Project／ファイル操作／Debugger等から渡る通知は、それぞれのステップで移行する。

- メイン: 機種ラベル、ROM登録／未ロード表示、開いているProject名、基本実行状態・サイクル数、実行／一時停止／リセット／ステップ完了、既存RAMモード選択・再起動通知、音声利用不可、プロジェクト外のロード済みプログラム表示をリソース化。既存RAM機能の通知だけを翻訳し、RAM対応を追加していない。
- 設定: 速度ラベル（標準／無制限を含む）をリソース化。DesktopExecutionSpeedから未使用の直書きlabelを除去し、速度比とenum名は維持。設定保存は引き続きenum.nameを使用。
- 外部エディター: コマンド例・引用方法・{file}省略の説明、既知のコマンド検証／設定要求／VS Code未検出の表示を翻訳。開く操作の成功／失敗も移行。コマンド展開・起動処理を変更せず、未知のOSエラーは翻訳済み概要に原文詳細を添える。
- 入力: 「ホスト入力」は翻訳し、Logical／Physicalは英語表記を維持（2026-10-15ユーザー方針）。メイン画面へ説明を追加しない。選択状態のsemantics、速度RadioButtonの操作説明、LCD Canvasの機種付き説明を追加。実機のキー刻印・LCD出力・RUN/PRO/RSV・機種ID・設定keyは維持。
- メインのProject概要の状態ラベル・変更あり・更新・ファイル読込中は移行。Projectツリー・作成／ビルドの処理通知・エラーはステップ5。
- 基本停止理由（ユーザー一時停止／ステップ完了）とCoreFault.UnsupportedOpcodeのUI表示を翻訳。Coreの型・処理は変更していない。Checkpointは英語の表示引数を明示し、Faultの入れ子説明まで従来表現を維持。Breakpoint／Watch等の詳細停止理由はステップ7。
- About／Helpは既存移行を確認。製品名・URL・著作権本文・テーマIDは原文維持。

### 検証

- `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render' :desktopApp:test :core:desktopTest build` 成功。最後のメイン状態・アクセシビリティ補完後も `:desktopApp:test :core:desktopTest build` 成功。
- StudioStringsTestは7件成功。日英key・引数・重複検査に加え、全速度／RunnerStateのラベル取得、速度の保存名による復元、コマンド例の{file}/{line}原文、ユーザーコマンド／パスの保持、未知OS診断の保持、入れ子Faultの英語Checkpoint保存表現を検証。
- macOSで独立プレビューの `japanese-basic.png`／`english-basic.png`（600×780）と `japanese-basic-narrow.png`／`english-basic-narrow.png`（420×780）を画像で確認。実際のHostKeyInputModeSelectorへ日英Stringsを注入。長いパス、エディター例、速度ラベル、入力選択、エラー、状態通知の折返しを確認。縮小時の下部はスクロール領域外になる。
- 実設定・ROM Libraryを使う本番Appの起動は行っていない。プレビューの速度行はラベル見本であり本番の設定保存操作はしない。OSネイティブメニューバー、実スクロール・ウィンドウ縮小操作、画面全体の配置、Windows／Linuxの実表示は未確認。従前の確認結果とは区別する。
- 既存1470U差分・Coreテスト・スキンは保護。packageVersionは1.3.1、commit／pushなし。

次はステップ4のROM管理／Create ROM Set仕上げ。メインに表示されるROM操作由来の通知とエラーもそこで移行する。

## 10. 順次作業ステップ4（2026-10-14）

ROM管理・Create ROM Setの画面文言と表示エラーを仕上げた。

- ROM管理: Libraryの説明、登録／検証状態、取込元名、登録／置換／削除、削除確認、読込み／取込み／保存／削除の結果通知とファイル選択タイトルをリソース化。
- family表示は機種名を原文のまま引数へ渡し、family説明だけ日英化。代表登録機種と容量別の実行機種、125x／126x／140xのLibrary共用方針・保存先は維持。カタログlabelの既存呼出しは英語既定、Studioは表示言語を明示。
- CoreのRomImportError／CreateSessionError／RomPackageValidationErrorとDesktopRomPackageErrorをStudioRomStringsで表示変換。全型の分岐を列挙し、サイズ・機種ID・component ID・パス・hashを改変せず引数へ渡す。Validation.errorsのtoString表示を翻訳済み説明へ変更。
- Library Failureに省略可能な型付きerrorを追加し、パッケージ検証／Session生成の元エラーを表示へ渡す。既存のmessageは非UI診断として維持。インストール・ファイル操作・metadata・保存形式は維持。
- Create ROM Set: 入力検証をRomSetInputIssueとして取り出し、既存inputErrorの英語診断を維持しつつUIを翻訳。選択後の再検証も型付き例外で伝達する。出力による入力ROMの置換禁止・既存保存先の確認・ROM変換不正を翻訳。未知のOS例外、ZIP／JSONの原文診断、非構造化のInvalidRomSet.reasonは翻訳済み概要の後に原文詳細として保持し、推測による全文置換をしない。
- 表示確認用にROM管理の1行をRomLibraryRowへ抽出。Main全体の構造は維持。長い取込元名は行内で折り返し、操作ボタンと12 dpの間隔を確保。ファイル選択欄もパスと参照ボタンの間隔を確保。
- 同名操作ボタンに機種／family付き説明、ROMファイル選択にラベル・パス付き説明を追加。機種名・元ファイル名・パス・ROMバイト・hash・ID・JSON fieldは翻訳しない。

### 検証

- JDK 21で `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render' :desktopApp:test :core:desktopTest build` 成功。StudioStringsTest 7件、追加StudioRomStringsTest 5件が成功。既存ROM Library／Creator／容量familyテストを含むStudio testsと全体build成功。
- テスト用に作成した一時フォルダーとゼロ等の合成データだけを使用。実設定・実ROM Libraryへアクセスせず、ROM／ROM由来fixtureを追加していない。
- 全manifest検証エラーの表示取得、サイズ／ID／パス／hash保持、元ZIP診断・OS診断保持、型付きLibrary失敗、inputError互換、入力ROM上書き防止と失敗時の出力未作成を検証。
- 日英文言取得の前後に作成した.pgromのZIP項目名、manifest本文、hash、合成コンポーネント内容をバイト比較して一致。ZIPの作成時刻は比較から除外。ファミリーの登録ID・共有保存先も一致を確認。
- macOSの独立Compose画像: 日英のROM概要／実際のRomLibraryRow・RomFileField、通常幅600×900と縮小幅420×900、削除確認／既存ROMセット置換確認480×420の計8枚を確認。長いパス・ファイル名・family名、未登録、検証済み、容量不正、作成成功／進捗の表示を確認し、余白調整後に再生成した。
- 本番Studioでの実ROM操作、OSファイル選択、実ウィンドウ縮小／スクロール操作は未実施。Windows／Linuxの実表示も未確認。画像は独立見本で、ROM管理と作成画面の全操作を実行した証拠ではない。後続の全体確認に保持する。
- Core実装・既存1470U変更・作成中スキンを変更せず、packageVersionは1.3.1。commit／pushなし。

次はステップ5のProject・Assembly Workspace・Build & Load。

## 11. 順次作業ステップ5（2026-10-14）

Project・Assembly Workspace・Build & Loadの表示文言を移行。

- Projectの作成／名称変更、初期ソース選択、フォルダー選択、開く／閉じる／ソース追加／manifest更新、外部変更検出、登録済み／未登録、ソース情報・空表示・通知を日英化。Trackedはmanifest登録状態として「登録済み」と表示し、Gitの追跡と区別する。
- Assemble／Build／Build & Loadの進捗、中止、成功／失敗、プレビュー保持、ファイル保存とエミュレーターロードの区別、機種不一致、以前のロードを保持する状態、診断件数・操作案内をリソース化。既存の単一機種互換判定やロード後の実行方針は維持。
- Workspaceの見出し、ソース・成果物の状態、診断位置を開く操作、メモリ配置・容量説明、結果・全コピー・空結果を移行。原文ログ、ユーザーのソース／コメント／名前／パス、命令・HEX、Listing／Symbol Map／DMP本文は維持。
- ProjectOpen／Create／Manifest／Buildの型付きエラーをUI側で翻訳し、明示Strings注入でテスト可能にした。Buildから使うBASICコンパイル／容量・メモリ配置／DMPの型付き説明も対応。非構造化のCore parser／Assembler／OS診断は翻訳済みの概要・位置説明に原文詳細を添える。Coreの型・code・messageを変更していない。
- Project操作・SC61の既知のアプリ側診断（既存フォルダー、対応ソース種別、タイムアウト、DMP未生成、終了コード等）を表示変換。外部Assembler設定の検証と接続失敗にも同じ表示変換を適用し、生stdout/stderrは保持。
- メモリ概要／配置レンダラーに表示用Strings引数を追加。従来の引数省略時は原文を維持し、UIだけ明示言語を渡す。生成Listing／Map／DMPの出力経路は変更していない。
- 初期ソース選択をProjectStarterOptionsへ抽出し、UI説明をリソース化。DesktopProjectStarterのenum名・順序・生成内容は維持。既定Project名「New PGP Project」とフォルダー名・番号付けも保存データとして維持。
- 長いソース／診断の表示幅を制限してアクションボタンを確保。サイドバーの長いボタン名は折返しを許可。初期ソースCheckboxとProjectツリー選択に説明・選択状態を追加。共通エラーダイアログの見出しはROM以外の呼出しにも合う「エラー」にした。

### 検証

- JDK 21で `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render' :desktopApp:test :core:desktopTest build` 成功。既存Project testsを含むStudio tests、Core tests、全体build成功。追加StudioProjectStringsTestは4件成功、全リソース検査も成功。
- 一時フォルダーで日英UI文言取得を挟み、同じ初期Assembly Projectを作成・内蔵Assemblerでビルド。manifest・初期ソース・DMP・Listing・Symbol Mapを項目名とバイト内容で比較して一致。既定Project表示名／フォルダー名も一致。実Project・実設定・ROM Libraryを使わず、外部ツールも追加テストから起動していない。
- 全ProjectManifestErrorの表示、型付きProjectOpen／Create／Buildエラーの位置・ID・パス・loadAddress／HEX保持、既知操作エラーと原文ログ保持、表示用メモリ配置と従来レンダリングの分離を検証。
- macOSで独立Composeプレビュー6枚を確認: 日英Workspace見本の600×1100／420×1100、作成ダイアログ480×650。ProjectStarterOptionsとAssemblyOutputFieldは本番と同じ表示部品へ日英Stringsを注入。長いProject名・パス、ビルド操作・状態・診断、初期ソース選択、原文Assembly／HEXの表示と折返しを確認。下部通知はスクロール領域外になる場合がある。
- Workspaceプレビューは代表状態の見本。実Projectツリーのドラッグ／外部変更検知／OSフォルダー選択／エミュレーターへの実ロード、実スクロール・縮小操作は実行していない。Windows／Linuxの実表示も未確認。これらはステップ10の実操作確認に保持。
- StandaloneのBASIC／メモリ入出力とQuick Assembleの通知・ファイル操作はステップ6。Debugger固有の停止理由／パネル等はステップ7。別機能の拡張、Core／1470Uキー修正、スキンを変更せず、packageVersionは1.3.1。commit／pushなし。

次はステップ6のBASIC／メモリ入出力・Quick Assemble。

## 12. 順次作業ステップ6（2026-10-14）

BASIC／メモリ入出力・Quick Assembleの文言移行を完了。

- 単独ロード／追加ロード・プロジェクト外の説明、BASICの直接ロード・ROM経由入力・保存、メモリダンプのロード・範囲指定・保存、Quick Assembleのファイル選択・成功／失敗通知をリソース化。ファイル名・拡張子filter・既定保存名、バイト数・セグメント数・アドレスは維持。
- ROM入力の未対応文字・記号・生バイトと行列位置を、型付きDesktopBasicLoadErrorからUI側で表示変換。BASICロード停止時はExecutionStatus.toStringではなくCoreFaultの表示変換を使用。
- 既知の内部BASIC／DMP／Assembler診断をStudioTransferStringsで翻訳。行番号、エスケープ、開始／終了マーカー、バイト書式、相対分岐範囲、未定義シンボル等を対象にし、元のCore型・code・messageは維持。未知の内部診断は日本語UIで「詳細（原文）」を添えて保持。英語UIの原文詳細は維持。
- 外部Assemblerのアプリ側失敗説明は翻訳し、生stdout/stderrは変換せず別に添える。ソース本文・予約語・命令・ユーザーコメント・パス・記号名・HEX・数値リテラルは翻訳しない。
- メモリ保存ダイアログの入力部をMemoryDumpRangeFieldsへ抽出。表示に日英Stringsを注入可能にし、アドレス解析・範囲検証・メモリアクセス・保存操作は元の呼出し側に維持。Start／End／逆転範囲の共通ラベル・説明は他の範囲ダイアログからも利用するが、Debugger固有の保存操作はステップ7。
- Quick Assembleの結果画面、Assembler機能拡張、Raw Binary／Intel HEX導線などの将来項目は追加していない。

### 検証

- JDK 21で `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render' :desktopApp:test :core:desktopTest build` 成功。追加StudioTransferStringsTestは4件成功。全リソースkey／引数・重複検査、既存Studio tests、Core tests、全体build成功。
- 合成のBASICソース（1.25を含む）、DMP、内蔵Assemblerの短いソースで、日英UI文言取得の前後のBASICコンパイルバイト・復号ソース・DMP再出力・AssemblyのDMP出力を比較して一致。BASICの小数点とHEXアドレスも維持。
- 既存parseHexAddressの0000／FFFF、0x／0X、空白、範囲外・不正文字の解釈を検証。保存通知の開始／終了アドレスを確認。UnsupportedRomInputの機種名・位置・生バイト、実際のBASIC／DMPパーサー診断の日本語表示、未知診断と原文詳細の保持を検証。
- 波括弧を含むユーザーファイル名と外部ログをMessageFormat引数へ渡し、内容がそのまま保持されることを検証。追加テストでは実設定・ROM Library・実ROMを使わず、外部Assemblerも起動していない。ROM／ROM由来fixtureを追加していない。
- macOSで独立Compose画像6枚を確認: 日英の通知・診断・原文ログ・ソース見本を600×850／420×850、保存範囲ダイアログを480×550で表示。範囲ダイアログは実際のMemoryDumpRangeFieldsを使用し、C100..C000の逆転エラーを表示。長いファイル名・パス、説明・通知・診断の折返しを確認。縮小時に下部がスクロール領域外になることを記録。
- 本番StudioでのROM経由入力・実RAMへのロード／保存、OSファイル選択、実ウィンドウ縮小／スクロール操作は未実施。Windows／Linuxの実表示も未確認。画像・自動検証と実操作の結果を区別し、ステップ10の確認に保持する。
- Core実装・既存1470U変更・スキン・実設定・ROM Libraryを維持。packageVersionは1.3.1。commit／pushなし。

次はステップ7のDebugger。


## 13. 順次作業ステップ7（2026-10-14）

Debuggerの文言移行を完了。

- デバッガーウィンドウ、CPU変更項目、逆アセンブル／メモリ／命令トレース／ウォッチ／メモリマップの操作・説明・状態・入力エラーを日英リソースへ移行。Breakpoint追加／削除と指定アドレスまで実行のアクセシビリティ説明もアドレス付きで取得する。
- 逆アセンブル保存、Checkpoint保存、AI Debug Contextの範囲・件数・プレビュー・JSON保存、ファイル選択タイトルと成功／失敗通知を移行。Coreのメモリ領域名・kind・IDは変更せず、StudioDebuggerStringsの表示変換で説明語のみを翻訳。
- Breakpoint／Run to／メモリ変更／読み書き停止理由を言語注入に対応。アドレス・バイト・R/W・レジスター名・命令・ROM component IDは原文維持。CheckpointはcheckpointDescriptionの明示英語取得を使用し、従来の英語保存表現を維持する。
- DebugContextErrorの型とサービスの原文診断は維持。Studioの表示だけ既知診断を翻訳し、未知診断には翻訳済み概要と原文を表示。Debug Contextの構造化JSON、schema、MCPプロトコルは変更していない。
- 縮小時に操作行を折り返す。CPU・逆アセンブル・Memory・Traceのデータ行は横スクロールにし、列と数値表現を維持。Debug Contextダイアログの固定最小幅を解除。各パネルへテスト用Stringsを注入可能にし、Debugger全体の再構成や機能追加は行わない。

### 検証

- JDK 21で `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render' :desktopApp:test :core:desktopTest build` 成功。StudioDebuggerStringsTestの3件、全リソースkey／引数／重複検査、既存Studio tests、Core tests、全体buildとgit diff --check成功。
- 全停止理由の従来英語文言（4件超のプレビュー省略、Fault内の説明を含む）を固定期待値で検証。日本語画面の停止理由とHEX／R/W保持を確認し、日英文言取得後に作るCheckpoint JSONをバイト比較して一致。
- 固定時刻・session ID・サイクル数で合成セッションのDebug Context JSONを比較して一致。サービスのInvalidRequest原文が表示変換後も変わらないこと、既知の件数診断・未知診断の原文保持、CPU識別子とPC-1245のCoreメモリ領域値の不変を検証。実ROM・ROM由来fixture・実設定・ROM Library・SDLは使っていない。
- macOSで独立Compose画像8枚を目視確認。日英CPU／Memory／逆アセンブルを900×1100と420×1100、Trace／Watch／メモリマップを900×1000と420×1000で描画。本番と同じパネルへ明示Stringsと合成セッションを注入。ボタン折り返し、領域名の折り返し、データ列保持を確認。狭い画像では横スクロールの右側と縦スクロールの下部は画像範囲外になる。
- 画像は初期／トレース無効状態の確認。BreakpointやWatchの実操作、横・縦スクロール操作、Checkpoint／Debug Contextダイアログの実表示・保存操作、OSファイル選択・実ウィンドウ縮小は未実施。Windows／Linuxの実表示も未確認。ステップ10の確認に保持し、画像・自動テストと区別する。
- 既存1470Uキー修正と関連Core／Studioテストの差分量は開始時と一致。作成中スキン6枚は維持。packageVersionは1.3.1、commit／pushなし。


## 14. 順次作業ステップ8（2026-10-14）

WAV Tool／Character Editorの文言移行を完了。

- WAVツールの入力選択、ファイル由来・再変換の説明、OLD／S1／S2選択、type 12、変換／解析／キャンセル、処理・結果状態、各保存／出力、未保存結果の置換／終了確認、タブ・Rawページ・診断を日英リソースへ移行。S1/S2未実装・pcwav案内と実機互換性未検証という現行の意味を維持し、対応機能は追加していない。
- タブの状態値Content／Raw／Waveformとwhen分岐は維持し、表示だけ翻訳。filename、machine ID、設定記録、原文ソース、バイト・HEX・アドレス、OLD／S1／S2／02／12／62を変更しない。
- WAV概要はStudioToolStringsで表示時に変換。旧summary／reportとレポートのInput／Settingsヘッダーは保存用の原文を維持。既知のコーデック診断はseverity・stage・座標空間・メッセージ・expected／actualの説明を翻訳し、code・数値・生データを保持。未知の説明には「詳細（原文）」を添える。BASIC／DMP入力の型付きエラーを結果に保持し、従来のerror原文とreportを残したままUI表示へ利用する。
- 波形の表示方式・振幅ガイド・OLD差分としきい値、PCM統計、カーソル・選択範囲・Raw対応、移動／拡大縮小の操作説明を移行。時間軸のs／msとLocale.ROOTによる数値書式、信号処理・マッピング・波形描画アルゴリズムは維持。Checkbox／ガイドSlider／波形Canvas／移動矢印／Raw行／診断に日英のアクセシビリティ説明を付与。
- ドットエディターのラベル、5×7書式の説明、入力案内・入力エラー、グリッドの操作説明を移行。入力モデルとCoreのDotByteTextResultは変更せず、エラーの表示変換を使用。クリア／反転／適用、バイト列・DATA／DB出力、書式ID・ビット順は維持。Undo／複数文字／ファイル入出力／機種別UDCは追加していない。
- 長い訳文に対応するため操作行と文字グリッド／入力欄をFlowRowで折り返し、文字入力欄の固定最小幅を解除。WAV ownerはプレビューでnullを渡せる形にし、本番のowner付きファイル選択とfocus復帰を維持。言語・文字モデルを独立描画へ注入可能にした。

### 検証

- JDK 21で `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render build/localization-preview step8' :desktopApp:test :core:desktopTest build` 成功。StudioToolStringsTestの追加4件、日英リソースkey／引数／重複検査、既存Studio tests、Core tests、全体buildとgit diff --check成功。
- 合成BASIC（1.25を含む）とアドレス付きDMPを変換・復号し、日英文言取得の前後でWAV・Program・Rawバイト、report、summary、マッピングを比較して一致。再エンコード結果のWAVとreportも一致。ユーザーパスと波括弧・機種ID・HEXアドレス保持を確認。実ROM／ROM由来fixture／実録音を追加していない。
- checksum診断のcode・座標・expected／actualと原文describeを保持。全severity／stage／座標空間の説明取得、未知診断保持、実WAVヘッダー不正、BASIC／DMP入力エラーの表示とreport不変を検証。
- 文字入力の空欄・個数不足・不正値・5×7範囲外ビットを日英で表示し、失敗時のパターン保持、バイト書式とDATA／DB出力・書式ID不変を確認。
- macOSの独立Compose描画18枚を確認。日英のドットエディター900×700／420×800、WAV初期800×420／420×550、WAV入力800×900／420×1050、実波形部品と概要・診断見本900×1000／420×1300、未保存置換確認の見本480×400。本番の表示部品を使用し、長いパスと説明・ボタンの折り返し、グリッド下への入力欄移動を確認。波形見本の下部がスクロール範囲外になる場合がある。
- 画像生成で波形見本の一部文字・Checkbox枠が欠けたため、プレビューにステップ8／言語別／単画面生成の指定を追加。`--args='--render build/localization-preview step8-ja'`、`step8-en`、`step8-en:wave-narrow`、`step8-en:wave` の別プロセスで再生成。日本語の再生成で欠けがない画像を確認したが、英語縮小画像では単画面生成後も欠けが残った。発生条件は未確定で、文字列自体の検証と実表示確認を区別する。描画画像を全面合格とは扱わず、ステップ10で本番画面と独立描画を確認する。アプリの文言や数値をこの描画現象に合わせて変更していない。
- WAV入力見本は存在を要求しない合成パスを使用し、ファイル読込み／保存ボタンを操作していない。波形は合成DMPのメモリ内変換から描画。未保存確認は同じリソースの代表見本。本番での変換・キャンセル・タブ遷移・Raw連動・zoom／pan・文字ドット編集・OSファイル選択、実スクロール・ウィンドウ縮小・アクセシビリティ操作は未実施。Windows／Linuxの実表示も未確認で、ステップ10へ保持。
- 実設定・ROM Library・Core実装・既存1470U差分・作成中スキン6枚を維持。packageVersionは1.3.1、commit／pushなし。


## 15. 順次作業ステップ9（2026-10-14）

ゲームパッド／MCP関連UIの文言移行を完了。

- MCPウィンドウの見出し、公開範囲、認証・初回設定手順、ポート入力／検証、起動・停止、待受状態、URL／トークン／認証ファイルのラベル、コピー操作と通知、公開ツール・最後のリクエストと成功／失敗表示をリソースへ移行。
- 表示だけのMcpServerPanelを抽出し日英Stringsを注入。サーバー所有・起動／停止・クリップボード操作はMain側に維持。ツール名・HTTP method・timestamp・URL・トークン・パスは原文を引数で保持。コピーするCodex設定のTOML、http_headers_helper、Authorization、enabled_toolsとMCP JSON／schema／認証・アクセス制御処理は変更していない。
- 起動失敗はStudioMcpStringsで翻訳済み概要を表示。既知のPOSIX／ACL／ファイル権限・ホーム／LOCALAPPDATA取得エラーはUI側で翻訳し、未知のOSエラーは原文を添える。DesktopMcpStartResultとcredential storeの原文エラーは維持。
- 共有ControllerSettingsの既存リソース移行を確認し、入力有効Switchの操作説明と図内ボタンの割当・押下状態・利用不可状態のsemanticsを追加。ControllerStringsの追加値は日本語既定とし、Android既存呼出し・保存動作を維持。commonMainへLocale／OS／ResourceBundle依存を追加していない。
- 英語の図内Unavailableが固定幅に収まらなかったため、図内専用unavailableShortを追加してN/Aと表示。通常の説明／アクセシビリティはUnavailable、日本語既定は従来の利用不可。デバイス名・ID・ポケコンのキー刻印は原文のまま。SDLから名前を取得できない場合だけホストから注入した「Gamepad／ゲームパッド」を表示し、SDLバックエンドの既定値は従来のGamepadを維持。
- MCPの操作行をFlowRowで折り返す。ゲームパッド図の形状・配置は維持。Player全体の翻訳、入力機能・永続化の変更、MCP書込み機能やtool追加は行っていない。

### 検証

- JDK 21で `./gradlew --gradle-user-home .gradle-local :desktopApp:localizationPreview --args='--render build/localization-preview step9-en' :desktopApp:test :core:desktopTest build` 成功。追加StudioMcpControllerStringsTestは4件成功。全リソースkey／引数／重複検査、既存Studio・MCP・ゲームパッドtests、Core tests、全体buildとgit diff --check成功。共有UIを使うAndroidのコンパイル／buildも成功。
- 日英のリクエスト説明でmethod／tool名／timestampと元のDesktopMcpRequestSummaryを保持。既知起動エラーと未知原文エラー、URL・トークン・長いパスの原文保持を検証。Codex設定を日英文言取得の前後で比較して一致し、トークンが設定本文へ入らないことも確認。
- 合成DebugContextServiceを用いてMCP tool一覧とschema、成功／InvalidArgumentsのJSON応答を日英文言取得前後で比較して一致。実ROM・実設定・実認証ファイルを使わず、追加テストはサーバー起動やクリップボード操作をしない。既存MCP testsは既存の独立テスト経路で実行。
- ControllerStringsの日本語既定とStudio日本語の説明・ボタン表記・押下状態・図内利用不可が一致。GamepadSettings.preferenceValuesと復元、PocketKeyのキー刻印が日英文言取得後も変わらないことを検証。実Preferencesへ書き込まず、SDL・ゲームパッド機器を追加テストやプレビューから起動していない。
- macOSの独立Compose描画10枚を確認。日英の本番McpServerPanelへ合成server info／request／エラーを注入し、待受状態760×1500／420×2200と停止・権限エラー420×1800を描画。長いtoken／パス、手順・説明とコピー操作の折り返しを確認。tokenは固定の非認証用見本で、実サーバーは起動していない。
- 本番ControllerSettingsへ合成デバイス2台・押下／未押下・割当／未割当・利用不可状態を注入し、600×850／420×1000の日英4枚を確認。英語のUnavailable切れを発見してN/Aへ修正し、再生成画像で収まることを確認。日本語は見える文言と図の配置を維持。
- 本番ウィンドウでのMCP起動／停止・コピー・OS権限エラーの発生操作、ゲームパッド接続／切断・選択／割当・設定保存、Android実機・アクセシビリティ操作、スクロール／縮小操作は未実施。Windows／Linux実表示も未確認。独立描画と自動テストの結果を実操作結果とは区別し、ステップ10へ保持。ステップ8で記録した独立描画の一部文字欠けも未解決として保持。
- Core実装、既存1470U差分、作成中スキン6枚、実設定・ROM Libraryを保護。packageVersionは1.3.1、commit／pushなし。

次はステップ10の翻訳漏れ・レイアウト・互換性の最終確認と文書整理。


## 16. 順次作業ステップ10（2026-10-14）

文言移行・静的確認・自動検証・記録整理を完了。本番GUIの全面確認は未完了。

- 直接表示する英語・日本語、動的状態、エラー変換、固定keyの文言取得を確認。原文維持のキー刻印、CPU、テーマID、著作権、tool名、保存／レポート表現を分類し、翻訳対象と混同しない。固定key781箇所に未定義key・引数数不一致なし。
- 独立JVMの起動境界テストを追加。ja／en／fr／zhの選択と英語fallback、見出し、1.25／C000、Locale不変を検証。JVMプロパティ注入による検証であり、OS設定変更による試験ではない。
- DebugContextFieldPairとAssemblerSelectionOptionsの小さな表示部品を追加。狭いダイアログでは入力を縦配置して本文をスクロールし、RadioButtonのラベル組とWorkspace操作行を折り返す。Main全体の再構成や機能追加は行っていない。
- ユーザーの文言修正を反映: ドットエディター、OSの設定に従う、エラー／警告の該当行、sc61専用の外部連携、画面下部の入力切替、バイナリー形式のROMダンプ。日英で説明を揃えた。
- JDK 21でStudio／Core testsと全体build成功。ローカライズ関連32件、日英863 keyの一致・重複・引数検査に失敗なし。JARのリソース同梱とソースとの一致を確認。保存互換性・共有UIの結果は各ステップと[最終検証記録](STUDIO_LOCALIZATION_VERIFICATION.md)に整理。
- macOSで日英の縮小Debug Contextダイアログ上部／下部480×650と外部Assembler選択・説明420×650の代表画像6枚を確認。値・ラベル・折り返しを確認したが、スクロール後の描画に一部文字欠けが残る。初回フレームだけの描画も試し、アニメーション途中となったため従来の2フレーム生成へ戻した。描画の全面合格とは扱わない。
- GUIツールのJavaアプリ選択がInvalid appとなり、本番実操作は未確認。Windows／Linux実表示、OS言語設定を実変更した起動、ネイティブメニュー・各ダイアログ・実スクロール／縮小・実機入力・アクセシビリティは未確認として明示。
- ROADMAP／TASK_REVIEWを「実装・自動検証済み、実表示確認は残る」という状態へ更新。言語上書き／実行中切替、Player全体・他の機能拡張は今回へ追加していない。
- 既存1470U差分・スキン・実設定・ROM Libraryを維持。packageVersionは1.3.1、commit／pushなし。


## 17. 追加指示：設定の表示言語（2026-10-14）

当初対象外だった設定からの言語上書きを、ユーザーの追加指示により実装。反映はアプリ再起動後とし、実行中切替は追加しない。

- 設定の先頭に表示言語を追加。「OSの設定に従う／日本語／English」から選択し、選択時に自動保存する。変更の説明と保存後の再起動案内、保存失敗の表示を日英で用意。
- StudioLanguagePreferenceはsystem／ja／enの固定値を保存。DesktopUiPreferencesのuiLanguageだけを更新し、既存の速度・エディター・Assembler・画面配置・ゲームパッド等のkeyを変更しない。設定がない場合・未知の値・読取り失敗はOS設定へ戻す。
- Desktopのmainで保存値を読んでからStudioStrings.systemを解決し、このプロセスのUI言語を固定。保存中のアプリでは既存／新規ウィンドウとも現在の言語を使い、次回起動から上書きを反映する。Locale.setDefaultを使わず、Core／共有UIへLocaleやPreferences依存を追加しない。
- Preferencesを注入するStudioLanguagePreferencesと表示だけのLanguageSettingsPanelを追加。保存失敗時は直前の値へ戻し、画面の選択を成功時だけ更新。実Preferencesへ触れず、テストはメモリー内のPreferencesを使う。
- 追加検証: 保存／復元・未知値・既存設定key保持・flush失敗時の復元2件、独立JVMのOS言語×上書き12組とプロセス内言語固定1件、実表示部品のEnglish選択OnClickによるメモリー内保存と日本語保持・再起動案内1件。起動テストのOS注入ja／en／fr／zhも維持。
- JDK 21でStudio／Core testsと全体build成功。ローカライズ関連10 suite／36 tests、日英870 keyの検査に失敗なし。固定key785箇所の引数／key不一致なし。JARの日英リソース一致も確認。
- macOSで実LanguageSettingsPanelの日英600×480／420×500の独立画像4枚を確認。選択したEnglishと日本語／英語の再起動案内・折り返しを確認。実設定の保存と本番再起動、Windows／Linux実操作は未実施で、既存の未確認事項と区別する。
- 実設定・ROM Library・既存1470U差分・スキン6枚を変更せず検証。packageVersionは1.3.1、commit／pushなし。


## 18. ユーザー受入れ・commit対象（2026-10-14）

ユーザーから「一通り確認しました」と報告を受け、文書整理後のcommit／push指示を受領。再起動反映の表示言語設定と用語修正を含め、今回の実装・受入れを完了とする。OS／個別操作の確認内訳は未指定で、各ステップの自動テスト・独立描画の結果とは区別する。

- ROADMAPとTASK_REVIEWはユーザー受入れ済みへ更新。Windows／Linuxの個別結果、OS言語設定変更試験、独立画像の一部描画欠けは後続確認として記録を保持する。
- ローカライズ実装・テスト・リソース・関連文書をcommit対象とする。既存1470Uキー修正・関連テスト・関連文書の差分と作成中スキン6枚を混ぜず、未commitのまま保持する。共有文書ではローカライズ箇所だけをstageする。
- packageVersionは1.3.1。公開済みタグ・履歴、ROM／ROM由来fixtureは変更・追加しない。

過去の節の「commit／pushなし」「未確認」は、そのステップを実施した時点の記録として保持する。現在の受入れ状態は本節と[最終検証記録](STUDIO_LOCALIZATION_VERIFICATION.md)を参照。
