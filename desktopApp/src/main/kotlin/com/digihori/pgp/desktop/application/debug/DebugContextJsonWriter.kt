package com.digihori.pgp.desktop.application.debug

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object DebugContextJsonWriter {
    private val json = Json { prettyPrint = true }

    fun write(context: DebugContextV1): ByteArray {
        val root = buildJsonObject {
            put("format", DebugContextV1.FORMAT)
            put("schemaVersion", DebugContextV1.SCHEMA_VERSION)
            put("capturedAt", context.capturedAt)
            put("session", buildJsonObject {
                put("id", context.session.id)
                put("revision", context.session.revision)
                put("machineId", context.session.machineId)
                put("runState", context.session.runState)
                putNullable("executedCycles", context.session.executedCycles)
            })
            context.stop?.let { put("stop", it.toJson()) } ?: put("stop", JsonNull)
            put("cpu", context.cpu.toJson())
            put("rom", buildJsonObject {
                putNullable("selectedBank", context.rom.selectedBank)
                putNullable("currentLocation", context.rom.currentLocation?.toJson())
            })
            put("memory", buildJsonArray { context.memory.forEach { add(it.toJson()) } })
            put("disassembly", buildJsonArray { context.disassembly.forEach { add(it.toJson()) } })
            put("memoryAccessHistory", buildJsonObject {
                put("enabled", context.memoryAccessHistory.enabled)
                put("truncated", context.memoryAccessHistory.truncated)
                put("entries", buildJsonArray {
                    context.memoryAccessHistory.entries.forEach { add(it.toJson()) }
                })
            })
            put("bankHistory", buildJsonObject {
                put("supported", context.bankHistory.supported)
                put("truncated", context.bankHistory.truncated)
                put("entries", buildJsonArray {
                    context.bankHistory.entries.forEach { entry ->
                        add(buildJsonObject {
                            put("sequence", entry.sequence)
                            put("previousBank", entry.previousBank)
                            put("selectedBank", entry.selectedBank)
                            put("selectorValue", entry.selectorValue)
                        })
                    }
                })
            })
            put("trace", buildJsonObject {
                put("enabled", context.trace.enabled)
                put("capacity", context.trace.capacity)
                put("truncated", context.trace.truncated)
                put("entries", buildJsonArray { context.trace.entries.forEach { add(it.toJson()) } })
            })
            put("warnings", buildJsonArray { context.warnings.forEach(::add) })
        }
        return (json.encodeToString(root) + "\n").encodeToByteArray()
    }

    private fun DebugCpuContext.toJson() = buildJsonObject {
        put("programCounter", programCounter); put("currentProgramCounter", currentProgramCounter)
        put("opcode", opcode); put("dataPointer", dataPointer)
        put("p", p); put("q", q); put("r", r); put("d", d); put("alu", alu)
        put("carry", carry); put("zero", zero); put("xInput", xInput); put("powerOn", powerOn)
        put("ia", ia); put("ib", ib); put("fo", fo); put("control", control); put("testPort", testPort)
        put("internalRam", buildJsonObject {
            put("encoding", "hex"); put("byteLength", internalRamByteLength); put("data", internalRamHex)
        })
    }

    private fun DebugStopContext.toJson() = buildJsonObject {
        put("kind", kind)
        when (val stop = this@toJson) {
            DebugStopContext.Reset, DebugStopContext.UserPause, DebugStopContext.StepComplete -> Unit
            is DebugStopContext.Breakpoint -> put("address", stop.address)
            is DebugStopContext.RunToAddress -> put("address", stop.address)
            is DebugStopContext.MemoryChanged -> {
                put("instructionAddress", stop.instructionAddress)
                put("changes", buildJsonArray {
                    stop.changes.forEach { change ->
                        add(buildJsonObject {
                            put("address", change.address); put("before", change.before); put("after", change.after)
                        })
                    }
                })
            }
            is DebugStopContext.MemoryAccessed -> {
                put("instructionAddress", stop.instructionAddress)
                put("accesses", buildJsonArray { stop.accesses.forEach { add(it.toJson()) } })
            }
            is DebugStopContext.Fault -> {
                put("faultKind", stop.faultKind)
                putNullable("address", stop.address)
                putNullable("opcode", stop.opcode)
            }
        }
    }

    private fun DebugMemoryContext.toJson() = buildJsonObject {
        put("startAddress", startAddress); put("byteLength", byteLength)
        putNullable("regionId", regionId); put("encoding", encoding); put("data", data)
    }

    private fun DebugDisassemblyContext.toJson() = buildJsonObject {
        put("address", address)
        put("bytes", buildJsonArray { bytes.forEach(::add) })
        putNullable("mnemonic", mnemonic); putNullable("operandValue", operandValue)
        putNullable("targetAddress", targetAddress); put("text", text)
        putNullable("romLocation", romLocation?.toJson())
    }

    private fun DebugMemoryAccessContext.toJson() = buildJsonObject {
        putNullable("sequence", sequence); putNullable("instructionAddress", instructionAddress)
        put("kind", kind.name.lowercase()); put("address", address); put("value", value)
    }

    private fun DebugTraceEntryContext.toJson() = buildJsonObject {
        put("sequence", sequence); put("address", address)
        put("bytes", buildJsonArray { bytes.forEach(::add) }); put("text", text)
        putNullable("romLocation", romLocation?.toJson())
        put("dataPointer", dataPointer); put("p", p); put("q", q); put("r", r); put("d", d)
        put("carry", carry); put("zero", zero)
    }

    private fun DebugRomLocationContext.toJson() = buildJsonObject {
        put("component", component); putNullable("bank", bank); put("offset", offset)
    }

    private fun JsonObjectBuilder.putNullable(name: String, value: Int?) {
        if (value == null) put(name, JsonNull) else put(name, value)
    }

    private fun JsonObjectBuilder.putNullable(name: String, value: Long?) {
        if (value == null) put(name, JsonNull) else put(name, value)
    }

    private fun JsonObjectBuilder.putNullable(name: String, value: String?) {
        if (value == null) put(name, JsonNull) else put(name, value)
    }

    private fun JsonObjectBuilder.putNullable(name: String, value: kotlinx.serialization.json.JsonElement?) {
        put(name, value ?: JsonNull)
    }
}
