# Pokecom GO Studio はじめに

このガイドでは、Pokecom GO StudioのROM LibraryへROMを登録し、対応機種を起動して
BASICプログラムを実行するまでを説明する。

## 最初に：旧Pokecom GOから移行するマシン語ファイル

**旧Pokecom GOのマシン語保存ファイルと、PGPの標準形式は異なります。**
旧版は指定したメモリ範囲を生のバイト列として保存しており、ファイル内に開始アドレスを持ちません。
PGPでは、開始アドレスとデータを一緒に保存するDMPを標準とし、用途に応じてIntel HEXも使います。
拡張子を変更するだけでは変換できません。BASICソース（`.bas`）やROMの登録とは別の話です。

| 形式 | アドレス情報 | 主な用途 |
| --- | --- | --- |
| 旧版の生バイナリー（例：`.bin`） | なし。元のロード開始アドレスを別途控える | 旧ファイルの移行入力 |
| PGP Memory Dump（`.dmp`） | あり。UTF-8のアドレス付き16進テキスト | Studioのマシン語ロード／保存・Project・Assembler、およびAndroid Playerへのマシン語ロード |
| Intel HEX（`.hex`／`.ihx`） | あり。レコードごとにチェックサムを持つテキスト | Studio WAV Toolや外部ツールとの交換 |

拡張子`.hex`が必ずIntel HEX形式を意味するわけではありません。
例えば`6100 34 ; PUSH`のような「アドレス＋バイト列」のファイルはDMP互換で、
Studioでは拡張子が`.hex`でもマシン語ロードで読み込めます。この場合、中身の形式変換は不要です。
Intel HEXは`:049000000001020366`のようなコロンで始まるレコードを持つ別の形式です。
以下でいうIntel HEXの対応範囲は、このレコード形式についての説明です。
Android Playerのファイル選択は`.bas`／`.dmp`が対象なので、DMP互換の`.hex`はコピーを`.dmp`名にして利用します。

**旧版の生バイナリーをStudio／Android Playerへ読み込む場合は、まずDMPに変換してください。**
Intel HEXは現在、通常のProject sourceやPlayerへの直接ロードには使えません。
Android Playerの保存はBASICのみで、DMP保存には対応していません。

旧ファイルの移行には、単独で使える[バイナリー／DMP／Intel HEX変換ツール](../../tools/memory/README.md)を用意しています。
Python 3.10以降が必要で、追加パッケージやStudioの起動は不要です。
例えば元の開始アドレスが`0x9000`だったファイルは、リポジトリ直下で次のように変換します。

```sh
python3 tools/memory/pgp_memory.py bin-to-dmp old.bin --start 0x9000 -o program.dmp
python3 tools/memory/pgp_memory.py bin-to-hex old.bin --start 0x9000 -o program.hex
```

`0x9000`は例です。元のプログラムで指定していた開始アドレスに置き換えてください。
分からない場合は推測せず、元の配布資料や保存時の記録で確認します。
CLIの`--start 9000`は10進数なので、16進数では必ず`0x`を付けます。
変換後は[形式の仕様](../reference/MACHINE_CODE_FORMATS.md)と
[Playerの入出力](PLAYER_PROGRAM_IO.md)に沿って利用してください。
アドレス情報があることと、対象機種のRAMへ安全にロードできることは別です。
BASIC領域等を上書きしないよう、対象機種とロード先を確認します。

## 1. 用意するもの

- Pokecom GO Studioの配布物、またはJDK 21を用意したソースツリー
- 所有する対応機種から正当に取得したROMイメージ
- 必要に応じて、UTF-8で保存したBASICソース（`.bas`）

ROMイメージはPGPのリポジトリおよび配布物には含まれない。ROMをGitHub Issueなどへ
添付しないこと。

現在のソースと0.7 alpha 1が対応する機種は次の14モデルです。
配布版ごとの制限は、その版のリリースノートを確認してください。

- PC-1245
- PC-1250／1251／1255
- PC-1260／1261
- PC-1350
- PC-1360／1360K
- PC-1401／1402
- PC-1450
- PC-1460
- PC-1470U

PC-1250／1251／1255は同じROM系統を使い、主な違いであるRAM容量は機種選択と
`Hardware RAM`／`Expanded RAM`で切り替える。

## 2. アプリを起動する

配布物を使う場合は、展開したアプリまたはインストールしたPokecom GO Studioを起動する。

- macOS: `.app`またはDMG内のアプリ
- Windows: ZIP内の`PokecomGOStudio.exe`またはMSIでインストールしたアプリ
- Linux x86_64: tar.gz内の`bin/PokecomGOStudio`またはDEBでインストールしたアプリ

ソースツリーから起動する場合は次を実行する。

JDK設定、テスト、OS別パッケージ生成を含む詳しい手順は[ソースからのビルド](../reference/BUILDING.md)を参照する。

```bash
./gradlew :desktopApp:run
```

Windowsでは次を使用する。

```powershell
.\gradlew.bat :desktopApp:run
```

登録したROMはアプリ専用領域へコピーされる。次回起動時は、前回使用した機種と保存済みROMを
自動的に復元するため、元ファイルを再選択する必要はない。

### 2.1 画面構成

メインウィンドウはLCD、ソフトウェアキーボード、実行操作を表示する。ファイル操作と開発機能は画面上部の
メニューバーから開く。

- `File`: ROMの登録・管理、ROM set作成
- `Project`: Projectの新規作成、Open、Assembly Workspace表示、Build & Load
- `Program`: BASIC／マシン語の単発入出力、Quick Assemble
- `Emulator`: Run／Pause／Step／ResetとRAM profile
- `Debug`: Debugger、Debug Checkpoint

プロジェクトを開くとメイン画面左側へファイルツリーとBuild操作を表示する。CPU、Memory、
Disassembly、Traceは`Debug` → `Open Debugger`で開く独立Debuggerウィンドウへ表示する。

### 2.2 アセンブリソースを確認・ビルドする

プロジェクトへ`.asm`を追加し、`Project` → `Open Assembly Workspace`を開く。
Assembleはスクロール付きの結果欄を更新する。BuildはDMP・Listing・Mapを`build/`へ保存し、
Build & Loadは保存後にエミュレータへロードする。単一の`src/pacman.asm`なら出力は
`build/pacman.dmp`、`build/pacman.lst`、`build/pacman.map`となる。
ボタン下のEmulator表示でロード状態を確認する。ロード後のプログラム実行はポケコン側で開始する。
Show memory layoutで配置可能な領域を確認できる。

対応する式・疑似命令、機種別配置と検証範囲は[SC61860アセンブラガイド](SC61860_ASSEMBLY.md)を参照。
単発変換のQuick AssembleはDMP保存のみで、結果プレビューの改善は今後の課題。

## 3. 機種を選ぶ

画面右上の機種プルダウンには全対応機種が表示される。ROM登録済みの機種だけを選択でき、選択すると
保存済みROMへ切り替えて自動RUNする。未登録機種は`ROM not registered`として無効表示される。

PC-1250／1251／1255は同じPC-1251系ROMを共有する。ROM管理画面で一度登録すれば3機種すべてを
選択でき、機種切替時は同じROMから各モデルのRAM構成を持つSessionを生成する。

PC-1250／1251／1255では次のRAMモードも選べる。

- `Hardware RAM`: 選択した実機と同じRAM範囲
- `Expanded RAM`: PC-1255相当の拡張RAM範囲

エミュレータならではの互換性を優先する現在の既定値は`Expanded RAM`である。

## 4. ROMを登録する

### 4.1 Pokecom GO形式のROMイメージ

`File`メニューの`Register ROM…`または`Manage ROMs…`を開き、対象機種の`Register…`を選ぶ。
次の従来形式は検証済み`.pgrom`へ変換され、アプリ専用領域へ保存される。

- 32KiBのアドレス空間イメージ
- Pokecom GO互換の64KiBアドレス空間イメージ

PC-1245の例では、`0x0000..0x1fff`から内部ROM 8KiB、`0x4000..0x7fff`から
外部ROM 16KiBを取り出す。64KiB形式の後半にあるダミー領域はROMとして使用しない。

`.bin`には機種IDがないため、ROM管理画面の正しい機種または機種ファミリーから登録する。

### 4.2 `.pgrom`パッケージ

ROM Libraryで対象機種または機種ファミリーの`Register…`を押し、`.pgrom`を選択する。パッケージ内の
機種ID、各ROMのサイズ、SHA-256を検証し、有効なSessionを生成できる場合だけ保存して起動する。

`.pgrom`の中身を確認するだけなら、一般的なZIPツールで展開できる。内容を変更して
再圧縮するとmanifestのサイズやSHA-256と一致しなくなるため、編集には使用しない。

### 4.3 ROMダンプから`.pgrom`を作る

`File` → `Create ROM Set…`で専用ウィンドウを開く。必要なROMサイズは対象機種によって異なる。

| 対象機種 | 内部ROM | 外部ROM |
|---|---:|---:|
| PC-1245／1250／1251／1255 | 8 KiB（8192 byte） | 16 KiB（16384 byte） |
| PC-1260／1261／1350／1401／1402／1450 | 8 KiB（8192 byte） | 32 KiB（32768 byte） |
| PC-1360／1360K／1470U | 8 KiB（8192 byte） | 128 KiB（131072 byte、8バンク連結） |
| PC-1460 | 8 KiB（8192 byte） | 64 KiB（65536 byte、4バンク連結） |

操作手順：

1. `Machine`で対象機種を選ぶ。初期値はメイン画面の選択機種。
2. `Internal ROM`の欄をクリックし、ファイル選択ダイアログで内部ROMを指定する。
3. `External ROM`の欄をクリックし、外部ROMを指定する。
4. `Destination folder and file name (.pgrom)`の欄をクリックし、保存ダイアログで
   作成先のフォルダとファイル名を指定する。`.pgrom`拡張子は省略すると補完される。
5. 選択内容を確認し、`Create`を押す。保存先に既存ファイルがある場合は
   `Replace`で上書きを確認する。`Cancel`で取り消せる。
6. 成功すると保存先と`Register in ROM Library`ボタンが表示される。
   このボタンで登録すると対象機種へ切り替わり、自動的にRUNする。
   作成だけで終える場合は`Close`で閉じる。後から`Manage ROMs…`で登録してもよい。

内部ROM・外部ROMは選択時にサイズを確認する。必要な項目が揃い、ROMのサイズが一致すると
`Create`が有効になる。サイズ不一致は該当欄に表示される。機種を変更すると選択内容はクリアされる。
作成に失敗しても選択内容は保持されるため、修正して再実行できる。

PC-1360／1360K／1470Uの外部ROM欄では、bank 0からbank 7までを番号順に連結した128 KiBのファイルを選ぶ。
PC-1460はbank 0からbank 3までを連結した64 KiBを選ぶ。
個別の16 KiBバンクファイルを選択・連結するUIはまだない。
PC-1360Kでは追加の漢字ROM欄にselector 0〜3の各64 KiBファイルを指定する。
かな漢字変換には4つすべてが必要。詳細は[PC-1360K](../reference/machines/PC-1360K.md)を参照。

ファイル名はROMの判定に使用しない。PGPがサイズを検証し、manifestとSHA-256を自動生成する。
元のROMファイルは変更せず、作成先として入力ROM自身を指定することもできない。
ROMと生成した`.pgrom`は`local-data/`などGit管理外の場所に保存する。

## 5. エミュレータを動かす

ROMの登録、機種切替、起動時復元に成功すると自動的にRUNする。メイン画面の`Pause`で停止できる。

- `Run`: CPU実行を開始
- `Pause`: CPU実行を一時停止
- `Reset`: 機種をリセットして停止
- `Step`: 停止中にCPU命令を1つ実行
- `RUN`: BASICの実行モード（PC-1245／1251系／1261系のみ表示）
- `PRO`: BASICの編集モード（PC-1245／1251系／1261系のみ表示）
- `RSV`: 予約語登録モード（PC-1251系／1261系のみ表示）

画面キーボードは実機の配列とSHIFT操作を再現する。PCの物理キーボードも使用できる。
ポケコンのSHIFTは、原則としてSHIFTキーを押して状態を点灯させ、その後に対象キーを押す。

### ゲームコントローラー

OSでUSB／Bluetoothコントローラーを接続してから、Studioの`Settings…`内の`Controller`を開く。
Android Playerでは3点メニューの`コントローラー設定`を開く。ROM登録前も画面右上から開ける。

1. コントローラー入力を有効にする。1台なら自動選択され、複数接続時は使用する1台を選ぶ。
2. 図の十字キー・ABXY・L1/R1をクリック／タップし、ポケコンのキーを選ぶ。
3. 例えば「左」に`4`を選ぶ。変更は自動保存される。設定は1つだけ保存し、以前の割当を上書きする。

ボタンを押している間は割当キーを保持し、離すと解放する。実際の入力はCPU実行中に有効になる。
設定中は押したボタンが図で点灯し、ポケコンには入力されない。
ABXYの印字が異なる機器は図の上下左右の位置で選ぶ。アナログスティック・トリガーは使用しない。
機種を切り替えても同じ割当を使い、その機種にないキーは入力せず設定画面に`利用不可`と表示する。
ゲームパッド操作はStudioのコマンド履歴には記録しない。キーボードの履歴操作は従来どおり使用できる。
StudioとPlayerの設定はそれぞれのアプリ内に保存される。

## 6. BASICソースを読み込む

BASICソース（UTF-8の`.bas`ファイル）は、`Load BASIC`で読み込む。

1. ROMを起動する。
2. `Program` → `Load BASIC…`を選ぶ。
3. UTF-8の`.bas`ファイルを選ぶ。
4. 対応機種ではメイン画面の`RUN`へ切り替える。
5. 停止している場合は`Run`でCPU実行を開始する。
6. 実機と同様に`RUN`コマンドを入力する。

`Load BASIC`はテキストを機種用の中間コードへ変換し、プログラム領域へ直接配置する。

特殊文字のエスケープ表記やテキスト仕様は[BASICテキスト形式](../reference/BASIC_TEXT_FORMAT.md)を参照する。

### Studioの外部編集とロード元表示

`Settings…` → `External editor`で、ツリーの`Edit`ボタンと診断行から開くエディタを設定する。
`Use OS text editor`でOS標準のテキストエディタを使うか、コマンドを入力して
`Apply editor setting`を押す。例えば`code --goto "{file}:{line}"`でVS Codeを指定できる。
空白を含む実行ファイルのパスは引用符で囲む。`{file}`を省略すると、ソースのパスを末尾へ追加する。
コマンドはシェル展開せず直接実行する。保存した設定は従来の`PGP_EDITOR`環境変数より優先される。
macOSでは`code`がアプリのPATHにない場合も、`/Applications`または`~/Applications`のVS Codeを
自動検出するため、ターミナル用のcodeコマンドをインストールする必要はない。

ツリーでBASIC・ASMなどのテキストソースを選び、`Edit`で開く。バイナリでは無効になる。
外部で保存した変更は監視により検出され、選択中の`Source information`も約500ms間隔で更新される。

ソフトキー下の左寄せの表示は、最後に正常にロードしたプログラムの出所を示す。
`Build & Load`では`Project: NAME`、単体のロードでは`Standalone: FILE（プロジェクト外）`となる。
機械語ロードは追加ロードとして表示する。
単体ロードではプロジェクトBASICの変更追跡を解除し、`Build & Load`で復帰する。
キャンセルや失敗では表示を切り替えない。初回ロード前は非表示で、ROMロードやResetで表示を解除する。

`Project` → `Close Project`はツリーとファイル監視を閉じる。エミュレーターとロード済みプログラムは
保持する。閉じたプロジェクトや現在とは別のプロジェクトからロードしたプログラムには
`現在のプロジェクト外`と表示する。ファイルの削除やプロジェクトへの書き戻しは行わない。

## 7. よくあるエラー

### ROMサイズが正しくない

内部ROMは8192 byte、外部ROMは16384 byteである必要がある。ファイルのヘッダー、
チェックサム、吸い出しツール独自の付加データが含まれていないか確認する。

### `.pgrom`の検証に失敗する

ZIP内の`manifest.json`、componentサイズ、SHA-256、機種IDのいずれかが一致していない。
手作業で修正せず、元のROMファイルから`Create ROM Set`で作り直す。

### 従来形式ROMを登録しても動かない

ROM Libraryで正しい機種または機種ファミリーの`Register…`を押したか確認する。PC-1245用ROMを
PC-1251系として登録することはできない。

### `Load BASIC`が失敗する

エラーメッセージに示された行番号や文字を確認する。UTF-8で保存されているか、対象機種で
未対応の予約語・特殊文字を使っていないか、プログラム領域の容量を超えていないか確認する。

### Windowsでボタン表示が壊れる

Apple Silicon MacのVMware上にあるWindows ARM64でx64版をエミュレーション実行した環境では、
Direct3D／OpenGL描画のhover表示が壊れる現象を確認している。Windows配布版は互換性を優先して
Software描画を既定としている。ネイティブWindows x64で再確認するまでの暫定措置である。
描画方式を比較する場合だけ`PGP_RENDER_API=DIRECT3D`または`OPENGL`を指定する。

### 音に途切れやクリックノイズがある

旧版で確認したWindowsの継続的なクリックノイズは停止処理を修正し、改善を確認している。
再現する場合は、配布版・OS・機種・再現手順を添えて報告する。実機との波形差は残る可能性がある。

## 8. ROMの保管

ROM、`.pgrom`、ROMから生成した検証データは公開リポジトリへコミットしない。
ソースツリーで管理する場合はGit対象外の`local-data/`を使用できる。

形式の詳細は[PGP ROM Package Format](../reference/ROM_PACKAGE.md)を参照する。
