# PC-1245 Assembly HELLO WORLD

AI連携・作業順9の最小受け入れサンプル。対象は **PC-1245 / Studio内蔵SC61860 assembler**。
ROM、外部assembler、生成済みバイナリは同梱しない。

## AIへ渡す情報

1. `pgp_get_machine_spec({"machineId":"pc-1245"})` を読む。
2. `pgp_get_development_context({"section":"metadata"})` で機種と内蔵assemblerを確認する。
3. 機種仕様の `display.directDrawing`、開始位置 `0xC500`、命令一覧からソースを生成する。
   ROM文字表示ルーチンのアドレスやABIは推測しない。
4. このサンプルの再生成では「HELLO WORLDを5×7の列データで描画し、残りの列を消去し、
   LCDを有効化してRTNで戻る。ROM呼び出し禁止。内蔵assemblerの基本命令だけを使う」と指定する。

`src/hello.asm` の字形データは独自作成。描画は2つの短いループで行い、字形データは各列を明示した。LCDは80列で、後半20列のアドレスは逆順。

## Studioでの再現

1. フォルダ全体を作業用の場所へコピーし、PC-1245の利用可能なROMをStudioで起動する。
2. Projectを開き、`pgp-project.json` のあるフォルダを選ぶ。Assemblerを内蔵にする。
3. Build成功を確認し、Loadする。BASICとAssemblyを一緒にロードする。
4. RUNモードで `RUN` を入力しENTER。LCDに `HELLO WORLD` が約3秒表示され、終了して入力待ちへ戻る。
5. 表示直後の復帰を厳密に確認する場合は `DONE` (`0xC556`) にブレークポイントを置いて
   実行する。停止時に表示を確認し、StepでRTNを実行する。PCはROMへ戻り、Rは2増える。
6. 続行して終了後、CLEAR、`1+1`、ENTERで通常のBASIC操作へ戻れることを確認する。

## AI連携で進める場合

1. 上記同様、作業用コピーを開いてPC-1245と内蔵assemblerを用意する。新規Project作成は手動。
2. AIへ機種仕様の取得と、登録済みSourceへの変更提案を依頼し、Studioで差分を承認する。
3. AIがBuildを要求するので承認し、診断と成功成果物を確認する。
4. Sessionを一時停止し、AIからのLoad要求を承認する。BASICも一緒にLoadし、停止状態を確認する。
5. Continue要求を承認して再開し、RUNモードで手動で `RUN` → ENTERを入力する。
   ContinueだけではRUN / CALLを入力しない。
6. 表示と復帰を確認する。厳密な停止確認にはAIに `DONE` のBreakpoint設定を依頼して承認する。

アドレス案内は原則16進（開始 `0xC500`）。同梱BASICの `CALL 50432` はその10進表記。
言語や機種に合う実行構文を使い、説明上の `0x` をそのままBASICへ入力するとは限らない。
ユーザーによるHELLO WORLD表示確認済み。Load／Continue／Pauseの実接続結果は
[AI連携記録](../../../docs/reference/AI_INTEGRATION.md)を参照。

## メモリと制約

- BASIC: `10 CALL 50432` / `20 END`。Assembly: code `0xC500..0xC556` / data `0xC580..0xC5BB`、計147 bytes。
- コードと字形は `0xC600` 未満に収める。上位RAMはROMが作業用に書き換える。
- PC-1245のBASIC管理ポインタ `0xC6E1..0xC6E4` を破壊しない。
- `LIP 0x5F; ORIM 0x01; OUTC` で他の制御ビットを保ってLCDを有効にする。
  実ROMのCALL中はLCDが無効になるため、描画だけでは表示されない。
- A、P、Q、DP、演算フラグを変更する。BASIC CALLのスタック上で実行しRTNで戻る。
  任意のPCジャンプによる起動はRTNの戻り先を用意しないため対象外。
- LCDのシンボル領域 `0xF83C/0xF83D` は書き換えない。終了後はROMの再描画により表示が変わり得る。
- PGPの実装上の動作確認であり、実機の電気的動作や任意のROM版のABI保証ではない。

## 検証

`Pc1245HelloWorldTest` はこのフォルダのソースをStudioの共通Build/Load処理で処理し、
合成ROMのCALL→描画→RTN、80×7全画素、スタックの復元を検証する。
ローカルのPC-1245 ROMがある場合は実ROMでBASIC RUN→CALL→描画→RTN→再入力も検証する。
ROM未提供の場合、実ROM検証はskipとして報告する。ROM本体や固定のROMアドレスはテストへ埋め込まない。

再生成した別プロジェクトも同じ受け入れテストで検証できる。`PGP_HELLO_PROJECT` にそのフォルダを指定し、
`Pc1245HelloWorldTest` を実行する。Assembly sourceには表示保持の入口 `HOLD_START` とRTN位置 `DONE` のラベルを設ける。
未指定時はこのサンプルを検証する。
