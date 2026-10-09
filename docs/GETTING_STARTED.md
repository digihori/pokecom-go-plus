# Pokecom GO Studio はじめに

このガイドでは、Pokecom GO Studio Technical PreviewのROM LibraryへROMを登録し、対応機種を起動して
BASICプログラムを実行するまでを説明する。

## 1. 用意するもの

- Pokecom GO Studioの配布物、またはJDK 21を用意したソースツリー
- 所有する対応機種から正当に取得したROMイメージ
- 必要に応じて、UTF-8で保存したBASICソース（`.bas`）

ROMイメージはPGPのリポジトリおよび配布物には含まれない。ROMをGitHub Issueなどへ
添付しないこと。

現在の対応機種は次のとおり。

- PC-1245
- PC-1250
- PC-1251
- PC-1255
- PC-1350
- PC-1360

PC-1250／1251／1255は同じROM系統を使い、主な違いであるRAM容量は機種選択と
`Hardware RAM`／`Expanded RAM`で切り替える。

## 2. アプリを起動する

配布物を使う場合は、展開したアプリまたはインストールしたPokecom GO Studioを起動する。

- macOS: `.app`またはDMG内のアプリ
- Windows: ZIP内の`PokecomGOStudio.exe`またはMSIでインストールしたアプリ
- Linux x86_64: tar.gz内の`bin/PokecomGOStudio`またはDEBでインストールしたアプリ

ソースツリーから起動する場合は次を実行する。

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

### 4.3 8KiB＋16KiBのROMから`.pgrom`を作る

PC-1245／1251ファミリーでは、ROM吸い出し時に得られる次の2ファイルから作成できる。

1. 内部ROM: 正確に8KiB（8192 byte）
2. 外部ROM: 正確に16KiB（16384 byte）

操作手順：

1. `File` → `Create ROM Set…`を選ぶ。
2. ダイアログに表示された対象機種を確認する。
3. 説明を確認して`Continue`を押す。
4. 最初のファイル選択で内部ROMを選ぶ。
5. 次のファイル選択で外部ROMを選ぶ。
6. 最後のダイアログで出力先と`.pgrom`ファイル名を指定する。
7. 作成後、`File` → `Manage ROMs…`から生成したファイルを登録する。

ファイル名は判定に使用しない。PGPがサイズを検証し、manifestとSHA-256を自動生成する。
バンクROM機種向けの一括選択UIはまだ実装途中である。

## 5. エミュレータを動かす

ROMの登録、機種切替、起動時復元に成功すると自動的にRUNする。メイン画面の`Pause`で停止できる。

- `Run`: CPU実行を開始
- `Pause`: CPU実行を一時停止
- `Reset`: 機種をリセットして停止
- `Step`: 停止中にCPU命令を1つ実行
- `RUN`: BASICの実行モード（PC-1245／1251系のみ表示）
- `PRO`: BASICの編集モード（PC-1245／1251系のみ表示）
- `RSV`: 予約語登録モード（PC-1251系のみ表示）

画面キーボードは実機の配列とSHIFT操作を再現する。PCの物理キーボードも使用できる。
ポケコンのSHIFTは、原則としてSHIFTキーを押して状態を点灯させ、その後に対象キーを押す。

## 6. BASICソースを読み込む

長い行や特殊文字を含む通常の開発用途では、Tokenizer方式の`Load BASIC`を使用する。

1. ROMを起動する。
2. `Program` → `Load BASIC…`を選ぶ。
3. UTF-8の`.bas`ファイルを選ぶ。
4. 対応機種ではメイン画面の`RUN`へ切り替える。
5. 停止している場合は`Run`でCPU実行を開始する。
6. 実機と同様に`RUN`コマンドを入力する。

`Load BASIC`はテキストを機種用の中間コードへ変換し、プログラム領域へ直接配置する。
長い1行を実機ROMの編集バッファへ入力する制限を受けない。

`Program` → `Type BASIC through ROM…`は、テキストをキー操作へ変換して実機ROMの編集処理に入力する互換方式である。
実機に近く手軽だが、1行入力長の上限があるため、長い行には使用しない。

特殊文字のエスケープ表記やテキスト仕様は[BASICテキスト形式](BASIC_TEXT_FORMAT.md)を参照する。

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

サウンドは初期実装である。WindowsではBASICをBREAKした後もクリックノイズが続く場合がある。
アプリを終了すると音声出力ラインも終了する。音声ラインの停止条件は今後改善する。

## 8. ROMの保管

ROM、`.pgrom`、ROMから生成した検証データは公開リポジトリへコミットしない。
ソースツリーで管理する場合はGit対象外の`local-data/`を使用できる。

形式の詳細は[PGP ROM Package Format](ROM_PACKAGE.md)を参照する。
