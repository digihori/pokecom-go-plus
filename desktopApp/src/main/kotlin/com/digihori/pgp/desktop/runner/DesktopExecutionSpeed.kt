package com.digihori.pgp.desktop.runner

import com.digihori.pgp.core.runtime.SpeedRatio

/** Provisional calibration steps around the machine-specific hardware baseline. */
internal enum class DesktopExecutionSpeed(val label: String, val ratio: SpeedRatio) {
    LEVEL_1("1 — 80%", SpeedRatio(4, 5)),
    LEVEL_2("2 — 90%", SpeedRatio(9, 10)),
    LEVEL_3("3 — 100% (Standard)", SpeedRatio.NORMAL),
    LEVEL_4("4 — 110%", SpeedRatio(11, 10)),
    LEVEL_5("5 — 120%", SpeedRatio(6, 5)),
    UNLIMITED("Unlimited (no wait)", SpeedRatio.NORMAL),
}
