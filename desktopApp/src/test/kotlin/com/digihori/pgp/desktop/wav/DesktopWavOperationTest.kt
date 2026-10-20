package com.digihori.pgp.desktop.wav

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.wav.*
import kotlin.test.*

class DesktopWavOperationTest {
    private val machine = MachineId("pc-1245")
    private fun canonical(machine: MachineId) = MachineId(if (com.digihori.pgp.core.api.MachineCatalog.require(machine).basicDialect == com.digihori.pgp.core.api.MachineBasicDialect.S1) "pc-1350" else "pc-1360")

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
        val result = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId("unknown"), "TEST", false)
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

    @Test fun s1AndS2BinaryRoundTripsAndHexExport() {
        val body = ByteArray(241) { (it * 37 + 18).toByte() }
        val input = DesktopWavBinaryFiles.writeHex(0xc000, body)
        for (target in listOf("pc-1350", "pc-1360")) {
            val machine = MachineId(target)
            val encoded = DesktopWavOperation.encode(input, true, machine, "CODE", false)
            assertNotNull(encoded.wav)
            assertNull(encoded.program)
            val decoded = DesktopWavOperation.decode(encoded.wav, machine)
            assertEquals(0xc000, decoded.s1Payload?.startAddress)
            assertEquals(0xc0f0, decoded.s1Payload?.endAddress)
            assertContentEquals(body, decoded.s1Payload?.body?.copyBytes())
            assertEquals("CODE.dmp", decoded.programName)
            assertNotNull(decoded.program)
            assertContentEquals(input, decoded.hexProgram)
            assertNotNull(decoded.pcm)
            assertEquals(decoded.raw?.size, decoded.mappings.size)
            assertTrue(decoded.diagnostics.isEmpty())
        }
    }

    @Test fun s1CorruptPayloadRetainsRawAndWaveformButNoProgram() {
        val raw = assertIs<CodecResult.Success<S1BinaryEncoding>>(S1BinaryCodec.encode("TEST", 0xc000,
            LogicalByteSequence(byteArrayOf(1, 2, 3)))).value.rawBytes.copyBytes()
        raw[raw.lastIndex] = (raw.last().toInt() xor 1).toByte()
        val signal = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(RawTransferByteSequence(raw))).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, signal.samples)).value.bytes
        val decoded = DesktopWavOperation.decode(wav, MachineId("pc-1350"))
        assertContentEquals(raw, decoded.raw?.copyBytes())
        assertNotNull(decoded.pcm)
        assertTrue(decoded.mappings.isNotEmpty())
        assertNull(decoded.s1Payload)
        assertNull(decoded.program)
        assertNull(decoded.hexProgram)
        assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, decoded.diagnostics.first().code)
    }

    @Test fun hexImportRejectsChecksumGapsOverlapsAndMissingEof() {
        val machine = MachineId("pc-1350")
        for (source in listOf(":01C000000100\n:00000001FF", ":01C00000013E", ":01C00000013E\n:01C00200023B\n:00000001FF",
            ":01C00000013E\n:01C00000013E\n:00000001FF", ":020000040001F9\n:00000001FF")) {
            val encoded = DesktopWavOperation.encode(source.encodeToByteArray(), true, machine, "TEST", false)
            assertNull(encoded.wav)
            assertNotNull(encoded.error)
        }
    }

    @Test fun formatAvailabilitySupportsAllKnownDialects() {
        for (dialect in com.digihori.pgp.core.api.MachineBasicDialect.entries) {
            assertTrue(DesktopWavOperation.supported(dialect, decode = true, binary = false))
            assertTrue(DesktopWavOperation.supported(dialect, decode = false, binary = true))
            assertEquals(true,
                DesktopWavOperation.supported(dialect, decode = false, binary = false))
        }
        assertFalse(DesktopWavOperation.supported(null, true, true))
    }

    @Test fun s1BasicRoundTripUsesSelectedMachineAndPreservesKana() {
        for (id in listOf("pc-1350", "pc-1260", "pc-1261", "pc-1401", "pc-1402", "pc-1450", "pc-1460")) {
            val machine = MachineId(id)
            val source = if (id == "pc-1350") "10 PRINT \"ｱｲｳ\"\n20 REM PRINT THEN\n" else "10 PRINT \"HELLO\"\n20 END\n"
            val encoded = DesktopWavOperation.encode(source.encodeToByteArray(), false, machine, "Basic", false)
            assertNotNull(encoded.wav, id + encoded.report())
            assertNull(encoded.program)
            assertNotNull(encoded.s1BasicPayload)
            assertTrue(encoded.s1BasicPayload.physicalTrailer)
            val decoded = DesktopWavOperation.decode(encoded.wav, machine)
            assertNotNull(decoded.program, id + decoded.report())
            assertEquals("Basic.bas", decoded.programName)
            val expected = assertIs<com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult.Success>(
                com.digihori.pgp.desktop.basic.DesktopBasicLoader.compileProgram(source.encodeToByteArray(), canonical(machine))).bytes
            val actual = assertIs<com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult.Success>(
                com.digihori.pgp.desktop.basic.DesktopBasicLoader.compileProgram(decoded.program, canonical(machine))).bytes
            assertContentEquals(expected, actual, id)
        }
    }

    @Test fun s1BasicCorruptionKeepsPhysicalResultsAndBlocksExport() {
        val encoded = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId("pc-1350"), "TEST", false)
        val raw = assertNotNull(encoded.raw).copyBytes()
        raw[9] = (raw[9].toInt() xor 1).toByte()
        val signal = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(RawTransferByteSequence(raw))).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, signal.samples)).value.bytes
        val decoded = DesktopWavOperation.decode(wav, MachineId("pc-1350"))
        assertNotNull(decoded.pcm)
        assertContentEquals(raw, decoded.raw?.copyBytes())
        assertTrue(decoded.syncPoints.isNotEmpty())
        assertNull(decoded.program)
        assertNull(decoded.s1BasicPayload)
        assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, decoded.diagnostics.first().code)
    }

    @Test fun unknownS1TokenAndWrongDialectCannotExportBasic() {
        val payload = assertIs<CodecResult.Success<S1BasicEncoding>>(S1BasicCodec.encode("TEST",
            LogicalByteSequence(byteArrayOf(0, 10, 2, 0x80.toByte(), 13)))).value
        val signal = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(payload.rawBytes)).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, signal.samples)).value.bytes
        val unknown = DesktopWavOperation.decode(wav, MachineId("pc-1350"))
        assertNotNull(unknown.s1BasicPayload)
        assertNull(unknown.program)
        assertTrue(unknown.diagnostics.any { it.stage == CodecStage.PROGRAM_DECODE && it.code == CodecDiagnosticCode.UNDEFINED_BYTE })
        val wrongDialect = DesktopWavOperation.decode(wav, MachineId("pc-1360"))
        assertNotNull(wrongDialect.s1BasicPayload)
        assertTrue(wrongDialect.diagnostics.any { it.code == CodecDiagnosticCode.UNDEFINED_BYTE })
        assertNull(wrongDialect.program)
    }

    @Test fun s2BasicRoundTripRetainsTypeJapaneseReferencesAndMachineVocabulary() {
        for (id in listOf("pc-1360", "pc-1360k", "pc-1470u")) for (protected in listOf(false, true)) {
            val machine = MachineId(id)
            val source = "10 ON A GOTO 100,200\n100 PRINT \"漢字ｱﾊﾟ\"\n200 IF A=1 THEN 100\n300 REM PRINT 漢字\n"
            val encoded = DesktopWavOperation.encode(source.encodeToByteArray(), false, machine, "S2test", protected)
            assertNotNull(encoded.wav, id + encoded.report())
            assertNull(encoded.program)
            val expectedType = if (protected) S2BasicType.PASSWORD_PROTECTED_BASIC else S2BasicType.BASIC
            assertEquals(expectedType, encoded.s2BasicPayload?.type)
            val decoded = DesktopWavOperation.decode(encoded.wav, machine)
            assertNotNull(decoded.program, id + decoded.report())
            assertEquals(expectedType, decoded.s2BasicPayload?.type)
            assertTrue(decoded.content.contains("漢字ｱﾊﾟ"))
            val expected = assertIs<com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult.Success>(
                com.digihori.pgp.desktop.basic.DesktopBasicLoader.compileProgram(source.encodeToByteArray(), canonical(machine))).bytes
            assertContentEquals(expected.copyOfRange(1, expected.lastIndex), encoded.s2BasicPayload?.body?.copyBytes())
            val actual = assertIs<com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult.Success>(
                com.digihori.pgp.desktop.basic.DesktopBasicLoader.compileProgram(decoded.program, canonical(machine))).bytes
            assertContentEquals(expected, actual)
        }
    }

    @Test fun s2CorruptPayloadAndWrongDialectBlockProgramExport() {
        val encoded = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId("pc-1360"), "TEST", true)
        val wrong = DesktopWavOperation.decode(assertNotNull(encoded.wav), MachineId("pc-1350"))
        assertNotNull(wrong.s2BasicPayload)
        assertNull(wrong.error)
        assertNotNull(wrong.program)
        val raw = assertNotNull(encoded.raw).copyBytes().also { it[9] = (it[9].toInt() xor 1).toByte() }
        val signal = assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(RawTransferByteSequence(raw))).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, signal.samples)).value.bytes
        val damaged = DesktopWavOperation.decode(wav, MachineId("pc-1360"))
        assertNotNull(damaged.pcm)
        assertContentEquals(raw, damaged.raw?.copyBytes())
        assertNull(damaged.program)
        assertNull(damaged.s2BasicPayload)
        assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, damaged.diagnostics.first().code)
    }

    @Test fun s2MachineSelectionKeeps1470uVocabularyDistinct() {
        val source = "10 LLINE 1,2,3,4".encodeToByteArray()
        val result = DesktopWavOperation.encode(source, false, MachineId("pc-1470u"), "TEST", false)
        val payload = assertNotNull(result.s2BasicPayload)
        assertContentEquals(byteArrayOf(0xfe.toByte(), 0x6a), payload.body.copyBytes().copyOfRange(3, 5))
        val decoded = DesktopWavOperation.decode(assertNotNull(result.wav), MachineId("pc-1470u"))
        assertNotNull(decoded.program, decoded.report())
        assertTrue(decoded.content.contains("LLINE"))
        val extended = DesktopWavOperation.encode("10 GCURSOR 1,2".encodeToByteArray(), false, MachineId("pc-1360"), "TEST", false)
        val other = DesktopWavOperation.decode(assertNotNull(extended.wav), MachineId("pc-1470u"))
        assertNotNull(other.program)
        assertNull(other.error)
    }

    @Test fun signalWarningKeepsValidatedPreviewButBlocksExportForEveryFormat() {
        for ((id, binary) in listOf("pc-1245" to true, "pc-1350" to true, "pc-1350" to false, "pc-1360" to false)) {
            val machine = MachineId(id)
            val encoded = DesktopWavOperation.encode((if (binary) "C000: 01" else "10 END").encodeToByteArray(), binary, machine, "TEST", false)
            val raw = assertNotNull(encoded.raw).copyBytes() + byteArrayOf(0x12)
            val old = id == "pc-1245"
            val samples = if (old) assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(RawTransferByteSequence(raw))).value.samples
                else assertIs<CodecResult.Success<S1SignalEncoding>>(S1SignalEncoder.encode(RawTransferByteSequence(raw))).value.samples
            val pcm = samples.copySamples().copyOf(samples.size - if (old) 100 else 200)
            val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, PcmSampleSequence(pcm))).value.bytes
            val decoded = DesktopWavOperation.decode(wav, machine)
            assertTrue(decoded.validated, id + decoded.report())
            assertTrue(decoded.diagnostics.any { it.stage == CodecStage.SIGNAL_DECODE && it.severity == CodecDiagnosticSeverity.WARNING })
            assertNull(decoded.program)
            assertNull(decoded.hexProgram)
            assertNotNull(decoded.raw)
            assertEquals(com.digihori.pgp.core.api.MachineCatalog.require(machine).basicDialect, decoded.signalDialect)
        }
    }

    @Test fun diagnosticRawNavigationClampsEofAndEmptyData() {
        assertEquals(0, rawPageFor(256, 256))
        assertEquals(1, rawPageFor(512, 512))
        assertEquals(0, rawPageFor(100, 0))
        assertEquals(0, rawPageFor(-1, 10))
        assertEquals(1, rawPageFor(256, 513))
    }

    @Test fun s1ProtectedBasicPreservesProtectionFlag() {
        val machine = MachineId("pc-1350")
        val result = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, machine, "TEST", true)
        assertEquals(0x17, assertNotNull(result.raw).unsignedByteAt(0))
        val decoded = DesktopWavOperation.decode(assertNotNull(result.wav), machine)
        assertTrue(assertNotNull(decoded.s1BasicPayload).passwordProtected)
        assertNotNull(decoded.program)
    }

    @Test fun projectDefaultsSelectDialectAndProgramKind() {
        assertEquals(com.digihori.pgp.core.api.MachineBasicDialect.OLD to "binary", projectWavDefaults(MachineId("pc-1251"), "dmp"))
        assertEquals(com.digihori.pgp.core.api.MachineBasicDialect.OLD to "basic", projectWavDefaults(MachineId("pc-1251"), "bas"))
        assertEquals(com.digihori.pgp.core.api.MachineBasicDialect.S1 to "binary", projectWavDefaults(MachineId("pc-1401"), "HEX"))
        assertEquals(com.digihori.pgp.core.api.MachineBasicDialect.S1 to "basic", projectWavDefaults(MachineId("pc-1401"), "bas"))
        assertEquals(com.digihori.pgp.core.api.MachineBasicDialect.S2 to "basic", projectWavDefaults(MachineId("pc-1470u"), "bas"))
        assertEquals(com.digihori.pgp.core.api.MachineBasicDialect.S1 to "binary", projectWavDefaults(MachineId("pc-1360"), "dmp"))
    }

    @Test fun longBasicTransferNamesAreShortenedAndReported() {
        for (id in listOf("pc-1350", "pc-1360")) for (protected in listOf(false, true)) {
            val result = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId(id), "LongFilename", protected)
            assertNotNull(result.wav, result.report())
            assertEquals("LongFil", result.transferFilename)
            assertTrue(result.report().contains("shortened"))
            assertTrue(result.report().contains("LongFilename"))
            assertTrue(result.diagnostics.none { it.severity == CodecDiagnosticSeverity.ERROR })
        }
    }

    @Test fun wavS1AllowsKanaRegardlessOfProjectMachine() {
        val source = "10 PRINT \"ｱｲｳ\"".encodeToByteArray()
        val a = DesktopWavOperation.encode(source, false, MachineId("pc-1401"), "TEST", false)
        val b = DesktopWavOperation.encode(source, false, MachineId("pc-1350"), "TEST", false)
        assertNotNull(a.wav, a.report())
        assertContentEquals(b.wav?.copyBytes(), a.wav.copyBytes())
    }

    @Test fun autoDetectsAllFormatsWithoutMachineOrFormatSelection() {
        for (id in listOf("pc-1245", "pc-1350", "pc-1360")) for (protected in listOf(false, true)) {
            val encoded = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId(id), "TEST", protected)
            val decoded = DesktopWavOperation.decode(assertNotNull(encoded.wav))
            assertNotNull(decoded.program, decoded.report())
            assertNotNull(decoded.detectedFormat)
            assertTrue(decoded.detectedFormat.contains(if (protected) "PASSWORD_PROTECTED_BASIC" else "BASIC"))
        }
        val binary = DesktopWavOperation.encode("C000: 01 FF".encodeToByteArray(), true, MachineId("pc-1350"), "CODE", false)
        assertEquals("S1/S2 BINARY", DesktopWavOperation.decode(assertNotNull(binary.wav)).detectedFormat)
    }

    @Test fun conflictingStreamsAreNotSilentlyAssignedToOneFormat() {
        val old = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId("pc-1245"), "OLD", false)
        val modern = DesktopWavOperation.encode("10 END".encodeToByteArray(), false, MachineId("pc-1350"), "S1", false)
        val mixed = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, PcmSampleSequence(
            assertNotNull(old.pcm).samples.copySamples() + assertNotNull(modern.pcm).samples.copySamples()))).value.bytes
        val decoded = DesktopWavOperation.decode(mixed)
        assertNull(decoded.detectedFormat)
        assertNull(decoded.program)
        assertEquals(2, decoded.decodeCandidates.size)
        assertTrue(decoded.decodeCandidates.all { it.raw != null && it.pcm != null })
        assertTrue(decoded.report().contains("Candidate"))
    }

}
