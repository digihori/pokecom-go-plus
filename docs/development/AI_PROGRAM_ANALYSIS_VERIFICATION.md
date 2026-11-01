# プログラム解析の取り込み・実行観測・MCP検証

実施日：2026-11-01。[拡充計画](AI_MCP_EXPANSION_PLAN.md)第4段階の初版。
操作手順は[解析ガイド](../guides/AI_PROGRAM_ANALYSIS.md)、APIは
[AI連携仕様](../reference/AI_INTEGRATION.md#22-解析用プログラムの取り込み取得2026-11-01)を参照。

## 実装範囲

- PC-1245を対象に、BASIC／Assemblyソース、DMP、配置先指定のRaw Binaryをローカル画面で選択・確認・公開。
- 公開前はMCPから取得不可。最後に公開したコピーをメモリ内に保持し、元ファイルを再読込みしない。
- `pgp_get_analysis_context`でmetadata、物理行番号付きソース、取り込んだRAMの範囲限定逆アセンブルを取得。
- 対象ID・revision・入力hash・正規化ソースhash・image hashと、入口・メモの未検証性を明示。
- 対象差し替えで以前のIDを失効。解除・アプリ終了で破棄。MCPに任意パス指定や公開・Load・実行の操作は設けない。
- AIへのinstructions・tool description・metadataで、事実／推定／未確認と行・アドレスの根拠を区別するよう案内。

この機能はAIへ解析材料を提供するもので、アプリ内に自動の意味解析器やコード／データ分類器を追加したものではない。
線形逆アセンブルは制御フローや実行された命令の証明ではない。実行中メモリとの対応は別途照合する。

## 自動検証

JDK 21で`./gradlew build :core:desktopTest :desktopApp:verifyDistributionRuntime`成功。
Desktop 496件、失敗0、skip 3件。`git diff --check`成功。

追加したテストは次を確認した。

- ファイル選択・プレビューだけでは未公開、明示的な公開後だけ取得可能。
- 入力配列の変更が公開済みコピーに反映されないこと、差し替え・解除による旧読取りの拒否。
- BOM・改行の正規化、行ページングと文字量上限、型・値・不要引数の拒否。
- DMPの区間を保持し、隙間を読まずに逆アセンブルすること。
- LIDPのアドレスoperandとCALL等の制御フロー先を区別すること。
- 不完全な末尾命令は範囲外へ読まずDBになり、次ページが正しく終了すること。
- ROM・LCD・RAM終端越え・整数オーバーフロー等を拒否し、失敗時に以前の公開対象を維持すること。
- エミュレーターのSession providerへ触れないこと。
- 日英、幅420／800の画面で公開確認・公開・解除の操作と描画。
- 既存MCP・stdioの公開ツール数19件と回帰。
- Debug Context JSONの停止理由（Reset・Pause・Step・Breakpoint等とnull）の保持。

ファイル選択後にメイン画面へフォーカスが戻る問題は、ファイルダイアログのownerを解析画面にして修正。
以下の実画面検証で、選択後に解析画面へ戻ることも確認した。

## 配布アプリの実画面・stdio検証

macOS 26.6.2 / Apple Siliconの再生成した配布アプリを使用。
Project未選択の状態でローカル画面からファイルを公開し、別プロセスの同梱stdioブリッジから取得した。
プローブの作業ディレクトリはリポジトリ外の空フォルダーで、MCP引数にはパスを渡していない。
これはCodexによるツール経由の検証であり、CodexネイティブMCPやCopilotの新規受入れとは区別する。

| 操作 | 結果 |
| --- | --- |
| 公開前metadata | available=false |
| BASIC選択→確認→公開→source取得 | 成功、物理行1・2が以下の本文と一致 |
| DMP選択→確認→公開 | 成功、旧BASIC IDはSTALE_ANALYSIS_TARGET |
| disassembly取得 | 成功、2区間18 byteから9行を取得、次ページなし |
| 入口未指定 | entryBasis=unknownを保持し、逆アセンブルは先頭区間からと明示 |
| 公開解除 | available=false、旧IDのsource取得はNO_ANALYSIS_TARGET |
| 終了 | Studio終了で一時トークン削除、元ファイル変更なし、解析対象のLoad・実行なし |

### A01に対応する最小BASICの静的解析

```basic
10 CALL 50432
20 END
```

取得した本文の物理行1から、`0xC500`（10進50432）の外部機械語を呼ぶことが分かる。
行2は復帰後に終了する。明示的なBASIC変数やPRINTはない。
呼出先の内容・表示・破壊レジスタ・正常復帰は、この本文だけでは未確認。
Buildや実行は実施していない。

### A02に対応する最小機械語の静的解析

検証用DMP（共通fixtureのループ部分に対応、CASE部分とデータ領域は含めない）：

```text
C000: 10 C1 00 02 41 52 78 C0 20 02 09 79 C0 00
C020: 02 42 52 37
```

取得した逆アセンブルの確認事項：

- `0xC000`はLIDP `0xC100`、`0xC003`はLIA `0x41`、`0xC005`はSTD。
- `0xC006`はCALL `0xC020`、復帰先`0xC009`はLIA `0x09`、`0xC00B`はJP `0xC000`。
- `0xC020`はLIA `0x42`、`0xC022`はSTD、`0xC023`はRTN。

`0xC000`から正しい呼出し環境で実行されると仮定した解釈は、`0xC100`への`0x41`→`0x42`の書込みと繰返し。
全体がBASICへ戻る正常終了とは解釈しない。値の目的や、この入口が実際に使われるかは未確認。
今回は未Load・未実行で、書込み順をTraceで実測した結果ではない。

## A02最小入力の実行観測（追検証）

上記の自作18 byte DMPを、Project未選択の検証用PC-1245 Sessionへ画面からLoadした。
停止中Contextの`0xC000`から14 byteと`0xC020`から4 byteがDMPと一致することを確認。
入口Breakpoint・Trace容量256はstdio経由の`pgp_propose_debug_action`で提案し、
Studio画面で承認。各operationはSUCCEEDEDになった。
その後、画面から通常のBASIC `CALL 49152`を入力し、入口Breakpointで停止した。
PCの強制移動・ROM内固定アドレスへの起動・BASICへの正常復帰は行っていない。

以後は画面の1命令Stepを合計9回使い、各地点で同じSessionのContextをstdio経由で取得した。
入口時revisionをr0として、各Stepでrevisionが1ずつ進むことを確認した。

| 入口からのStep数 | 次のPC | 観測結果 |
| --- | --- | --- |
| 0 | `0xC000` | DMPとRAMが一致、`0xC100=0x00` |
| 3 | `0xC006` | `0xC005`のSTD直後、`0xC100=0x41` |
| 6 | `0xC023` | CALL先`0xC022`のSTD直後、`0xC100=0x42`、Rは入口より2減少 |
| 7 | `0xC009` | `0xC023`のRTN後、Rは入口時の値へ復帰 |
| 9 | `0xC000` | `0xC00B`のJP後、A=`0x09`、`0xC100=0x42`、対象18 byteは不変 |

末尾9件のTraceのアドレスとバイト列が、公開対象から得た命令に一致した。
実行順は`C000 → C003 → C005 → C006 → C020 → C022 → C023 → C009 → C00B`。
ROMからの起動中に古いTraceが容量を超え、`truncated=true`だったため、全起動履歴の取得とは扱わない。
メモリアクセス履歴は無効。書込み順の根拠は、命令履歴だけでなく2つのSTD直後に別々に取得したMemory値。
終了時は停止したままにし、無限ループを正常終了と解釈していない。

### 観測で見つかった停止理由の出力不具合

UIにはBreakpoint／Step完了の停止理由が表示されたが、初回のMCP応答は`stop=null`だった。
JSON writerで`put`の戻り値にElvis演算子を使い、追加した停止理由をnullで上書きしていたため、
値を選んでから1回だけ`put`するよう修正した。停止理由別の回帰テストを追加。
修正版の配布アプリを再起動し、実際の一時停止で`stop.kind=userPause`が返ることを再確認した。
上表の9命令観測は修正前の実行で、停止位置はCPUのPC・画面・Traceを併用して確認した記録である。

## Copilotによる解析APIの実接続検証

VS Code 1.143.1の「ローカル」実行環境、表示モデルGPT-6 Luna（Auto）を使用。
既存の接続検証チャットを継続し、検証用ワークスペースに同梱stdioブリッジの設定を一時追加した。
認証トークンは設定に保存していない。以下は誘導付きの最小入力検証であり、
共通課題が求める新規チャットでの独立した全fixture受入れとは区別する。

| 読取り | 確認した結果 |
| --- | --- |
| BASIC metadata → source | 対象ID・revisionを使い、物理行1のCALL先を50432=`0xC500`と解釈。END、明示INPUT／PRINTなしを説明し、呼出先や副作用を未確認とした |
| DMP差し替え後metadata → disassembly | 新しい対象ID・revisionで2区間9命令を取得 |
| 停止中Debug Context | 対象18 byteとRAMの一致、`0xC100=0x42`、PC=`0xC000`、直前PC=`0xC00B`を説明 |
| Traceの解釈 | CALL→サブルーチン→RTN→`0xC009`への復帰を確認し、最終値と命令履歴だけでは途中の`0x41`書込みを実測と断定しなかった |

Copilotには上表のSTD直後の2つのContextを渡していないため、Copilot自身が書込み順を実測確認したとは扱わない。
操作提案・承認・Load・CALL・StepはCodexのプローブと画面操作で実施し、Copilotは読み取りを検証した。

### 実行状態を誤解させるmetadataの修正

初回CopilotはRAM一致を確認した後も、metadataの`loadedIntoEmulator=false`と
`userProvidedNotExecuted`を根拠に「現在も未ロード」と解釈した。
解析用コピーがLoadしないことと、別操作でLoad・実行された現在のSessionの状態を混同させる表現だったため、
前者を`importLoadsEmulator=false`、対応関係を`runtimeCorrespondence=notTracked`へ変更。
実行状態はここでは追跡しない旨を`runtimeStateNotice`に明記し、入口を`userProvidedUnverified`へ変更した。
画面の入口表示も「未実行」から「未検証」へ変更した。

修正版の配布アプリを再起動・再公開し、Copilotにmetadataだけを再取得させた。
新しい対象IDを使い、上記フィールドを根拠に「現在のエミュレーターが未ロード・未実行とは判断できない」と
説明することを確認。古いSessionや対象IDを再利用せず、Debug Contextの追加取得も行わなかった。
検証終了後はStudioを終了して認証ファイルの削除を確認し、検証用VS Code設定を空のserversへ戻した。

## 残る検証・機能

- 全Debugger fixtureと、新規チャットでの独立したA01／A02受入れ。最小18 byteの実行観測とCopilot読み取りは上記で確認。
- 新APIを使うCodexネイティブMCP、Copilot以外のAIクライアント、Windows・Linuxでの画面・配布検証。
- BASICトークン列の解析用入力、他機種、ROM由来プログラムの取り込みは初版対象外。
- Project新規生成、キー操作・LCD取得、実行上限は後続段階。

静的解析の材料を取得できたことと、任意の既存プログラムを完全に解析・実行確認できることは区別する。
