# 次回リリース前の7件の修正

2026-10-30。7件と比較演算子の修正は実装・自動検証済みです。LC-3実動作とLinux分類も確認済み。
Studioの操作性・互換性を中心とした変更で、残る環境別の手動確認は下記に記録します。公開版数は変更しません。
WAV変換の追加デバッグ、Playerのマシン語保存、PlayerのROMセット作成、複数AI対応は今回の対象外です。

| 順序 | 項目 | 実装内容 |
| --- | --- | --- |
| 1 | RAM既定値・保存 | Studioは未設定なら実機RAM。1250／1251／1255共通の選択を保存・復元。不正な設定値も実機RAMへ戻す。保存失敗時は選択を変更せずエラーを表示 |
| 2 | キー入力漏れ | ダイアログ開閉状態をcomposition中に監視。Project新規・改名、保存・アドレス指定等のダイアログ中はEmulator入力を遮断。設定・ROMセット作成も対象。Debugger入力欄は欄ごとの所有権で遮断し、フォーカス移動・破棄で解除 |
| 3 | MCP承認ボタン | ソースレビューの承認・却下を差分の上へ移動。差分は高さ制限付きで独立スクロール。新しいソース要求は履歴より先頭に表示し、外側のスクロールも先頭へ戻す |
| 4 | 矢印キー刻印 | ↕の絵文字fallbackを避け、↑と↓を上下に少しずらして重ねる。通常キー・可変サイズ描画の両方に適用。STAT／TITLEやキー動作は維持 |
| 5 | Linux分類 | 配布設定のmenuGroupをDevelopmentへ変更 |
| 6 | エスケープ・円記号 | 機種別PI／SQR／EX／INS／BXを変換し、保存時も名前付きescapeへ統一。¥／￥／バックスラッシュ2文字を機種別円記号コードへ変換し、保存は¥。OLD以外のEXは診断 |
| 7 | Project機種制約 | 開くだけでは機種・ROMを切り替えない。Buildは現在のSession機種・RAM構成を優先。SessionがなければManifest機種と選択RAM設定を使用。BASICの変換先を切り替え、機種ID不一致だけの拒否を撤廃 |

## 互換性と境界

- RAMモード切替は従来どおりSessionを再生成するため、RAM上のプログラムは先に保存してください。
  Core API単体の既定値は今回変更せず、Studioが設定を明示してSessionを生成します。Playerは実機RAMを維持します。
- 特殊文字のコード表・連続escape・カナ／日本語との関係は[BASICテキスト仕様](../../reference/BASIC_TEXT_FORMAT.md)を参照してください。
  自動テストの文字変換確認と、後述のLC-3本体の実動作確認は区別して記録します。
- Projectのformat version 1と必須machineIdは維持し、machineIdはSession不在時の既定値として扱います。
  新しい互換グループやManifest形式は追加しません。
- Assembly／DMPのロード先アドレスは自動変換しません。RAM外・物理ミラー重複・BASICとの衝突検査を維持します。
  機種固有I/O・LCD・ROM呼出しの互換性はプログラム側で対応します。
- UIとMCPは現在機種でBuildし、Build成果物には変換先機種を保持します。MCPの内容revisionにも現在機種・RAM構成を含め、
  切替前のBuildをそのままLoadしないようにします。機種切替時にはAssembly previewとLoad表示を無効化します。

## 検証

JDK 21で`build :core:desktopTest`が成功しました。Core desktop tests 499件成功、
Studio 421件中420件成功・既存1件skipです。`git diff --check`も成功しました。
最終の全体buildはAndroid DEX生成のメモリ不足を避けるため、一時的にGradle heapを4 GiB、
並列workerを2にして実行しました（配布設定・gradle.propertiesの変更なし）。
Android lintはKotlin metadataのバージョン診断を出力しましたが、タスクと全体buildは成功しました。

- 全14モデルの特殊記号コード・正規化出力・保存再読込み、円記号入力別名、LC-3形式の文字変換、非対応EXの拒否。
- 設定未保存時の実機RAM、両モードの保存・別storeでの復元、不正値のfallback、保存失敗時の旧値保持。
- Logical／Physicalの入力遮断、保持キーの解放、入力欄間の移動・欄の破棄を含むComposeフォーカス操作。
- 日英で長い差分のスクロール前後に承認・却下ボタン位置が維持されることと、両ボタンの操作。
  通常／可変サイズの矢印刻印も隔離描画し、PNGを目視確認しました。
- Manifestと別機種向けのBASIC変換、Assemblyの配置拒否、承認付きMCP Load、機種切替後の旧Build拒否。

描画結果は`desktopApp/build/reports/seven-fixes/`に生成します。
以下の手動受入れは別途必要です。

- 配布アプリでRAM切替・終了・再起動、ROM自動復元後のMEM表示。
- Project名、保存アドレス、Debugger各入力欄で文字・Tab・Enter・キャンセル・フォーカス移動を確認。
- Windows／Linuxを含む実画面での矢印とMCPレビュー。Linuxのdeb導入後のDevelopment分類は確認済み。
- 実ROMを使った異なる機種間のProject Build／Load。

### LC-3の動作確認（2026-10-30）

PC-1261のLC-3が正常に動作することを確認しました。特殊文字・比較演算子の修正後の実利用確認として記録します。
個別の操作手順・使用ソース・配布物の識別情報は未記録です。全LC-3構文や全機種の確認完了を意味しません。

既存のiOS署名・ストア公開関連の作業ツリー変更は、この7件の修正対象には含めません。

## 追加修正：BASIC比較演算子の欠落（2026-10-30）

PC-1261で`>=`が`>`になる読込み不具合を確認しました。
1260／1261と1350のTokenizerに、`<=`を3C、`>=`を3E、`<>`を3Cへ縮める誤った登録がありました。
ASCIIの比較文字列をそのまま保持するよう登録を削除しました。
逆変換でも単独の`<`・`>`を複合演算子へ誤変換しなくなります。
OLD系の専用トークン表は変更しません。

S1／S2全10モデルで`>=`・`<=`・`<>`・`>`・`<`・`=`について、通常の式・文字列・REMの
実byte列と保存再読込みを確認する回帰テストを追加しました。
すでに欠落した状態でロード済みのプログラムは自動修復できないため、修正版で元ソースから再ロードします。

追加修正後、JDK 21の`build :core:desktopTest`成功。Core desktopは500件すべて成功、
Studioは421件中420件成功・既存1件skipを確認しました。`git diff --check`も成功しました。

## Linux検証用deb（2026-10-30）

[Linux検証ビルド](https://github.com/digihori/pokecom-go-plus/actions/runs/40972395816)で、
Core／hostInput／Studioのテスト、runtime検査、packageDeb、分類検査が成功しました。
生成ソースは`f9817aaa47fdbc1cc61b9e89cd0edcade8fa8091`（検証専用ブランチ）です。

- ファイル：`desktopApp/build/linux-verification-20261030/PokecomGOStudio-1.0.0-linux-amd64-test-20261030.deb`
- 公開版数1.0.0／deb内部版数1.6.0、amd64、65,254,520 bytes。
- SHA-256：`181fccab3b90eff7410d1abb718f0e630ce9a1790325493f74164d0155ade9e1`
- 取得後のchecksum一致と、debを直接展開したdesktop entryの`Categories=Development`を確認。
  旧1.0のdebは`Categories=Unknown`でした。
- 2026-10-30：検証用debの導入後、Developmentグループに表示されることを確認しました。
  Linux分類改善の手動受入れは完了です。ディストリビューション・デスクトップ環境の詳細は未記録です。
- 同じ内部版数での検証更新は`sudo dpkg -i ./PokecomGOStudio-1.0.0-linux-amd64-test-20261030.deb`で行います。

Linuxの新しいJDKは、カンマ区切りの不正HostをHTTP parserで400として拒否するため、
既存テストを400／403の拒否を許容する形へ修正しました。tool未実行の検証と、本番の拒否処理は維持しています。
検証ブランチのworkflowはLinuxパッケージ確認専用です。正式Releaseへの公開は行っていません。

## 文書の更新

日英READMEのROM経由BASIC入力の案内を削除し、通常のTokenizerロードへ統一しました。
RAM設定・Project機種制約・Linux分類・MEM確認の説明を現状へ合わせ、公開版と開発版の違いを明記しました。
次の開発4案件と後続の複数Project構想は[次のリリース案件](NEXT_DEVELOPMENT_CANDIDATES.md)へ分離しています。
