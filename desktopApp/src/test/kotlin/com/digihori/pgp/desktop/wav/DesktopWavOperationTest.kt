package com.digihori.pgp.desktop.wav

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.wav.*
import kotlin.test.*

class DesktopWavOperationTest {
    private val machine = MachineId("pc-1245")

    @Test fun sourceConversionAndSeparateProgramExport() {
        val encoded = DesktopWavOperation.encode("10 PRINT \"HELLO\"\n".encodeToByteArray(), false, machine, "test-name.bas", true)
        assertNull(encoded.error)
        assertNotNull(encoded.wav)
        assertNull(encoded.program)
        val decoded = DesktopWavOperation.decode(encoded.wav, machine)
        assertNotNull(decoded.program)
        assertEquals(OldPayloadType.PASSWORD_PROTECTED_BASIC, decoded.payload?.type)
        assertEquals("TESTNAM.bas", decoded.programName)
        assertTrue(decoded.content.contains("HELLO"))
    }

    @Test fun payloadFailureKeepsPcmRawAndPositionsButCannotExportProgram() {
        val payload = assertIs<CodecResult.Success<OldPayloadEncoding>>(
            OldPayloadEncoder.encodeBasic("TEST", LogicalByteSequence(byteArrayOf(0xf0.toByte()))),
        ).value.payload.copyRawBytes()
        payload[9] = (payload[9].toInt() xor 1).toByte()
        val signal = assertIs<CodecResult.Success<OldSignalEncoding>>(
            OldSignalEncoder.encode(RawTransferByteSequence(payload)),
        ).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.writeOldSignal(signal)).value.bytes
        val result = DesktopWavOperation.decode(wav, machine)
        assertNotNull(result.pcm)
        assertContentEquals(payload, result.raw?.copyBytes())
        assertTrue(result.mappings.isNotEmpty())
        assertTrue(result.syncPoints.isNotEmpty())
        assertNull(result.payload)
        assertNull(result.program)
        assertTrue(result.report().contains("expected="))
        assertTrue(result.report().contains("RAW_BYTES"))
    }

    @Test fun signalFailureKeepsContainerAndPcm() {
        val wav = assertIs<CodecResult.Success<WavEncoding>>(
            PcmWavWriter.write8BitMono(8000, PcmSampleSequence(ShortArray(1000))),
        ).value.bytes
        val result = DesktopWavOperation.decode(wav, machine)
        assertNotNull(result.pcm)
        assertNull(result.raw)
        assertNull(result.program)
        assertTrue(result.summary.contains("8000 Hz"))
        assertTrue(result.diagnostics.any { it.stage == CodecStage.SIGNAL_DECODE })
    }

    @Test fun unsupportedProjectTargetIsNotSilentlyEncodedAsOld() {
        val result = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId("pc-1350"), "TEST", false)
        assertNotNull(result.error)
        assertNull(result.wav)
    }

    @Test fun validatedTransferWithInvalidBasicKeepsLogicalData() {
        val wav = assertIs<CodecResult.Success<OldWavEncoding>>(
            OldWavCodec.encodeBasic("TEST", LogicalByteSequence(byteArrayOf(1, 2, 3, 0xf0.toByte()))),
        ).value.wav.bytes
        val result = DesktopWavOperation.decode(wav, machine)
        assertNotNull(result.payload)
        assertNotNull(result.raw)
        assertNull(result.program)
        assertNotNull(result.error)
    }
    @Test fun addressedBinaryPreservesAddressesAndRejectsGaps() {
        val dump = "C000: 01 02 03\n".encodeToByteArray()
        val encoded = DesktopWavOperation.encode(dump, true, machine, "CODE", false)
        assertNotNull(encoded.wav)
        val decoded = DesktopWavOperation.decode(encoded.wav, machine)
        assertEquals(0xc000, decoded.payload?.startAddress)
        assertEquals(0xc002, decoded.payload?.endAddress)
        assertContentEquals(byteArrayOf(1, 2, 3), decoded.payload?.body?.copyBytes())
        assertEquals("CODE.dmp", decoded.programName)
        assertNotNull(decoded.program)
        val invalid = DesktopWavOperation.encode("C000: 01\nC002: 02\n".encodeToByteArray(), true, machine, "GAP", false)
        assertNull(invalid.wav)
        assertTrue(invalid.error!!.contains("gap"))
    }

}
