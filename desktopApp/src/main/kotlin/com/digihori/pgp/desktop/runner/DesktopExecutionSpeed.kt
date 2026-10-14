package com.digihori.pgp.desktop.runner

import com.digihori.pgp.core.runtime.SpeedRatio

/** Provisional calibration steps around the machine-specific hardware baseline. */
internal enum class DesktopExecutionSpeed(val ratio: SpeedRatio) {
    LEVEL_1(SpeedRatio(4, 5)),
    LEVEL_2(SpeedRatio(9, 10)),
    LEVEL_3(SpeedRatio.NORMAL),
    LEVEL_4(SpeedRatio(11, 10)),
    LEVEL_5(SpeedRatio(6, 5)),
    UNLIMITED(SpeedRatio.NORMAL),
}
