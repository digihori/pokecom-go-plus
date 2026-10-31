# Pokecom GO Studio / Android Player 1.1.0

リリースノート（2026-10-31）。タグ：`v1.1.0`。比較対象：`v1.0.0`。
機能・受入れ記録の基準はmainの`d493f16`。最終配布候補のcommitと検証結果は
[公開チェックリスト](../checklists/RELEASE_CHECKLIST_1.1.0.md)で確定します。

## 日本語

StudioはmacOS Apple Silicon、Windows x64、Linux x86_64、Android PlayerはAndroid 8.0以降を対象とします。
対応14モデルは1.0.0を継続し、ROM・ゲームは同梱しません。AndroidはGoogle Playで更新します。
iOS 0.7.0（build 3）は審査中で、今回のStudio／Android公開とは別に次回1.1.0を準備します。

### 主な変更

- BASICロード後に旧RUN位置・カーソル状態が残る問題を修正。PC-1245、PC-1401／1402、PC-1360／1360Kの直接ロード・結合ロードに対応しました。
- BASICの機種別特殊記号・円記号と保存再読込みの互換性を改善。PC-1260／1261・1350で比較演算子が欠落する問題を修正しました。欠落済みのプログラムは元ソースから再ロードしてください。
- Studioの未設定時RAMを実機RAMにし、125xのRAM選択を保存・復元します。RAM切替前にはプログラムを保存してください。
- Projectを開くだけでは機種を切り替えず、Buildは現在のSession機種・RAM構成を優先します。Assembly／DMPのアドレスは自動変換しません。
- Debuggerへ停止中のメモリ検索・1byte RAM編集、条件付きログポイント、bank指定Breakpoint、上限付き履歴／Traceの独立JSON出力を追加。観測したCASE復帰経路のStep Over／Outを改善しました。
- Quick Assembleに結果プレビューを追加し、共通のアセンブル処理を使用します。
- ダイアログ・Debugger入力欄のキー入力漏れ、検索パネル更新、履歴保存後のフォーカスを修正。MCP承認ボタンと提案受信時の画面表示、矢印キー刻印を改善しました。
- WAV録音入力の44.1／48kHz対応とS1／S2のbyte同期・Binary metadata処理を改善。PC-1245／1401／1360KのBASIC・Binary計6ケースで実機往復OKの報告を受領しました。
- Linuxのアプリメニュー分類をDevelopmentへ変更しました。

### 検証と制限

BASICの3機種の手動確認とWAV実機往復、DebuggerのmacOS手動24ケースおよびWindows／Linuxの動作確認報告を記録済みです。
これは1.1.0最終配布物の検証完了や全機種・全ROMの網羅確認を意味しません。
DebuggerのT22（別bankの同じ論理位置を通る実行確認）は検証プログラム待ちで保留です。
WAVの全長境界・保護付き形式・全機種・日本語の網羅確認は未完了です。
Playerのマシン語保存・ROMセット作成、複数AI対応は今回の対象外です。

公開版数1.1.0、Studio内部パッケージ版数1.7.0、Android versionCode 11。
Windows MSIのUpgradeCodeを維持します。Windowsは未署名のためOS警告が表示される場合があります。
macOSの署名・公証は最終候補で確認します。Android debug APKはGitHub Releaseへ添付しません。
更新前にプログラム・Project・ROM原本をバックアップしてください。Playerにセーブステートはありません。

## English

Studio and Android Player 1.1.0; changes since `v1.0.0`, based on `d493f16`.
Studio targets macOS Apple Silicon, Windows x64 and Linux x86_64; Android Player requires Android 8.0+.
The same 14 models remain supported. ROMs and games are not included.
iOS 0.7.0 (build 3) remains under review; iOS 1.1.0 preparation is separate.

- Fixed stale BASIC navigation and cursor state after loading on PC-1245, PC-1401/1402 and PC-1360/1360K.
- Improved BASIC special-character/yen round trips and fixed lost comparison operators on PC-1260/1261 and PC-1350. Reload damaged programs from their original source.
- Studio defaults to physical RAM and remembers 125x RAM choices. Save programs before switching RAM modes.
- Opening a Project keeps the current machine; builds use the current Session machine/RAM. Assembly/DMP addresses are not translated.
- Added paused memory search, single-byte RAM editing, conditional logpoints, bank-scoped breakpoints and bounded independent JSON history/trace export. Improved Step Over/Out for observed CASE returns.
- Added Quick Assemble result previews using the shared assembly pipeline.
- Fixed input leakage, stale debugger panels and export-dialog focus; improved MCP approval visibility and arrow-key labels.
- Improved 44.1/48 kHz recorded WAV input, S1/S2 byte synchronization and binary metadata handling. Six BASIC/binary hardware round trips passed on PC-1245, PC-1401 and PC-1360K.
- Linux packages now use the Development menu category.

Existing acceptance reports do not replace testing the final 1.1.0 packages. Debugger bank-execution case T22 remains deferred.
WAV boundary, protected-format, Japanese and all-model coverage remain incomplete.
Player machine-code saving, ROM-set creation and multiple AI connections are outside this release.
Public version: 1.1.0; Studio package version: 1.7.0; Android versionCode: 11. The MSI UpgradeCode is preserved.
Windows packages remain unsigned. Verify macOS signing/notarization on final assets. Android updates use Google Play; debug APKs are CI artifacts only.
Back up programs, projects and ROM originals before updating. Player has no save states.
