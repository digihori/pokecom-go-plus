# Pokecom GO Studio projects

Pokecom GO Studioは内蔵テキストエディタを持たず、任意の外部エディタで管理するソースを
プロジェクトとして読み込む。現在のformat version 1は既定のBuild機種と複数の入力ソースを記録する。Project自体は実行機種を制限しない。

本書はプロジェクト管理の基本仕様を定義する。実装済みの範囲と今後実装する範囲は
`docs/development/planning/ROADMAP.md`で管理する。

現在はプロジェクト定義の読み込み、検証、ソースファイルの外部変更検知、プロジェクト全体の
Build & LoadとAssemblyソースのBuild連携を実装している。プロジェクト機能は実行方法を決めず、ロード後の
`RUN`、`DEF-S`、`CALL &C000`等はユーザーがポケコン上で操作する。従来の
`Load BASIC`、`Load Machine Code`は単発操作として引き続き利用できる。

## Manifest

プロジェクトのルートにUTF-8の`pgp-project.json`を配置する。
基本仕様として、Studioの`Create Project`はユーザーが選んだ親フォルダの下へプロジェクト名のフォルダを作り、
その中へManifestと`src/`を生成する。作成時はBASIC、Memory Dump、Assemblyをチェックボックスで
組み合わせて初期ソースにできる。すべて未選択なら空のProjectを作成する。
既存のプロジェクトフォルダ、Manifest、初期ソースは上書きしない。
既定名が重複して通し番号付きフォルダを作る場合は、Manifestの表示名にも同じ番号を付ける。

```text
selected-parent/
└── mogura-game/
    ├── pgp-project.json
    ├── src/
    │   ├── main.bas
    │   ├── machine.dmp
    │   └── main.asm
    └── build/
```

プロジェクト表示名とフォルダ名は別に管理する。作成後の追加、削除、改名、サブフォルダへの移動は通常のファイラーや
外部エディタから自由に行える。

```json
{
  "format": "pgp-project",
  "formatVersion": 1,
  "name": "Mogura Game",
  "machineId": "pc-1245",
  "sources": [
    {
      "id": "main",
      "type": "basic",
      "path": "src/main.bas"
    },
    {
      "id": "routine",
      "type": "memory-dump",
      "path": "src/routine.dmp"
    }
  ]
}
```

`sources`は順序を保持するが、ロード順序や依存関係の意味はBuild機能の実装時に定義する。
各`id`はプロジェクト内で一意とし、英字から始まる英数字、`_`、`-`だけを使用する。
`path`はManifestからの相対パスであり、絶対パス、`..`、バックスラッシュは使用できない。
これによりプロジェクトフォルダをOS間で移動できる。

## Source types

| `type` | 用途 | 現在の状態 |
|---|---|---|
| `basic` | Tokenizerへ渡すBASICテキスト | Build & Load対応 |
| `memory-dump` | アドレス付き`.dmp` | Build & Load対応 |
| `raw-binary` | 指定アドレスへ置くRaw Binary | Build & Load対応 |
| `assembly` | 内蔵SC61860 Assemblerへ渡すソース | Build & Load対応 |

Raw Binaryだけは16進ロードアドレスを必須とする。

```json
{
  "id": "characters",
  "type": "raw-binary",
  "path": "src/characters.bin",
  "loadAddress": "0xC000"
}
```

アドレスは`0xC000`、`&C000`、`C000`のいずれでも記述できる。

## Project tree

Studioはプロジェクト内のファイルとManifestを比較し、メイン画面左側へツリー表示する。

```text
Mogura Game
├── Tracked
│   ├── main.bas
│   └── engine.dmp
└── Untracked
    ├── aaa.bas
    └── bbb.asm
```

- `Tracked`: Manifestの`sources`に登録され、BuildおよびEmulator更新の対象になる
- `Untracked`: `src/`以下に存在するが、Manifestへ登録されていない

ここでいうTracked／UntrackedはGitの管理状態ではなく、PGP Manifestへの登録状態を意味する。
ファイルシステムの追加、削除、改名はツリーへ自動反映するが、Manifestは自動変更しない。

`Update Project`を実行すると、存在しなくなったTrackedの登録をManifestから削除し、対応形式の
UntrackedをManifestへ追加する。監視だけではManifestを書き換えないため、外部エディタの一時的な
保存動作で登録が失われることはない。現在の自動追加対象は`.bas`、`.dmp`、`.asm`とし、ロード
アドレスが必要なRaw Binary等の未対応形式はUntrackedへ残す。

存在しない登録ファイルはツリーへ表示せず、`Update Project`前にBuildまたはEmulator更新を行った
場合だけ具体的なエラーを表示する。
一時ファイルや隠しファイルはツリーの対象外とする。ファイル内容の変更は`changed`表示だけを更新し、
EmulatorやManifestへ自動反映しない。

ツリー上のファイルを選択すると、`.bas`、`.dmp`、`.asm`の先頭に連続して記述した`#`行を
Projectペイン下部のSource informationへ表示する。実行開始方法やCALL先など、ソースの利用方法を
ファイル自身に残すためのStudio用ヘッダーであり、Build時には空行として扱うためエラーの行番号は変わらない。
最初の`#`以外の行以降は通常のソースとして扱う。

```text
# PC-1360用のキー入力確認プログラム
# Build & Load後、RUNモードで CALL &C000
# 使用領域: C000H-C0FFH

ORG 0xC000
```

Source informationの文章は表示幅で折り返し、長い場合は縦スクロールする。ファイルツリーは長いパスを
省略せず、縦横にスクロールできる。ツリーとSource informationの境界は上下へドラッグでき、表示高さを
次回起動時に復元する。Raw Binaryはテキストヘッダーの対象外とする。

## Build & Load

現在のStudioは`Build & Load`で全ソースを検証・変換し、すべて成功した場合だけ成果物をEmulatorへ一括ロードする。
メイン画面のProjectペインと`Project`メニューのどちらからでも実行できる。

- BASICをTokenizerで対象機種の中間コードへ変換する
- Assemblyを内蔵SC61860 Assemblerで変換する
- Memory DumpとRaw Binaryを解析する
- 配置範囲の重複とSessionの書込み規則を検証する
- 失敗した場合はEmulatorを変更しない

Assemblyソースを含むプロジェクトでは`Project` → `Open Assembly Workspace`から専用ウィンドウを開ける。
プロジェクト未選択時にもWorkspace自体は開くことができ、そこからプロジェクトを新規作成またはOpenできる。
`Assemble`はメモリ上のMemory、Disassembly、Listing、Symbol Mapプレビューだけを更新し、失敗時は
最後に成功したプレビューを保持する。`Build`は同じ変換結果から`build/program.dmp`、
`build/program.lst`、`build/program.map`を生成する。`Build & Load`も同じビルド処理を経由する。
現時点の状態表示は`Not built`、`Up to date`、`Source changed`、`Runtime modified`、`Build failed`を使用する。

## External editing workflow

1. `Project` → `Open Project…`を選び、`pgp-project.json`を含むプロジェクトフォルダを指定する。
2. BASIC、ダンプ、アセンブリ等を任意の外部エディタで編集する。
3. Studioは内容のハッシュを定期的に比較し、作成、変更、削除を検知する。
4. `Build & Load`は全ソースを先に検証・変換し、成功した場合だけロード処理へ進む。
5. ロード後は、ポケコンへ`RUN`、`DEF-S`、`CALL`等を直接入力して実行する。

ファイルシステムのタイムスタンプ精度に依存しないため、同じサイズの上書き保存も検知する。
変更検知自体は自動だが、編集中の不完全な状態をEmulatorへ自動ロードしない。
Studioは`Source changed`を表示し、ユーザーが`Build & Load`を実行した時だけ最新ソースを反映する。
Assembly Workspaceでは最後に成功したAssembly後に変更された入力へ`*`を表示する。

プロジェクトのBASICをEmulatorへロードした後は、内部的にロード直後のBASICプログラムを基準として追跡する。
PROモード等でEmulator内のBASICプログラムが変更された状態をProjectペインへ表示するUIは今後再配置する。
通常のプログラム実行による変数や表示内容の変更は追跡対象外とする。ROM変更、Reset、別プロジェクトのOpen、
単発の`Load BASIC`ではこの追跡を終了する。

1プロジェクトに複数のBASICプログラムを登録できる。Projectペインで登録済みファイルを選び、
「選択ファイルをビルドしてロード」で、そのファイルだけを変換・ロードする。
他のBASICやAssemblyはこの操作の対象にならない。選択ファイルの成果物はメモリ上でロードし、
全体Buildの成果物ファイルやMCP用の全体Build結果としては保存しない。
状態は「選択ファイルをロード済み」とし、Project全体が最新とは表示しない。
「プロジェクト全体をビルドしてロード」は従来の一括操作で、BASICが複数ある場合は対象選択を案内する。
複数の`.dmp`と
Raw Binaryは登録できるが、配置範囲が重なる場合はEmulatorへロードせずBuildエラーにする。
Projectを開いても現在の機種・ROMは切り替えない。Buildは現在のEmulator機種・RAM設定を使用し、
BASICをその機種の方言へ変換する。Sessionがない場合だけManifestの`machineId`を既定のBuild先として使用する。
Manifestの`machineId`はformat version 1との互換性のため引き続き必須だが、機種IDの不一致だけではLoadを拒否しない。
Assembly／DMPのアドレスは自動変換せず、現在機種のRAM範囲・ミラー重複・BASICとの衝突をLoad時に検証する。
機種固有のLCD／I/O／ROM呼出しの動作互換性を保証するものではない。機種切替後は再Buildする。

マシン語のRaw Binaryに指定するアドレスは実行開始位置ではなくロード位置である。実行は通常どおり
`CALL`、BASICプログラムからの呼び出し、またはDebuggerの実行操作で行う。

## コンソールと最下部メッセージ

最下部には現在の状態と最新結果を短く表示する。その上のコンソールには、ProjectのBuild／Loadの経過・結果と
Studioのエラーを時刻付きで蓄積する。「表示」→「コンソール」または最下部のメッセージをクリックして開く。
「非表示」で閉じてもログは記録され、消去されない。

上端をドラッグして高さを変更できる。手動の表示設定と高さは次回起動時に復元する。
ログの選択コピー・全コピー・ログ消去に対応し、過去ログをスクロールしている間は末尾へ自動移動しない。
保持量は最大1000件・20万文字で、古いログから取り除く。ログ履歴はアプリ終了時に破棄する。
エミュレータのLCD出力の転記や、すべての最下部メッセージの履歴化は行わない。

## マクロ実行

「マクロ」→「マクロを実行…」で`.pgmacro`を選択する。Projectのツリーでマクロを選び、
「選択マクロを実行」ボタンからも起動できる。初版はPC-1260／1261、1401／1402、1450、1460、1470Uに対応する。
他の機種はROMエラー検出未対応のため実行前に停止する。

UTF-8、最大1 MiBのファイルに1行1命令で記述する。空行と`#`で始まるコメントを使える。
命令名・モード名・キー名は大文字小文字を区別しない。文字列は半角二重引用符で囲み、
文字列内の引用符とバックスラッシュはそれぞれ`\"`と`\\`で記述する。

```text
load "basic1.bas"
mode RUN
cmd "RUN"
pause "終了を確認して再開してください"
mode PRO
cmd "NEW"
mode RUN
load "basic2.bas"
```

loadはBASICテキストとDMPを扱い、相対パスはマクロファイルのフォルダを基準とする。
cmdはENTERを自動送信し、送信完了後に1秒待つ。プログラムの終了は判定しない。
追加の時間待ちは`wait 500ms`や`wait 2s`、キー操作は`key ENTER`や`key BREAK`で記述する。
pauseからはコンソールまたは「マクロ」メニューの「マクロを再開」で続行する。

マクロ開始時はコンソールを自動表示する。ROMエラー・Studio操作エラーで後続命令を中断し、
ファイル名と行番号・理由を表示する。「マクロを中断」は入力送信・待機・pause中も利用できる。
非表示やログ消去では実行を止めない。コンソールを閉じた場合はメニューから中断できる。
マクロのためエミュレータは実行状態になり、完了・中断後もそのまま動く。
暴走したマシン語は自動検出できず、マクロ中断でも停止しないため、エミュレータの停止・Resetを使う。
エミュレータの停止・Resetを行うとマクロも中断する。実行済み操作は巻き戻さない。

[サンプルと手動確認手順](../../test-data/macros/README.md) · [初版仕様と制限](../development/planning/STUDIO_MACRO.md)

## Command history

RUNモードでユーザーがポケコンへ直接入力し、ENTERで確定したキー列をStudioがセッション内に保存する。
Studio独自のコマンド入力欄は設けず、履歴を呼び出すと過去のキー列をポケコンへ再入力する。

メイン画面のソフトウェアキーボード下部で、PCキーボードからの入力方式を切り替えられる。選択は保存され、
既定値はLogicalとする。

- `Logical`: OSが生成した文字を機種別キー列へ変換する。例えば`Shift+1`で生成された`!`を入力する
- `Physical`: ホスト側の物理キーをポケコンキーへ対応付ける。`Shift+1`は`SHIFT`と`1`の同時押しになる
- 左ControlおよびF1は、どちらの方式でも`DEF`として押下・解放する
- 入力方式の切り替え時は、押下中のポケコンキーを解放する

- `Alt+↑`: 1つ前のコマンドを呼び出す
- `Alt+↓`: 1つ新しいコマンドへ戻る
- 呼び出した時点ではENTERを送らず、ユーザーが確認・編集してからENTERを押す
- 入力途中で履歴を呼び出した場合は、CLEARを送ってから履歴のキー列を入力する
- PRO／RSVモードの入力、自動入力、履歴再生自体は記録しない
- 同じコマンドを繰り返した場合は重複を1件にまとめる

履歴は文字列ではなく、SHIFTや編集キーを含むポケコンキー列として押下順に保持する。DEFはゲーム操作等に
利用されるため履歴へ含めない。そのため機種別の文字割当や
特殊文字を再解釈する必要がない。現在はROMを開いている間だけのセッション履歴とし、永続化は行わない。
プロジェクトの有無には依存しない。PC側の文字入力とENTERはemulated cycle基準の入力キューへ順番に積み、
速い連続入力でもROMのキースキャンから外れない保持時間とキー間隔を確保する。


## プロジェクト名変更とソース追加

`Project` → `Rename Project…`で開いているプロジェクトの表示名を変更する。
プロジェクトフォルダ名も新規作成時と同じ規則で変更する（空白はハイフンに置換）。
ソースやbuild成果物を含むフォルダ全体を移動し、相対パスは維持する。
同名フォルダがある場合は変更せずエラーを表示する。

`Project` → `Add Existing Files…`（既存ファイルを追加）で既存の`.bas` / `.asm` / `.dmp` / `.pgmacro`ファイルを複数選ぶと、
プロジェクトの`src`へコピーし、Manifestとツリーへ登録する。元ファイルはそのまま残る。
同名ファイルがある場合は`name-2.asm`のように連番を付け、既存ファイルを上書きしない。
未登録の別ファイルは、この操作では登録しない。追加後は必要に応じてBuild & Loadを実行する。
複数追加は1回のManifest更新で確定し、失敗時は今回コピーしたファイルを取り除く。
`.pgmacro`は補助ファイルとしてsrc内に置き、Build対象のManifestには登録しない。
ツリーではUntrackedに表示され、Update ProjectでもBuild対象へ追加されない。
マクロの実行方法は上記を参照。
一括Build／LoadではBASICソースは1つまでとし、複数BASICがある場合は選択ファイルの操作を使う。
`Project` → `New File…`（新規ファイル）では、拡張子付きの名前を指定してsrc内にファイルを作成できる。
BASICは`10 END`、Assemblyは機種の開始アドレスと`RTN`、DMPは空、マクロはコメントを初期内容とする。
編集は従来どおり外部エディタで行う。同名の既存ファイルは上書きしない。

ツリーでファイルを選び、`Project` → `Rename Selected File…`（選択ファイルの名前変更）で改名できる。
同じフォルダ内で拡張子を維持して改名し、登録済みソースはID・種類・ロードアドレスを保ったままManifestのパスも更新する。
Manifestの更新失敗時はファイル名を元に戻す。未登録ファイルも改名できるが、自動登録はしない。
マクロのload文やAssemblyのINCLUDE等、ファイル内容中の参照は自動変更しないため、利用者が更新する。
新規作成・既存追加・名前変更はProjectペインのボタンからも利用できる。

## Project内のゴミ箱

ツリーでファイルを選び、「Project」→「選択ファイルをゴミ箱へ移動…」またはProjectペインの同名ボタンから移動する。
確認後、Project直下の`.trash/日時-一意な番号/元の相対パス`へファイルを移し、登録済みソースはManifestから外す。
同名ファイルを繰り返し移動しても上書きしない。未登録のマクロ等も移動できる。
Manifest更新に失敗した場合はファイルを元へ戻す。エミュレータのメモリ上のプログラムは消去しない。

「Project」→「プロジェクトのゴミ箱を開く」でファイル管理アプリを開く。隠しフォルダだが、この操作で直接開ける。
各移動先の`entry.json`には元の相対パス、移動日時、登録済みの場合はソースID・種類・ロードアドレス等を保存する。
ゴミ箱はツリー走査とUpdate Projectの対象外で、自動的にBuildへ戻らない。

専用の復元・ゴミ箱を空にする操作は未実装。必要な場合は元のパスへファイルを戻し、
BASIC／Assembly／DMPはUpdate Projectで再登録する。旧ソースIDやRaw Binaryの設定を復元したい場合は、
`entry.json`の情報を参照してManifestも復元する。既存ファイルを上書きしないよう別名で退避してから行う。
マクロやAssemblyの参照文字列は自動更新しないため、移動後も使うソースの参照は利用者が修正する。
