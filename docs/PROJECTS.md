# Pokecom GO Studio projects

Pokecom GO Studioは内蔵テキストエディタを持たず、任意の外部エディタで管理するソースを
プロジェクトとして読み込む。プロジェクトは対象機種と複数の入力ソースを結び付ける。

本書はプロジェクト管理の基本仕様を定義する。実装済みの範囲と今後実装する範囲は
`docs/ROADMAP.md`で管理する。

現在はプロジェクト定義の読み込み、検証、ソースファイルの外部変更検知、プロジェクト全体の
Build & Loadを実装している。プロジェクト機能は実行方法を決めず、ロード後の`RUN`、`DEF-S`、
`CALL &C000`等はユーザーがポケコン上で操作する。Assemblerは今後接続する。従来の
`Load BASIC`、`Load Machine Code`、`Type BASIC`は単発操作として引き続き利用できる。

## Manifest

プロジェクトのルートにUTF-8の`pgp-project.json`を配置する。
基本仕様として、Studioの`Create Project`はユーザーが選んだ親フォルダの下へプロジェクト名のフォルダを作り、
その中へManifestと`src/`を生成する。既存のプロジェクトフォルダ、Manifest、初期ソースは上書きしない。

```text
selected-parent/
└── mogura-game/
    ├── pgp-project.json
    ├── src/
    │   ├── main.bas
    │   └── main.dmp
    └── build/
```

プロジェクト表示名とフォルダ名は別に管理する。初期ソース名はBASIC、Memory Dumpともに`main`を
使用し、拡張子で区別する。作成後の追加、削除、改名、サブフォルダへの移動は通常のファイラーや
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
| `assembly` | 内蔵SC61860 Assemblerへ渡すソース | 予約済み |

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

Studioはプロジェクト内のファイルとManifestを比較し、常時参照できるツリーとして表示する。

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

## Build and Emulator update

ソースから成果物を作る操作と、成果物をEmulatorへ反映する操作は分離する。

- `Build`: Tokenize、Assembly、形式検証、配置重複検査を行い、成果物を作る。Emulatorは変更しない
- `Update Emulator`: 必要なら最新ソースをBuildし、成功した成果物だけをEmulatorへロードする

BASIC利用者は通常`Update Emulator`だけで作業でき、Assembler利用者はEmulatorを変更せず
`Build`してListing、Symbol Map、Binary等を確認できる。状態表示もSource、Build、Emulatorを分ける。

```text
Sources:  Changed
Build:    Out of date
Emulator: Previous build loaded
```

## External editing workflow

1. Studioで`Open Project`を選び、`pgp-project.json`を含むプロジェクトフォルダを指定する。
2. BASIC、ダンプ、アセンブリ等を任意の外部エディタで編集する。
3. Studioは内容のハッシュを定期的に比較し、作成、変更、削除を検知する。
4. `Update Emulator`は全ソースを先に検証・変換し、成功した場合だけロード処理へ進む。
5. ロード後は、ポケコンへ`RUN`、`DEF-S`、`CALL`等を直接入力して実行する。

ファイルシステムのタイムスタンプ精度に依存しないため、同じサイズの上書き保存も検知する。
変更検知自体は自動だが、編集中の不完全な状態をEmulatorへ自動ロードしない。
Studioは`source changed`を表示し、ユーザーが`Update Emulator`を実行した時だけ最新ソースを反映する。

プロジェクトのBASICをEmulatorへロードした後だけ、ロード直後のBASICプログラムを基準として
`Emulator BASIC: Synced`を表示する。PROモード等でEmulator内のBASICプログラムが変更された場合は
`Emulator BASIC: Modified`へ変わる。通常のプログラム実行による変数や表示内容の変更は対象外とする。
ROM変更、Reset、別プロジェクトのOpen、単発の`Load BASIC`ではこの追跡を終了する。

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
