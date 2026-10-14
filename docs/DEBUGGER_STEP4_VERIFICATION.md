# Debugger ステップ4 検証記録

## 範囲

登録タブの日英UI、ブレークポイント／監視の一覧・追加・編集・削除・有効切替、複数監視を実装。
停止中に編集し、無効ブレークポイントは逆アセンブル行から同じIDで再有効化する。
登録はRunner内に保持し、Resetで維持、新Sessionで解除する。値変更基準はReset／host load／再有効化で更新。
16bit論理アドレスは全ROMバンク共通。機種固有のCore観測・RAM境界の意味は維持する。
256ブレークポイント、64監視、値変更は各4096バイト以内かつ有効範囲合計4096バイト以内。
重複範囲も比較コストとして数える。入力・上限・観測失敗は型付き結果をUIで翻訳し、登録操作は失敗時に変更しない。

読み書き停止を値変更より優先するが、全値変更基準を毎命令更新する。
同じアクセスの複数監視一致は重複させず、実際の繰り返しアクセスは残す。
無効登録だけなら通常のまとめ実行を維持する。Core faultの停止優先度を維持する。
従来の単一監視setterは自カテゴリのみを置換する互換入口として残す。
MCP tool名・Debug Context schema version 1・停止情報形式を変更しない。登録一覧のMCP公開は追加しない。

条件・ヒット回数、バンク指定、永続保存、Step Over／Out、ソース連携等は未実装。

## 自動検証

JDK 21によるmacOSローカル検証:

- 全体 `build` 成功。
- `:core:desktopTest` 成功（435件、失敗0。変更なしのUP-TO-DATE結果を再利用）。
- Studio 309件、失敗0、既存SDL条件付きskip 1件。
- 新規15件: 登録モデル10件、無効登録のまとめ実行1件、日英UI操作4件。
- CRUD・同一ID保持・重複／上限の失敗時不変・複数監視の独立停止・重複一致・基準更新・Reset・旧setter互換・Fault優先・MCP取得不変を確認。
- UI Semanticsで追加／編集／削除／有効切替、不正入力、実行中無効化、逆アセンブルとの共有、タブ間の編集保持を確認。
- 既存停止／再開／Step／Run To／Breakpoint／読み書き監視、履歴・Trace・Checkpoint・MCP・ローカライズ整合テストも成功。
- `git diff --check` 成功。

## 描画・環境の区別

日英、登録／監視／実行中／空一覧、1200×900／420×800の16枚を独立描画。
代表5枚を目視確認し、狭幅の折返し、一覧、入力欄、有効状態、操作無効化を確認。
出力先は `desktopApp/build/debugger-step4-preview/`。合成ROMのSessionと本番Composableを注入する。
実Preferences・ROM Library・Studio main・SDL・MCPサーバーを起動せず、Java prefs rootだけの隔離は使わない。
本番macOSウィンドウでの実ROM・マウス／キーボードfocus・実スクロール確認は未実施。
Windows／Linux／iOSのCI・実機確認は本件では未実施。既存commitのCI成功と今回の確認は区別する。

## 保護対象

ステップ1〜3の既存変更を保持。Core commonMain、実設定、ROM Library、参照プロジェクト、保護スキン、公開タグを変更しない。
ROM由来fixtureは追加しない。packageVersionは1.3.1。実装時点ではcommit／push未実施。今回の公開準備の集約は[準備記録](DEBUGGER_PREVIEW_READINESS.md)を参照。
