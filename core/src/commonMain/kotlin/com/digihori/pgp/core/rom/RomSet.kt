package com.digihori.pgp.core.rom

public data class MachineId(public val value: String) {
    init {
        require(value.matches(ID_PATTERN)) { "Invalid machine ID: $value" }
    }
}

public data class RomComponentId(public val value: String) {
    init {
        require(value.matches(ID_PATTERN)) { "Invalid ROM component ID: $value" }
    }
}

public data class RomRole(public val value: String) {
    init {
        require(value.matches(ID_PATTERN)) { "Invalid ROM role: $value" }
    }

    public companion object {
        public val INTERNAL: RomRole = RomRole("internal")
        public val EXTERNAL: RomRole = RomRole("external")
        public val KANJI: RomRole = RomRole("kanji")
    }
}

public class RomComponent(
    public val id: RomComponentId,
    public val role: RomRole,
    bytes: ByteArray,
) {
    private val content: ByteArray = bytes.copyOf()

    public val size: Int
        get() = content.size

    /** Returns a copy so callers cannot mutate the component held by this model. */
    public fun copyBytes(): ByteArray = content.copyOf()
}

public class RomSet(
    public val machineId: MachineId,
    components: List<RomComponent>,
) {
    public val components: List<RomComponent> = components.toList()

    init {
        require(this.components.isNotEmpty()) { "A ROM set must contain at least one component" }
        val ids = this.components.map { it.id }
        require(ids.size == ids.distinct().size) { "ROM component IDs must be unique" }
    }

    public fun component(id: RomComponentId): RomComponent? = components.firstOrNull { it.id == id }
}

private val ID_PATTERN: Regex = Regex("[a-z0-9][a-z0-9._-]*")
