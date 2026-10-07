# Pokecom GO Studio はじめに

このガイドでは、Pokecom GO Studio Technical PreviewでROMを開き、PC-1245または
PC-1250／1251／1255を起動してBASICプログラムを実行するまでを説明する。

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

## 3. 機種を選ぶ

画面右上の機種プルダウンには全対応機種が表示される。ROM登録済みの機種だけを選択でき、選択すると
保存済みROMへ切り替えて自動RUNする。未登録機種は`ROM not registered`として無効表示される。

PC-1250／1251／1255は同じPC-1251系ROMを共有する。ROM管理画面で一度登録すれば3機種すべてを
選択でき、機種切替時は同じROMから各モデルのRAM構成を持つSessionを生成する。

PC-1250／1251／1255では次のRAMモードも選べる。

- `Hardware RAM`: 選択した実機と同じRAM範囲
- `Expanded RAM`: PC-1255相当の拡張RAM範囲

エミュレータならではの互換性を優先する現在の既定値は`Expanded RAM`である。

## 4. ROMを開く

### 4.1 Pokecom GO形式のROMイメージ

`File`メニューの`Register ROM…`または`Manage ROMs…`を開き、対象機種の`Register…`を選ぶ。
次の従来形式は検証済み`.pgrom`へ変換され、アプリ専用領域へ保存される。

- 32KiBのアドレス空間イメージ
- Pokecom GO互換の64KiBアドレス空間イメージ

PC-1245の例では、`0x0000..0x1fff`から内部ROM 8KiB、`0x4000..0x7fff`から
外部ROM 16KiBを取り出す。64KiB形式の後半にあるダミー領域はROMとして使用しない。

`.bin`には機種IDがないため、ROM管理画面の正しい機種または機種ファミリーから登録する。

### 4.2 `.pgrom`パッケージ

`Open ROM`から`.pgrom`を選択する。パッケージ内の機種ID、各ROMのサイズ、SHA-256を
検証し、すべて正しい場合だけ起動する。

`.pgrom`の中身を確認するだけなら、一般的なZIPツールで展開できる。内容を変更して
再圧縮するとmanifestのサイズやSHA-256と一致しなくなるため、編集には使用しない。

### 4.3 8KiB＋16KiBのROMから`.pgrom`を作る

PC-1245／1251ファミリーでは、ROM吸い出し時に得られる次の2ファイルから作成できる。

1. 内部ROM: 正確に8KiB（8192 byte）
2. 外部ROM: 正確に16KiB（16384 byte）

操作手順：

1. 画面上部で対象機種を選ぶ。
2. `Create ROM Set`を押す。
3. 説明を確認して`Continue`を押す。
4. 最初のファイル選択で内部ROMを選ぶ。
5. 次のファイル選択で外部ROMを選ぶ。
6. 最後のダイアログで出力先と`.pgrom`ファイル名を指定する。
7. 作成後、`Open ROM`から生成したファイルを開く。

ファイル名は判定に使用しない。PGPがサイズを検証し、manifestとSHA-256を自動生成する。
バンクROM機種向けの一括選択UIはまだ実装途中である。

## 5. エミュレータを動かす

ROMを開いた直後は停止状態になる。`Run`を押すとCPU実行を開始する。

- `Run`: CPU実行を開始
- `Pause`: CPU実行を一時停止
- `Reset`: 機種をリセットして停止
- `Step`: 停止中にCPU命令を1つ実行
- `RUN mode`: BASICの実行モード
- `PRO mode`: BASICの編集モード
- `RSV mode`: 対応機種の予約語登録モード

画面キーボードは実機の配列とSHIFT操作を再現する。PCの物理キーボードも使用できる。
ポケコンのSHIFTは、原則としてSHIFTキーを押して状態を点灯させ、その後に対象キーを押す。

## 6. BASICソースを読み込む

長い行や特殊文字を含む通常の開発用途では、Tokenizer方式の`Load BASIC`を使用する。

1. ROMを起動する。
2. `Load BASIC`を押す。
3. UTF-8の`.bas`ファイルを選ぶ。
4. `RUN mode`へ切り替える。
5. 必要なら`Run`でCPU実行を開始する。
6. 実機と同様に`RUN`コマンドを入力する。

`Load BASIC`はテキストを機種用の中間コードへ変換し、プログラム領域へ直接配置する。
長い1行を実機ROMの編集バッファへ入力する制限を受けない。

`Type BASIC`は、テキストをキー操作へ変換して実機ROMの編集処理に入力する互換方式である。
実機に近く手軽だが、1行入力長の上限があるため、長い行には使用しない。

特殊文字のエスケープ表記やテキスト仕様は[BASICテキスト形式](BASIC_TEXT_FORMAT.md)を参照する。

## 7. よくあるエラー

### ROMサイズが正しくない

内部ROMは8192 byte、外部ROMは16384 byteである必要がある。ファイルのヘッダー、
チェックサム、吸い出しツール独自の付加データが含まれていないか確認する。

### `.pgrom`の検証に失敗する

ZIP内の`manifest.json`、componentサイズ、SHA-256、機種IDのいずれかが一致していない。
手作業で修正せず、元のROMファイルから`Create ROM Set`で作り直す。

### 従来形式ROMを開いても動かない

`.bin`を開く前に選んだ機種が正しいか確認する。PC-1245用ROMをPC-1251として開くことはできない。

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
