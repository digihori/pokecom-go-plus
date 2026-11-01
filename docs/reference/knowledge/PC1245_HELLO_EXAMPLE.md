# PC-1245 HELLO WORLD作例

作例IDは`pc1245-hello-world`。内蔵assembler向けのBASIC＋Assembly Projectです。
`pgp_get_example`でmanifestと2つのソース本文を取得できます。ROMと生成済みバイナリは含みません。
実際のBuild入力と同じ既存fixtureをビルド時に同梱し、別のソースコピーを手で保守しません。

BASICが機械語`0xC500`をCALLし、独自の5×7字形でHELLO WORLDを描画してRTNで戻ります。
コード・字形は`0xC600`未満。X/Yを保存し、A/P/Q/DP/フラグは変更します。
HOLD_STARTとDONEのラベルがあり、DONEにBreakpointを置いて表示とRTNによる復帰を確認できます。
表示保持時間は機種のサイクル設定に依存するため、この知識項目は秒数を保証しません。

利用手順：
1. 利用者が検証用の空のProject領域を用意し、取得したmanifestとソースを保存する。
2. PC-1245の利用可能なROMと内蔵assemblerを選ぶ。
3. Buildし診断を確認する。Loadは停止状態で承認し、BASICとAssemblyをまとめて配置する。
4. 必要な停止条件を登録し、再開後にRUNモードで手動のRUNとENTERを入力する。
5. 表示とBASICへの復帰を確認する。MCPで画面を取得できなければ利用者の目視確認と記録する。

既存のPc1245HelloWorldTestは共通Build/Load、合成CALL→描画→RTN、画素とスタックを検証します。
実ROMを使う検証はROMがない環境ではskipされます。READMEには過去のユーザーによる表示確認記録があります。
任意ROM・実機の動作保証を意味せず、テストファイルの存在だけで今回の実ROM検証済みとは扱いません。
