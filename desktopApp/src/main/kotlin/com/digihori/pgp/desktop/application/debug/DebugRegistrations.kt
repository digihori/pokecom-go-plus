package com.digihori.pgp.desktop.application.debug

import com.digihori.pgp.desktop.debug.DebuggerBreakpoint
import com.digihori.pgp.desktop.debug.DebuggerWatch
import kotlinx.serialization.json.*

internal sealed interface DebugRegistrationsResult {
    data class Success(val snapshot: DebugRegistrationsSnapshot) : DebugRegistrationsResult
    data object SessionRunning : DebugRegistrationsResult
}

/** IDs are meaningful only within session.id. No memory contents or watch baselines are exported. */
internal data class DebugRegistrationsSnapshot(
    val session: DebugSessionContext,
    val breakpoints: List<DebuggerBreakpoint>,
    val watches: List<DebuggerWatch>,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("format", "pgp-debug-registrations")
        put("schemaVersion", 1)
        put("session", buildJsonObject {
            put("id", session.id); put("revision", session.revision)
            put("machineId", session.machineId); put("runState", session.runState)
        })
        put("addressScope", "logicalAllBanks")
        put("breakpoints", buildJsonArray {
            breakpoints.forEach { entry -> add(buildJsonObject {
                put("id", entry.id); put("address", entry.address); put("enabled", entry.enabled)
            }) }
        })
        put("watches", buildJsonArray {
            watches.forEach { entry -> add(buildJsonObject {
                put("id", entry.id); put("startAddress", entry.range.first)
                put("endAddressInclusive", entry.range.last); put("enabled", entry.enabled)
                put("kind", when (entry.kind) {
                    com.digihori.pgp.desktop.debug.DebuggerWatchKind.CHANGE -> "change"
                    com.digihori.pgp.desktop.debug.DebuggerWatchKind.READ -> "read"
                    com.digihori.pgp.desktop.debug.DebuggerWatchKind.WRITE -> "write"
                    com.digihori.pgp.desktop.debug.DebuggerWatchKind.READ_WRITE -> "readWrite"
                })
            }) }
        })
    }
}
