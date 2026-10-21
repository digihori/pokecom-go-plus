# Golden Test Data v1

## 1. 目的

Pokecom GO、PGP、将来の別実装で、同じROM、入力列、実行量に対する状態を比較する。
Golden DataはROMイメージやSave Stateではなく、再現手順と観測可能な期待値を記録する。

## 2. 配置と再配布

- `local-data/golden/`: 実ROMから採取した未審査・包括的データ。Git管理外
- `test-data/golden/`: 合成プログラム、または内容を確認して再配布可能と判断した最小データ
- `test-data/golden/schema-v1.json`: 共通JSON Schema

ROM byte、逆アセンブル結果、ROM由来の文字列はGolden Dataへ格納しない。実ROMはSHA-256で
識別する。RAM範囲にもROMからコピーされた内容が含まれ得るため、Gitへ追加する前に確認する。

## 3. ケース構造

1ケースを1つの`.json`ファイルにする。文字コードはUTF-8、整数は10進、byte列とアドレス表示は
小文字16進とする。最上位の`schemaVersion`は`pgp-golden-1`で固定する。

```json
{
  "schemaVersion": "pgp-golden-1",
  "caseId": "pc1245-cold-reset",
  "description": "Cold reset直後の状態",
  "machineId": "pc-1245",
  "producer": {
    "implementation": "pokecom-go",
    "revision": "Git commitまたはリリース識別子"
  },
  "rom": {
    "format": "pokecom-go-flat-64k",
    "sha256": "64桁のSHA-256"
  },
  "actions": [
    { "type": "reset" },
    { "type": "checkpoint", "expectation": "after-reset" }
  ],
  "expectations": {
    "after-reset": {
      "cpu": {
        "programCounter": "0000",
        "dataPointer": "0000",
        "p": "00",
        "q": "00",
        "r": "60",
        "carry": false,
        "zero": false,
        "powerOn": true,
        "internalRamHex": "省略せず512桁"
      },
      "memory": [
        { "start": "8000", "bytesHex": "00000000" }
      ],
      "display": {
        "enabled": true,
        "rows": [
          "80文字の0/1", "80文字の0/1", "80文字の0/1", "80文字の0/1",
          "80文字の0/1", "80文字の0/1", "80文字の0/1"
        ],
        "symbols": ["RUN"]
      },
      "audio": { "frequencyHz": 0 }
    }
  }
}
```

## 4. Action

Actionは配列順に適用する。

| `type` | 必須フィールド | 意味 |
|---|---|---|
| `reset` | なし | Cold reset |
| `step` | `count` | 指定命令数を実行 |
| `runCycles` | `cycles` | 指定サイクル以上になる最初の命令境界まで実行 |
| `pressKey` | `key` | 論理キーを押す |
| `releaseKey` | `key` | 論理キーを離す |
| `setOperatingMode` | `mode` | `RUN`または`PROGRAM`を設定 |
| `checkpoint` | `expectation` | 同名の期待値と現在状態を比較 |

ホスト時刻、sleep、フレーム数はActionに含めない。時間は`runCycles`だけで表す。

## 5. Expectation

期待値は必要な観測だけを書ける。省略フィールドは比較しない。ただしCPU命令単体ケースでは、
意図しない変更も検出するためCPU全フィールドと内部RAM全体を原則記録する。

- CPUの16-bit値は4桁、8-bit値は2桁の16進文字列
- `internalRamHex`はアドレス0から順に256 byteを連結した512桁
- `memory`は必要なRAM/I/O範囲だけを記録し、範囲を重複させない
- LCDはrow-majorの7行×80列を`0`/`1`で表し、描画色や拡大率を含めない
- symbolは`DisplaySymbol`名を辞書順で格納する
- `revision`は最適化用の実装詳細なのでGolden比較対象にしない
- 実行停止を確認するケースだけ`execution`へ`READY`またはunsupported opcodeを記録する

## 6. 採取と承認

1. Pokecom GOのrevisionとROM SHA-256を固定する。
2. Pokecom GO側で同じActionを実行し、`local-data/golden/`へ採取する。
3. PGPで再生し、差分をCPU、RAM、LCD、I/O単位で確認する。
4. 差分の理由を修正または明文化する。PGPの出力で期待値を無条件に上書きしない。
5. Gitへ入れる場合はROM由来データとライセンスをレビューし、`test-data/golden/`へ最小化する。

最初のケースはcold reset、ROM起動100万サイクル、RUN/PROGRAM切替、単一キー押下、BREAKの
順で採取する。BASICプログラムや長いRAMダンプは後続ケースへ分離する。

## 7. PGPでの再生

Desktopテストの`GoldenScenarioRunner`がJSONをstrict modeで読み込み、ROM SHA-256を確認してから
公開`EmulatorSession` APIだけでActionを再生する。Expectationは指定されたフィールドだけを比較し、
エラーにはcheckpoint名と対象フィールドを含める。

`test-data/golden/pc1245-synthetic-reset.json`は再配布可能な合成NOPT ROMのケースであり、JSON読込み、
SHA-256拒否、reset、`runCycles`、CPU、RAM、LCD、audio比較をCIで検証する。実ROMケースも同じRunnerを
使うが、JSONとROMの両方を`local-data/`に留める。

## 8. PGP Snapshotの採取

`GoldenSnapshotExporter`は明示的に`PGP_EXPORT_GOLDEN=1`を指定したDesktopテストからだけ実行する。
PC-1245ではreset直後と100万サイクル後を採取し、CPU全フィールド、内部RAM、論理RAM
`0x8000..0x87ff`、LCD/I/Oミラー`0xf800..0xf8ff`、論理LCD、audio、実行状態を保存する。

producer revisionは`PGP_GOLDEN_PRODUCER_REVISION`から必須入力とし、dirty treeの場合は呼出し側が
`-dirty`等を付ける。出力先は`local-data/golden/pc1245-pgp-boot-1000000.json`で、書込み後に
同じROMとRunnerで全checkpointを再生検証する。

この出力はPGPの現在値を固定する回帰用データであり、Pokecom GOとの一致を証明するGolden正解値ではない。
Pokecom GOから採取したproducer=`pokecom-go`のケースと比較し、差分を評価してから期待値を昇格させる。

## 9. Pokecom GO既存Save Stateの取込み

Pokecom GOはActivityの`onPause()`で`Sc61860params`をGson JSONにし、default SharedPreferencesの
`PREF_SC`へ保存する。`PokecomGoStateImporter`はこの既存JSONを変更なしで読み、CPU、内部RAM、
選択メモリ、PC-1245 LCDとRUN/PRO状態を`GoldenExpectation`へ正規化する。

debug buildを接続した端末では、アプリをpauseまたはforce-stopした後、次のようにpreferences XMLを
端末外へ取得できる。端末、SDK、ビルド設定によって`run-as`が使えない場合がある。

```bash
adb exec-out run-as tk.horiuchi.pokecom \
  cat shared_prefs/tk.horiuchi.pokecom_preferences.xml \
  > local-data/golden/pokecom-go-preferences.xml
```

`PREF_SC`はXML entityをdecodeしてJSONとしてImporterへ渡す。preferencesには他のユーザー設定も
含まれるため、ファイル全体をGitへ追加しない。

PGPの`PokecomGoPreferencesReader`はXMLから`PREF_SC`だけを抽出し、entity decode後のJSONを
`PokecomGoStateImporter`へ渡す。DTD、外部entity、外部schemaを無効化し、採取ファイルに意図しない
外部参照が含まれていてもローカルファイルやネットワークへアクセスしない。

採取した実データの取込み確認は、次の明示的なDesktopテストで実行する。環境変数を指定しない通常の
ビルドでは、Git管理外の採取ファイルを要求しない。

```bash
PGP_VERIFY_POKECOM_GO_STATE=1 ./gradlew :core:desktopTest
```

既存Save Stateには累積命令数や累積サイクルがない。したがって自由走行後のSnapshotをPGPの
`runCycles` checkpointへ直接対応付けてはならない。reset直後、step数を固定したdebug実行、または
将来の一時的な採取patchで実行境界を固定した状態だけを比較対象にする。

## 10. 命令トレースによる残差調査

固定cycle比較でCPU位相差が残る場合、最終Snapshotを推測で合わせず、両実装で同じTSV列を出力する。

```text
instruction  cycleBefore  cycles  pc  opcode  pcAfter  q  ib  testPort
```

PGP側は公開`EmulatorSession.step()`だけを使うtest utilityで採取し、通常のCore実行経路へtrace hookを
追加しない。まずcycle checkpointを段階的に狭め、分岐直前の有限tailを比較する。Pokecom GO側は
一時debug copyだけに同じ列の出力を追加し、参照リポジトリや製品動作へ常設しない。

実ROMの100万cycle直前256命令は、`PGP_EXPORT_INSTRUCTION_TRACE=1`を明示したDesktop testだけが
`local-data/golden/pc1245-pgp-trace-1000000-tail.tsv`へ出力する。通常testとCIはROMも出力先も要求しない。

起動直後からの分岐調査が必要な場合だけ`PGP_EXPORT_FULL_INSTRUCTION_TRACE=1`も併記し、最大30万命令を
`local-data/golden/pc1245-pgp-trace-1000000-full.tsv`へ出力する。ファイルは大きくなるためGit管理外とし、
通常の回帰確認では有限tailを使う。
