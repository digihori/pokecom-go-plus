# WAV Tool S1／S2対応の進捗

文書区分：調査・計画・実装／検証の記録（2026-10-22整理）。
過去の時制・件数・未確認事項を保持しています。現在の操作は[導入ガイド](../guides/GETTING_STARTED.md)、
[Androidヘルプ](../guides/PLAYER_HELP.md)、[iPhoneヘルプ](../guides/PLAYER_HELP_IOS.md)または対応する機能ガイドを参照してください。

更新日: 2026-10-21

対象はStudio WAV Toolのs1bin／s1basic／s2basicのEncode・Decode、波形・raw・結果保存への接続。
既存のOLD対応を維持し、以下の6段階と追加のUI・文字変換調整を実施した。
Player、音声デバイスI/O、ROM CSAVE/CLOAD連動、Decode結果の直接ロードは今回の対象外。

| 段階 | 作業 | 状況 |
| --- | --- | --- |
| 1 | 仕様・検証データ整理 | 完了。下記参照 |
| 2 | S1／S2共通信号のEncode／Decode | 完了。下記参照 |
| 3 | s1binとStudio接続 | 実装・合成検証完了。実機往復は未確認 |
| 4 | s1basicと機種別body adapter | 実装・合成検証完了。実機往復は未確認 |
| 5 | s2basic・行参照・日本語・type | 実装・合成検証完了。実機往復は未確認 |
| 6 | UI・診断・手順書・OLD回帰の仕上げ | 実装・自動検証完了。実機確認は別表で継続 |

## 2026-10-22：S1 Binaryの最終full chunkを実機規則へ訂正

pcwav 0.5.2の追加実機検証に追従し、Studio/CoreのEncode・Decodeを修正した。
最後の1〜120 bytesに個別chunk SUMを付けず、最後のchunk全体＋最初のFFでtail SUMを計算する。
120／240 bytesの固定raw／PCM期待値を訂正し、旧形式8件は拒否する。
65536 bytesの長さ・アドレス検証、破損・切詰め診断とBASIC処理を維持する。
28件の独立生成raw／PCM期待値がpcwav 0.5.2と一致。Studio経路はPC-1350／1360／1360Kの
120／240／241 bytesで確認する。以下は段階ごとの履歴で、当時の120倍数の想定は本訂正を優先する。
実機互換性の根拠は上流のPC-1360K確認記録であり、PGP修正版の新たな実機往復確認とは区別する。

## 今回分の完了（2026-10-21）

ユーザーから一通りの確認完了との報告を受領し、WAV関連の今回分を一区切りとした。
Encodeの形式・種別指定、Projectからの初期値、Decode自動判別、S1カナ／S2日本語、
長いBASICファイル名の7文字への短縮とレポート表示を今回の到達点とする。
[文字変換確認用BASIC](../../test-data/wav/samples/README.md)も提供した。

自動検証は`build :core:desktopTest`が成功し、固定vectorとCP932表の再生成検証も成功した。
このユーザー確認から実機往復の機種・条件・byte比較結果までは確定しないため、
[実機確認表](../guides/WAV_TOOL_GUIDE.md#実機往復の確認記録)は個別の記録が得られるまで未確認のままとする。
実録音の速度ずれ・ノイズ対応、44100／48000HzのS1／S2 Decode、直接ロードなどは後続課題。
以下は各段階での記録であり、現在の操作仕様は[操作ガイド](../guides/WAV_TOOL_GUIDE.md)を参照する。

## 段階1：仕様・検証データ整理

- [参照動作仕様](../reference/WAV_S1_S2_FORMAT.md)とpcwav固定commitを照合した。
- S1 Binary末尾を、実機録音で確認したpartial raw＋最初のFFに修正した。
  pcwavの旧FFのみの方式と区別し、黙って互換受入れする設計にしない。
- Binaryは最後が120 bytesでもchunk checksum付き、BASICは最後の120 bytesをtailへ含める、と明示した。
- BASICのcarry補正位置、type 37がpassword文字列設定を意味しないこと、
  65536-byte長が参照Decoderで0になる問題を記録した。
- [28件の固定合成vector](../../test-data/wav/s1-s2/README.md)を追加した。
  採用raw／参照raw、leader込みPCMのsample数・SHA-256を分け、実装前の独立した期待値とした。
- 失敗系、最大長、範囲、別sample rate等の後続テスト条件を仕様書に記載した。

固定vectorはPython標準ライブラリだけで検証できる。
参照値の再採取時だけpcwav／Perlを使い、製品・通常テストの依存にはしない。
ROM・実機録音は追加せず、外部リポジトリも変更しない。

## 段階2：S1／S2共通信号

- Coreへ`S1SignalEncoder`／`S1SignalDecoder`を追加。S1とS2で共用する。
- 8000Hz、4.096秒leader、16-unit byte framingを生成し、rawとPCMの位置対応を保持する。
- Decodeでは固定時間の読み飛ばしをせず、搬送波から同期する。
  LOW／HIGH nibbleの開始位置、復元済みbyte、切詰め・同期喪失の診断を返す。
- 同期・停止ビットと各unitの搬送波を検査する。破損後は新しいleaderまで待ち、途中byteを出力しない。
- Encodeは8000Hz、Decodeは8000／16000／24000／32000Hzを対象とする。
  非整数倍のsample rateは推測して読まず`UNSUPPORTED_FORMAT`を返す。
  リサンプリング、録音の速度ずれ・ノイズへの対応は未実装。
- 先頭の同期確立／破損後の再同期には125ms以上の連続搬送波を要求する。
  leader全長の厳密な検証やpayloadの識別・checksum検証は行わない。
- BASICの物理trailer FFも呼出側がrawへ明示する。信号層から追加・削除しない。
- 既存のPCM WAV読書き・正規化に接続可能。OLDの処理は変更していない。

## 段階3：s1binとStudio接続

- `S1BinaryCodec`でtype 67、name／metadata、120-byte chunk、FF FF tailをEncode・Decodeする。
- partial raw＋最初のFFによるtailを採用し、旧pcwav tailへの無条件fallbackは行わない。
- 1〜65536 bytesと16-bit address範囲を扱い、最後のfull chunkにもchecksumを付ける。
- name／metadata／chunk／tailのchecksum、予約領域・filename、宣言長、終端、余剰rawを検証する。
  構造・checksumに問題がある場合はプログラム出力不可。位置と期待値／実際値を診断へ残す。
- StudioでS1／S2 binaryを有効化。S2 Binaryも同じtype 67を共用する。
  段階3時点ではS1／S2 BASIC作成は無効だった。段階4でS1 BASICへ対応（下記）。
- 保存済み`.dmp`またはIntel HEX（`.hex`／`.ihx`）からWAVを作成できる。
  Intel HEXはrecord 00／01、baseが0の02／04を扱う。非0のbaseやその他のrecordは明示的に拒否する。
  checksum、EOF、16-bit範囲、重複、gapを検証し、暗黙の結合・埋め込みはしない。
- Decode成功時は`.dmp`とIntel HEXを別々に保存できる。Encode結果はWAV保存とpreviewのみ。
- 波形・raw・同期位置・診断を既存の解析画面へ接続。payload失敗時もPCM/rawは保持する。
  信号破損の警告が残る場合もプログラム保存は出さない。
- 固定Binary 8件と一致し、旧tailとの差がある6件を拒否。120-byte前後、最大65536 bytes、
  address overflow、全切詰め位置、各checksum反転、終端・余剰、Studio往復・HEX入出力を合成テストで確認。

実機確認済みなのは採用tail仕様の根拠となる既存録音であり、新しいPGP Encoderの実機ロード・
セーブ録音の往復は未確認。今後の手順：小さい既知の`.dmp`をS1でWAV化→実機へCLOAD→
内容照合→実機から録音→S1でDecode→`.dmp`の開始address・全byteを比較する。
S2実機も同じ手順で別途確認する。

## 段階4：s1basicと機種別body adapter（2026-10-21）

- Coreの`S1BasicCodec`へtype 07、逆順7文字filename、120-byte chunk、FF FF tailを実装した。
  filenameは印字可能ASCIIの7文字以内（大文字化や切捨てをしない）。空のBASIC bodyは拒否する。
- BASICの最後のchunkは120 bytesでもchunk checksumを付けず、全体をtail checksumへ含める。
  header／中間chunk／tailのchecksumを検査し、位置・期待値／実際値を返す。
- 行番号1〜65279、行長、末尾0Dを検証し、行長から復元する。
  行内にあるFF FFを転送終端と誤認しない。body上限は65536 bytes。
- `FF + line records + FF`と転送bodyとのadapterを追加。BASIC物理trailer FFはEncode時に
  payloadの後へ1 byte付ける。Decodeはpayloadのみ／trailer 1 byte付きの双方を受け入れ、余剰を拒否する。
- 既存`BasicProgramCodec`を機種指定で再利用。PC-1350／1260／1261／1401／1402／1450／1460の
  機種別語彙を一律の表へ置換しない。半角カナの可否も既存の機種別codecに従う。
- StudioでS1 BASICの作成・Decode・`.bas`保存を有効化。S1対象機種を選択でき、Project機種を初期値として保持する。
  段階4時点ではS2 BASICは未対応だった。段階5で対応（下記）。S1／S2 BinaryとOLDは維持する。
- textを同じ機種で再コンパイルし、元の全byteと一致する場合だけプログラム保存を許す。
  未知byte・変換による欠落や、信号破損時は内容preview、payload、PCM/raw、診断を残す。
- 固定S1 BASIC 8件（119／120／121／239／240／241 bytes、quote／REM、カナ）とのraw一致を確認。
  境界・checksum破損・全切詰め位置・行構造・物理trailer・adapter・行内FF FFを合成テストで検証した。
- Studioで上記7機種のsource→WAV→sourceを再コンパイルし、元のBASIC imageとのbyte一致を確認した。

新しいS1 BASIC codecの実機往復は未確認。対象機種を明示して短い`.bas`をEncode→実機でCLOAD→
内容を確認→実機から録音→同じ機種でDecodeし、BASIC imageのbyte一致を確認する手順を残す。

## 段階5：s2basic・行参照・日本語・type（2026-10-21）

- `S2BasicCodec`でtype 27／37を識別・保持する。S1の検証済みBASIC転送構造を共用し、
  name、120-byte chunk、最後のfull chunkを含むtail、物理trailer、行長・行末を検証する。
- type 27をdefaultとし、Studioではtype 37をcheckboxで選べる。両者の差はtype byteのみ。
  パスワード文字列を設定する機能や領域は追加しない。
- PC-1360／1360KとPC-1470Uの機種指定を保持し、既存の個別語彙表を使う。
  共通命令でも語彙表を統合しない（例：1360のGCURSOR tokenは1470Uでは未定義）。
- `GOTO`／`GOSUB`／`THEN`の単一行参照と、`ON ... GOTO/GOSUB`のcomma区切りリストを
  `1F hi lo`へ変換する。範囲0〜65535を検証し、桁あふれを黙って切り捨てない。
  quote／REM内の数字・キーワードはtextとして保持する。
- 半角カナとCP932の2-byte日本語へ対応。CoreにOS charset APIを持ち込まず、
  Python標準ライブラリのcp932から生成した11280 pairの固定表を利用する。
  `python3 scripts/generate-s2-cp932.py --verify`で再現性を確認できる。
  波ダッシュ等の6互換文字はCP932の正規化に従う。表現不能なUnicodeはエラーにする。
- text出力は同じ機種へ再コンパイルし全byte一致を確認する。未定義tokenや文字変換の損失、
  信号破損、転送checksum不一致ではプログラム保存を出さず、取得済みPCM/raw・位置・診断を残す。
- S2固定vector 11件で、ソース→機種別body→payloadの独立期待値一致を確認。
  両typeの境界・切詰め・各checksum、CP932割当済みpairとJVM windows-31jの比較、
  3機種×2typeのStudio往復、日本語・行参照、1470U語彙、未知token・範囲外・破損を検証する。

新しいS2 codecの実機往復は未確認。PC-1360系とPC-1470Uは別々に確認する。
CP932のファイル変換対応は、各実機がすべての漢字や拡張文字を表示・実行できることの保証ではない。
`.bas`保存は転送type／filename等のメタデータを保持しないため、type保持が必要な場合はrawや元WAVも保存する。

## 段階6：UI・診断・手順書・OLD回帰（2026-10-21）

- [操作ガイド](../guides/WAV_TOOL_GUIDE.md)に全形式の対応表、Encode／Decode／保存、形式・機種・type指定、
  エラー時の確認、sample rate制約、実機検証の記録表をまとめた。
- OLD専用の差分しきい値をS1／S2の判定として表示しない。S1／S2はPCMと実際のLOW/HIGH同期位置を表示する。
- OLDを含む全形式で、信号破損警告やエラーが残る場合はプログラム保存を出さない。
  payloadの検証済みpreview、PCM/raw、診断は残す。
- EOFや空の診断範囲を選んでもraw・PCMの表示範囲内へ移動する。
- 全形式の破損末尾と保存抑止、raw EOF移動の回帰テストを追加し、既存のOLDと全形式の検証を実行する。

ソフトウェアの実装・自動検証と、新codecの実機往復確認は分ける。
実機接続・録音の確認はこの環境で実施しておらず、[ガイドの未確認表](../guides/WAV_TOOL_GUIDE.md#実機往復の確認記録)で継続する。

## 検証記録

- `generate.py --pcwav ../pcwav`で指定commitの参照raw・PCMと独立期待値を比較した。
- `generate.py --verify`で28件の整合性を確認した。
  partialがあるBinaryの6件で旧tailとの差を検出し、それ以外のraw・PCMは参照値と一致した。
- 既存の1360K配布デコーダーの合成テスト6件が成功。
- これらは仕様・fixtureの検証であり、製品codecや全長・全機種の実機確認ではない。

JDK 21で`build :core:desktopTest`成功（578 tasks、15実行、563 up-to-date）。
`git diff --check`と追加文書のローカルリンク確認も成功。新しい製品codecのテスト実施を意味しない。

段階2の追加テストでは、固定vector全28件のPCM sample数・SHA-256一致とraw復元、
全256 byteの位置情報、8／16／24／32kHz、先頭・末尾無音、振幅低下・極性反転、
切詰め、同期・停止ビット破損、搬送波欠落、新leaderでの復帰、既存WAVコンテナとの接続を確認する。
合成信号による確認であり、新codecの実機録音・実機往復は未確認。

段階2の検証結果：JDK 21で`build :core:desktopTest`成功（578 tasks、174実行、404 up-to-date）。
Core Desktop 471件、Core iOS Simulator 418件が成功。固定vector 28件の整合性再検査と
`git diff --check`も成功。

段階3の検証結果：JDK 21で`build :core:desktopTest`成功（最終578 tasks、19実行、559 up-to-date）。
Core Desktop 478件、Core iOS Simulator 424件、Studio 329件（1 skip）、失敗0。
日本語・英語の800px／420px表示を画像化して確認した。

段階4の検証結果：JDK 21で`build :core:desktopTest`成功（最終578 tasks、15実行、563 up-to-date）。
Core Desktop 484件、Core iOS Simulator 429件、Studio 332件（1 skip）、失敗0。
固定vector 28件の整合性、`git diff --check`、日本語／英語の800px／420px表示を確認。
`desktopApp:run`でJVMプロセスの起動も確認した。

段階5の検証結果：JDK 21で`build :core:desktopTest`成功（最終578 tasks、17実行、561 up-to-date）。
Core Desktop 490件、Core iOS Simulator 433件、Studio 335件（1 skip）、失敗0。
28件の固定vectorと11280 pairのCP932表を再検証し、`git diff --check`も成功。
日本語／英語の800px／420px表示とDesktop JVM起動を確認した。

段階6の検証結果：JDK 21で`build :core:desktopTest`成功（最終578 tasks、15実行、563 up-to-date）。
Core Desktop 490件、Core iOS Simulator 433件、Studio 337件（1 skip）、失敗0。
28固定vector・CP932生成表・`git diff --check`の再検証、日英のOLD／S1波形表示（900px／420px）、
Desktop JVM起動を確認。新codecの実機往復・録音許容範囲は未確認表に残した。

2026-10-21 UI見直し：冒頭のtype一覧・mode/互換性行・type checkboxを削除。
世代とBASICパスワードなし／あり・マシン語を利用者が明示選択する。S2のマシン語は選択肢に出さない。
S1保護付き（17）の生成・識別・保持を追加。入力拡張子は選択形式との適合確認にのみ使用する。

形式選択UIの日英・通常幅／狭い幅を画像で確認し、S1保護付きの生成・Decode・flag保持のテストが成功。
Desktopアプリの起動も確認した。

2026-10-21追加：対象機種の選択UIを削除。Projectツリーからは機種とファイル拡張子で初期選択する。
PC-1251の.dmpはOLD・マシン語、.basはOLD・BASICパスワードなし。利用者による変更は引き続き可能。

2026-10-21追加：S1/S2 BASICの長い転送名は先頭7文字へ短縮し、元の名前と短縮後の名前をレポートへ記録する。
文字種の制約は維持し、長さによる変換エラーは出さない。

2026-10-21方針変更：WAVツールは形式単位の上位仕様でBASICを変換し、個別機種のカナ・語彙制限を適用しない。
Project情報はEncodeの初期選択にだけ使用する。DecodeはOLD／S1共通信号とヘッダー・checksum等から
自動判別し、形式指定UIを廃止。Binary 67はS1/S2共通と表示する。未確定・競合時は候補と途中結果を保持する。
エミュレータ本体の機種別BASIC仕様は変更しない。

自動判別対応の検証：JDK 21の全ビルド・Core Desktop 490件・Core iOS Simulator 433件・Studio 343件（1 skip）が成功。
全通常／保護付き形式、共通Binary、自動判別競合、S1カナの機種非依存変換を確認。
日英のDecode画面・Desktop起動・`git diff --check`も確認した。

## 2026-10-31：高sample rate入力とchecksum点検

OLD／S1／S2共通で44100／48000Hz入力を追加。Encodeは8000Hzを維持する。
過去の「44100／48000Hz未対応」は当時の記録。現行仕様と今回の境界・破損検証は
[WAV入力・checksum検証記録](WAV_INPUT_CHECKSUM_AUDIT.md)と[操作ガイド](../guides/WAV_TOOL_GUIDE.md)を参照。
速度ずれ補正の追加は今回の範囲外とする。

## 2026-10-31：実機往復6ケースのユーザー受入れ

PC-1245（OLD）／PC-1401（S1）／PC-1360K（S2）のBASIC／Binaryについて、
用意した正常6ケースすべてOKとのユーザー報告を受領した。
今回分を実機受入れ完了とし、過去の「未確認」は当時の履歴として保持する。
対象・確認範囲は[実機結果記録](../../test-data/wav/hardware/RESULTS_2026-10-31.md)を参照。
保護付き形式、他機種、文字種・全長境界の網羅確認まで完了したとは扱わない。
