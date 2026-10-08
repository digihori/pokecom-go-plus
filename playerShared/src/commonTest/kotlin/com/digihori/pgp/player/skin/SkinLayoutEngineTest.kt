package com.digihori.pgp.player.skin

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.PresentationMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SkinLayoutEngineTest {
    @Test
    fun fitsAndCentersTheFullDeviceInsideSafeInsets() {
        val placement = SkinLayoutEngine.calculate(
            definition(),
            SkinLayoutRequest(
                availableSize = SkinSize(500f, 1000f),
                safeInsets = SkinInsets(left = 10f, top = 20f, right = 10f, bottom = 20f),
                mode = PresentationMode.FULL_DEVICE,
            ),
        )

        assertEquals(0.48f, placement.scale)
        assertEquals(SkinRect(10f, 260f, 480f, 480f), placement.viewport)
    }

    @Test
    fun controllerDisplayUsesItsDedicatedCropAndRoundTripsCoordinates() {
        val placement = SkinLayoutEngine.calculate(
            definition(),
            SkinLayoutRequest(SkinSize(800f, 400f), mode = PresentationMode.CONTROLLER_DISPLAY),
        )

        assertEquals(SkinRect(100f, 100f, 800f, 200f), placement.sourceRegion)
        assertEquals(1f, placement.scale)
        assertEquals(150f, placement.screenX(250f))
        assertEquals(175f, placement.skinX(75f))
        assertEquals(150f, placement.skinY(150f))
    }

    private fun definition(): SkinDefinition = SkinDefinition(
        id = "test-skin",
        machineIds = setOf(MachineId("pc-1245")),
        designSize = SkinSize(1000f, 1000f),
        fullDeviceRegion = SkinRect(0f, 0f, 1000f, 1000f),
        playableRegion = SkinRect(0f, 200f, 1000f, 800f),
        landscapeRegion = SkinRect(0f, 100f, 1000f, 500f),
        controllerDisplayRegion = SkinRect(100f, 100f, 800f, 200f),
        lcdContentRegion = SkinRect(200f, 150f, 600f, 100f),
    )
}
