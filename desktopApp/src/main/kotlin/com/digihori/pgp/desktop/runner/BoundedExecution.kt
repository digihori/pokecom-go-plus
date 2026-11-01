package com.digihori.pgp.desktop.runner

internal data class BoundedExecutionResult(
    val id: String, val instructions: Long, val cycles: Long,
    val inputComplete: Boolean, val reason: String?, val pc: Int, val revision: Long,
)

internal data class BoundedExecutionPlan(
    val id: String, val maxInstructions: Long, val maxCycles: Long,
    var instructions: Long = 0, var cycles: Long = 0,
)
