package com.digihori.pgp.controller.ui

import com.digihori.pgp.input.GamepadButton

/** Host-supplied text; existing Player callers retain Japanese by default. */
data class ControllerStrings(
    val title: String = "ゲームコントローラー",
    val enable: String = "コントローラー入力を有効にする",
    val disconnected: String = "未接続",
    val selectDevice: String = "使用する1台を選択",
    val instructions: String = "図のボタンを選び、ポケコンのキーを割り当てます。設定中の操作はテスト表示のみです。",
    val positions: String = "ABXYは標準位置を表示しています。機器の印字が異なる場合は上下左右の位置で選んでください。",
    val saveHelp: String = "スティックは対象外です。変更は自動保存され、以前の設定を上書きします。",
    val assignment: (String) -> String = { "$it の割当" },
    val close: String = "閉じる",
    val unavailable: String = "利用不可",
    val unassigned: String = "未割当",
    val unassignedShort: String = unassigned,
    val buttonLabel: (GamepadButton) -> String = { it.label },
    val pressed: String = "押下中",
    val released: String = "未押下",
    val unavailableShort: String = unavailable,
)
