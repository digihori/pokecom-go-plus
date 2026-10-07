# Pokecom GO Studio projects

Pokecom GO Studioは内蔵テキストエディタを持たず、任意の外部エディタで管理するソースを
プロジェクトとして読み込む。現在のformat version 1は対象機種と複数の入力ソースを結び付ける。

本書はプロジェクト管理の基本仕様を定義する。実装済みの範囲と今後実装する範囲は
`docs/ROADMAP.md`で管理する。

現在はプロジェクト定義の読み込み、検証、ソースファイルの外部変更検知、プロジェクト全体の
Build & LoadとAssemblyソースのBuild連携を実装している。プロジェクト機能は実行方法を決めず、ロード後の
`RUN`、`DEF-S`、`CALL &C000`等はユーザーがポケコン上で操作する。従来の
`Load BASIC`、`Load Machine Code`、`Type BASIC`は単発操作として引き続き利用できる。

## Manifest

プロジェクトのルートにUTF-8の`pgp-project.json`を配置する。
基本仕様として、Studioの`Create Project`はユーザーが選んだ親フォルダの下へプロジェクト名のフォルダを作り、
その中へManifestと`src/`を生成する。作成時はBASIC、Memory Dump、Assemblyをチェックボックスで
組み合わせて初期ソースにできる。既存のプロジェクトフォルダ、Manifest、初期ソースは上書きしない。
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

Studioはプロジェクト内のファイルとManifestを比較し、専用のProject Filesウィンドウへツリー表示する。
プロジェクトを開くと同ウィンドウが前面に開き、閉じた後は`Project` → `Show Project Files`で再表示できる。

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

## Build & Load

現在のStudioは`Build & Load`で全ソースを検証・変換し、すべて成功した場合だけ成果物をEmulatorへ一括ロードする。
Project Filesウィンドウと`Project`メニューのどちらからでも実行できる。

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
PROモード等でEmulator内のBASICプログラムが変更された状態をProject Filesへ表示するUIは今後再配置する。
通常のプログラム実行による変数や表示内容の変更は追跡対象外とする。ROM変更、Reset、別プロジェクトのOpen、
単発の`Load BASIC`ではこの追跡を終了する。

現在は1プロジェクトにつきBASICプログラムを1ファイルまで登録できる。複数の`.dmp`と
Raw Binaryは登録できるが、配置範囲が重なる場合はEmulatorへロードせずBuildエラーにする。
プロジェクトの対象機種と現在ロード中のROMが異なる場合もロードしない。

マシン語のRaw Binaryに指定するアドレスは実行開始位置ではなくロード位置である。実行は通常どおり
`CALL`、BASICプログラムからの呼び出し、または将来のDebugger操作で行う。

## Command history

RUNモードでユーザーがポケコンへ直接入力し、ENTERで確定したキー列をStudioがセッション内に保存する。
Studio独自のコマンド入力欄は設けず、履歴を呼び出すと過去のキー列をポケコンへ再入力する。

- `Alt+↑`: 1つ前のコマンドを呼び出す
- `Alt+↓`: 1つ新しいコマンドへ戻る
- 呼び出した時点ではENTERを送らず、ユーザーが確認・編集してからENTERを押す
- 入力途中で履歴を呼び出した場合は、CLEARを送ってから履歴のキー列を入力する
- PRO／RSVモードの入力、`Type BASIC`等の自動入力、履歴再生自体は記録しない
- 同じコマンドを繰り返した場合は重複を1件にまとめる

履歴は文字列ではなく、SHIFTや編集キーを含むポケコンキー列として保持する。そのため機種別の文字割当や
特殊文字を再解釈する必要がない。現在はROMを開いている間だけのセッション履歴とし、永続化は行わない。
