# PC-1251のBASIC・メモリ・表示とAI対応範囲

対象：PGPのPC-1251実装。実装由来の情報であり、実機・任意ROMでの動作保証ではない。
PC-1250／PC-1255を同じmachineIdとして扱わない。

## BASIC

CoreのPC-1251定義はOLD BASICを選択し、プログラムcodecはPC-1245と共通の
OLD方言定義を使う。`pc1251.basic`の構造化データはそのコード表・行番号上限・
行形式を返す。これは完全な実行時文法やROMルーチンABIではない。
ROMエディターへの入力にはPC-1251専用の文字・特殊記号キー変換を使うため、
PC-1245と同じキー列で入力できるという意味ではない。

## RAMと配置

`pc1251.memory`はCoreのカタログ領域と既定配置を返す。
PC-1251の物理RAM開始は0xB800、EXPANDED設定ではPC-1255相当の0xA000まで
拡張する。HARDWARE／EXPANDEDで利用可能なRAMとミラーが変わる。
カタログのmemoryRegionsは説明用であり、現在のSessionの書込み可否を保証しない。

知識APIのmemoryLayout.scopeはcatalogDefaultsで、実行中Sessionは参照しない。
現在の構成は`pgp_get_machine_spec`をmachineId未指定、または現在の機種を指定して取得する。
実際のBASIC開始・終了位置はROMの管理ポインターにも依存する。
PC-1245作例の0xC500や0xC600制限を、PC-1251の安全な配置の根拠にしない。

## LCD

Core定義では24文字×1行。実装は120列×7ドットを保持する。
今回の知識APIは文字寸法と既存machine specの情報を返す。
PC-1251のdirectDrawingはnullで、PC-1245の80列アドレス式を流用しない。
停止中LCDの`pgp_get_display`はPC-1251にも対応し、120列×7行のピクセルとPNGを返す。
メモリ直接描画の仕様公開と、Sessionの表示取得は別の機能。

## 実行・生成の範囲

PC-1251では知識検索・本文取得と共通assembler／SC61860命令メタデータを提供する。
Project／Source新規作成、上限付きRUN／CALL・キー入力、LCD取得に対応する。
操作にはStudioでの承認と最新Session／Project revisionが必要。CALL先は現在の
RAM構成で書込み可能かを提案時・承認時に確認し、ミラー先を入口に指定することは拒否する。
解析用ファイル公開は未対応。同梱作例はpc1251-numeric-labelsを提供する。
Studioの手動操作に機能があることと、MCPから同じ操作を行えることは区別する。

共通命令情報にも完全な命令動作・フラグ・cycle数は含めない。
2026-11-02に1つのローカルROMで新規生成・Build・Load・RUN・数値入力・LCDを観測した。
CALLはWatchでSTD直後とRTNを観測したが、別のCALL要求では入力完了後の値を確認できなかった。
入力完了だけで実行成功を判定せず、表示・停止・Memoryを照合する。
任意ROM ABI、複数ROMでの再現性、実機表示、他AIでの全工程は未検証。
