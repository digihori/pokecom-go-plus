# iOS LCDモード位置と右上トグルの修正

文書区分：調査・計画・実装／検証の記録（2026-10-22整理）。
過去の時制・件数・未確認事項を保持しています。現在の操作は[導入ガイド](../guides/GETTING_STARTED.md)、
[Androidヘルプ](../guides/PLAYER_HELP.md)、[iPhoneヘルプ](../guides/PLAYER_HELP_IOS.md)または対応する機能ガイドを参照してください。

2026-10-21。PC-1261系追加後のユーザー指定による表示・操作の修正。

- Apple facadeのskinへ、Androidと同じ共有`withFixedModeIndicators()`を適用する。
  PC-1245はRUN・PRO、PC-1250／1251／1255はRUN・PRO・RSVを固定位置に並べる。
  PC-1260／1261の既存固定配置は維持する。選択中だけ点灯し、非選択ラベルは消灯状態で残す。
- 右上のボタンはRUN→PRO→RUNのトグルとする。RSVを循環先に含めない。
  RSVは動作モードメニューから選択する。RSV中のボタン表示はRUNで、押すとPROへ切り替える。
  実際のRSV状態はLCDの固定シンボルへ表示する。
- Coreや元のSkin Catalogは変更せず、iOSのhost表示へ既存の共有処理を接続する。
  既存ROM、モデル選択、メニューの対応モード、ROM/プログラム入出力は維持する。

検証：全体`build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。
578 tasks中19実行、559 UP-TO-DATE。Swift単体40件成功（実音声デバイス依存1件を除外）。
追加ケースで全6モデルの固定位置・非重複・選択モードだけの点灯、右上のRUN/PRO往復、
RSVのメニュー選択とそこからPROへのトグルを確認した。

最初のNative実行では、既存probeテストの「全status領域が同時点灯」という期待値が失敗した。
非選択モードが消灯する仕様に合わせ、従来の状態表示は全点灯、PROは点灯、RUNは消灯と検証し直し、
全体再実行で成功した。

最終検証：共有Player iOS Native139件、Swift単体40件、対象UI3件（PC-1245／1251系／1261系）がすべて成功。
UIでも右上がRUN/PROのみで往復し、メニューRSV選択後に固定シンボルが点灯することを確認した。
PC-1245とPC-1250の添付画像で固定位置を目視確認した。
結果は`iosApp/build/CI-TestResults-20261021-101045.xcresult`（Git対象外）。
`git diff --check`も成功。使い捨てSimulatorで検証し、通常の保存ROM・設定は操作していない。
