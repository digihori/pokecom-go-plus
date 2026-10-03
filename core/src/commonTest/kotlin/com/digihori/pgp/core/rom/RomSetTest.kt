package com.digihori.pgp.core.rom

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class RomSetTest {
    @Test
    fun componentOwnsItsBytesAndReturnsCopies() {
        val source = byteArrayOf(1, 2, 3)
        val component = RomComponent(RomComponentId("internal"), RomRole.INTERNAL, source)

        source[0] = 9
        val firstCopy = component.copyBytes()
        firstCopy[1] = 8

        assertContentEquals(byteArrayOf(1, 2, 3), component.copyBytes())
        assertEquals(3, component.size)
    }

    @Test
    fun findsComponentsByTypedId() {
        val component = RomComponent(RomComponentId("external"), RomRole.EXTERNAL, byteArrayOf(4))
        val romSet = RomSet(MachineId("pc-1245"), listOf(component))

        assertNotNull(romSet.component(RomComponentId("external")))
        assertEquals(null, romSet.component(RomComponentId("internal")))
    }

    @Test
    fun rejectsEmptySetsAndDuplicateIds() {
        assertFailsWith<IllegalArgumentException> { RomSet(MachineId("pc-1245"), emptyList()) }

        val first = RomComponent(RomComponentId("internal"), RomRole.INTERNAL, byteArrayOf(1))
        val duplicate = RomComponent(RomComponentId("internal"), RomRole.EXTERNAL, byteArrayOf(2))
        assertFailsWith<IllegalArgumentException> {
            RomSet(MachineId("pc-1245"), listOf(first, duplicate))
        }
    }

    @Test
    fun rejectsIdsThatAreUnsafeForManifests() {
        assertFailsWith<IllegalArgumentException> { MachineId("../pc-1245") }
        assertFailsWith<IllegalArgumentException> { RomComponentId("Internal ROM") }
        assertFailsWith<IllegalArgumentException> { RomRole("INTERNAL") }
    }
}
