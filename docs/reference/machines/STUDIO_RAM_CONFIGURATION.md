# StudioのRAM構成：実機準拠／制限解除

2026-11-02。開発版の仕様。公開済みStudio 1.1.0の「拡張RAM」とは異なる。

## 対象と設定

対象はPC-1250／1251／1255、PC-1260／1261、PC-1401／1402。
シリーズ内はモデル別の標準RAM構成を持ち、ROMの種類・版とRAM構成は分離して扱う。
使用ROMの対応・機種ID照合は維持し、ROMが無条件に互換であるとはみなさない。

Studioの「エミュレーター」メニューで「RAM構成：実機準拠」「RAM構成：制限解除」を選ぶ。
既定値は実機準拠。選択はシリーズ単位で保存し、同系列モデル間で共有する。
構成切替時はROMからSessionを再生成する。RAM・実行位置は引き継がないため、先に保存する。
実行中なら再起動後も実行し、停止中なら停止状態を維持する。

## 制限解除時のアドレス空間

| シリーズ | 独立RAM（両端を含む） | 容量 | RAMミラー |
| --- | --- | --- | --- |
| 1251系 | 8000–CFFF | 20 KiB | D000–D7FF → C000–C7FFのみ維持 |
| 1261系 | 4000–7FFF | 16 KiB | なし |
| 1401系 | 2000–5FFF | 16 KiB | なし |

1251系の8000–9FFF、9000／9800、B000等に対する容量依存のミラーは解除する。
8000–CFFFの各byteは独立して保持される。実機準拠では既存のモデル別ミラーを維持する。
LCD・ROM alias・制御領域・CPU内部RAMは変更しない。境界・RAMミラーも含めて
バスとMachineMemoryLayoutを一致させ、Debugger、DMP、Project配置検査、MCPで共用する。

## BASICの扱い

追加領域はマシン語・データを配置可能なRAMとして提供する。
ROMを書き換えず、BASIC開始位置・容量・MEM表示をPGPから強制設定しない。
ROM自身が追加RAMを検出して異なるBASIC開始ポインタを選ぶ場合も、ROMの結果に従う。

1251系の実ROMテストで、制限解除時に従来のA000より低い位置を選ぶケースを確認した。
BASIC入出力・Projectのロード計画は、構成に対応する有効なRAM上のROMポインタを
受け入れ、読み込む際も開始ポインタを勝手に移動しない。管理ポインタを保護する
既存の上限（C6E1／66E0／46E0）は維持し、追加RAMの全量をBASICへ割り当てない。
Build時の仮配置には従来のモデル別既定開始位置を使い、Load時には実ポインタで再検査する。

## Playerと対象外機種

この設定はStudio専用。Android／iOS Playerの生成処理はRamMode.HARDWAREを明示し、
モデル別の実機構成に固定する。Playerの画面・設定・操作は変更しない。
カード方式などその他機種ではUNRESTRICTEDを渡しても従来のRAM構成を維持する。
RAMカードの管理は別途設計する。

## 設定・APIの移行

CoreのRAMモードはcom.digihori.pgp.core.api.RamMode（HARDWARE／UNRESTRICTED）へ統一。
EmulatorConfigurationの既定もHARDWAREとし、ramModeまたは系列別ramModesで指定できる。
MachineMemoryLayout.ramModeが実際の構成を表す。旧Pc1251FamilyMemoryModeと
pc1251FamilyMemoryMode／pc1251MemoryModeの呼出しは新APIへ更新する。

Studio設定はramMode.PC_1251／PC_1261／PC_1401へ保存する。
1251系の新キーが未保存なら旧キーpc1251FamilyMemoryModeを読む。
旧EXPANDED選択はUNRESTRICTEDへ移行するが、旧10 KiBと新20 KiBは同じ構成ではない。
旧値は削除せず、新しい設定保存が失敗した場合は前の選択を保持する。

Debugger設定は実際のRAMモードを対象照合へ含める。旧1251系EXPANDEDの設定は
新UNRESTRICTEDへ自動適用せず、構成不一致として拒否する。
旧リリースで他系列の実機構成に付いていたEXPANDEDラベルはHARDWAREとして読み込む。
MCPのrevisionにも構成を含め、構成切替前のBuild／要求をそのまま利用しない。
機種仕様のmemoryLayout.ramRangesは実際のRAM区間と物理開始位置・ミラー有無を示す。
Debuggerのメモリマップも実行中のSessionのRAM構成を参照する。未起動時は選択機種の
保存済み構成を使う。3系列のRAM範囲とRAMミラーを実際のバス配置から生成し、
制限解除した1251系では8000–CFFFのRAMとD000–D7FFのミラーを別行で表示する。

## 検証範囲

合成ROMで全7モデルの全追加RAM範囲、ページ間の独立性、境界、cold reset、ROM不変、
D000ミラー、対象外機種の不変、ROMが設定したBASICポインタの保持を検証する。
系列別設定の復元・旧設定移行・保存失敗、Project配置のモード差、MCPの実範囲、
Debugger旧設定の識別も検証する。実機・全ROM版の互換性やMEM値の保証とは区別する。

## 2026-11-02の検証結果

JDK 21で以下が成功。

```sh
./gradlew --gradle-user-home .gradle-local build :core:desktopTest :desktopApp:verifyDistributionRuntime --max-workers=2 -Dorg.gradle.jvmargs=-Xmx4g
```

- Core desktop：526件、失敗・エラー0。利用可能なローカル1251 ROMの回帰テストも成功。
- Studio：482件、失敗・エラー0、既存skip 3件。
- Player共有desktop：127件、失敗・エラー0。
- 全体build・Androidチェック・共有Native生成・配布runtime検査成功。
- git diff --check成功。

macOSの確認用配布アプリで、PC-1251／1255・1261・1401を使用し、
3系列のRAMメニュー、実機準拠から制限解除への再起動、実機準拠への復帰、
1251系のモデル間共有、他系列から戻った際の選択保持を確認した。
1401では停止中に構成を変更しても停止中・0サイクルの新Sessionとなることを確認。

検証用のPreferencesFactoryをOSの一時領域へ用意し、設定はメモリ内だけで保持した。
通常のユーザー設定への保存は行わず、検証用アプリは終了済み。
永続設定の再起動復元・旧設定移行・保存失敗は隔離したPreferencesの自動テストで検証。
Windows／Linuxの実画面、全ROM版の初期化・MEM値、利用者による受入れは未実施。
確認用アプリはdesktopApp/build/compose/binaries/main/app/PokecomGOStudio.app。
版数・タグ・配布先の変更は行っていない。

Debuggerメモリマップの追加検証：全7モデル×両モードで全16bitアドレスのRAM分類・
ミラー分類とバス配置の一致、および1251系制限解除時の表示境界を自動テストで確認。
