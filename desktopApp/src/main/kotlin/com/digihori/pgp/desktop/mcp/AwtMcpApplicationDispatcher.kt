package com.digihori.pgp.desktop.mcp

import java.awt.EventQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonObject

internal object AwtMcpApplicationDispatcher : McpApplicationDispatcher {
    override fun dispatch(block: () -> JsonObject): JsonObject {
        if (EventQueue.isDispatchThread()) return block()
        val task = FutureTask(block)
        EventQueue.invokeLater(task)
        return task.get(TOOL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private const val TOOL_TIMEOUT_SECONDS: Long = 5
}
