package com.digihori.pgp.core.wav

import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.*
import kotlin.test.*

class S1SignalVectorTest {
    @Test
    fun s2PayloadAndMachineSourceMatchAllIndependentVectors() {
        val file = sequenceOf(File("test-data/wav/s1-s2/vectors.json"), File("../test-data/wav/s1-s2/vectors.json")).first { it.isFile }
        val cases = Json.parseToJsonElement(file.readText()).jsonObject.getValue("cases").jsonArray
        var count = 0
        for (entry in cases) {
            val case = entry.jsonObject
            if (case.getValue("kind").jsonPrimitive.content != "s2basic") continue
            count++
            val id = case.getValue("id").jsonPrimitive.content
            fun bytes(key: String) = case.getValue(key).jsonPrimitive.content.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val expected = bytes("expectedRawHex")
            val body = bytes("logicalHex")
            val image = assertIs<com.digihori.pgp.core.source.basic.BasicProgramCompileResult.Success>(
                com.digihori.pgp.core.source.basic.BasicProgramCodec.compileProgram(case.getValue("source").jsonPrimitive.content.encodeToByteArray(),
                    com.digihori.pgp.core.rom.MachineId("pc-1360")), id).bytes
            assertContentEquals(body, image.copyOfRange(1, image.lastIndex), id)
            val encoded = assertIs<CodecResult.Success<S2BasicEncoding>>(S2BasicCodec.encode(case.getValue("name").jsonPrimitive.content,
                LogicalByteSequence(body), S2BasicType.entries.first { it.rawValue == case.getValue("type").jsonPrimitive.int })).value
            assertContentEquals(expected, encoded.rawBytes.copyBytes(), id)
            val decoded = assertIs<CodecResult.Success<S2BasicDecoding>>(S2BasicCodec.decode(
                RawTransferByteSequence(expected + byteArrayOf(-1)))).value
            assertContentEquals(body, decoded.body.copyBytes(), id)
            assertEquals(encoded.type, decoded.type)
        }
        assertEquals(11, count)
    }

    @Test
    fun s1BasicPayloadMatchesIndependentVectors() {
        val file = sequenceOf(File("test-data/wav/s1-s2/vectors.json"), File("../test-data/wav/s1-s2/vectors.json")).first { it.isFile }
        val cases = Json.parseToJsonElement(file.readText()).jsonObject.getValue("cases").jsonArray
        var count = 0
        for (entry in cases) {
            val case = entry.jsonObject
            if (case.getValue("kind").jsonPrimitive.content != "s1basic") continue
            count++
            fun bytes(key: String) = case.getValue(key).jsonPrimitive.content.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val expected = bytes("expectedRawHex")
            val body = bytes("logicalHex")
            val encoded = assertIs<CodecResult.Success<S1BasicEncoding>>(S1BasicCodec.encode(
                case.getValue("name").jsonPrimitive.content, LogicalByteSequence(body))).value
            assertContentEquals(expected, encoded.rawBytes.copyBytes(), case.getValue("id").jsonPrimitive.content)
            assertContentEquals(body, assertIs<CodecResult.Success<S1BasicDecoding>>(
                S1BasicCodec.decode(RawTransferByteSequence(expected + byteArrayOf(-1)))).value.body.copyBytes())
        }
        assertEquals(8, count)
    }

    @Test
    fun binaryPayloadMatchesFixedVectorsAndRejectsLegacyPartialTails() {
        val file = sequenceOf(File("test-data/wav/s1-s2/vectors.json"), File("../test-data/wav/s1-s2/vectors.json")).first { it.isFile }
        val cases = Json.parseToJsonElement(file.readText()).jsonObject.getValue("cases").jsonArray
        var count = 0
        for (entry in cases) {
            val case = entry.jsonObject
            if (case.getValue("kind").jsonPrimitive.content != "s1bin") continue
            count++
            fun bytes(key: String) = case.getValue(key).jsonPrimitive.content.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val expected = bytes("expectedRawHex")
            val logical = bytes("logicalHex")
            val encoded = assertIs<CodecResult.Success<S1BinaryEncoding>>(S1BinaryCodec.encode(
                case.getValue("name").jsonPrimitive.content, case.getValue("address").jsonPrimitive.int,
                LogicalByteSequence(logical))).value
            assertContentEquals(expected, encoded.rawBytes.copyBytes())
            assertContentEquals(logical, assertIs<CodecResult.Success<S1BinaryDecoding>>(
                S1BinaryCodec.decode(RawTransferByteSequence(expected))).value.body.copyBytes())
            val legacy = bytes("referenceRawHex")
            if (!legacy.contentEquals(expected)) assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH,
                assertIs<CodecResult.Failure>(S1BinaryCodec.decode(RawTransferByteSequence(legacy))).diagnostics.single().code)
        }
        assertEquals(8, count)
    }

    @Test
    fun matchesEveryIndependentPcmVectorAndRecoversRaw() {
        val file = sequenceOf(File("test-data/wav/s1-s2/vectors.json"), File("../test-data/wav/s1-s2/vectors.json"))
            .first { it.isFile }
        val cases = Json.parseToJsonElement(file.readText()).jsonObject.getValue("cases").jsonArray
        assertEquals(28, cases.size)
        for (entry in cases) {
            val case = entry.jsonObject
            val id = case.getValue("id").jsonPrimitive.content
            val hex = case.getValue("expectedRawHex").jsonPrimitive.content
            val payload = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val kind = case.getValue("kind").jsonPrimitive.content
            val raw = if (kind.endsWith("basic")) payload + byteArrayOf(0xff.toByte()) else payload
            val signal = assertIs<CodecResult.Success<S1SignalEncoding>>(
                S1SignalEncoder.encode(RawTransferByteSequence(raw)), id).value
            val pcm = signal.samples.copySamples()
            assertEquals(case.getValue("expectedPcmSamples").jsonPrimitive.int, pcm.size, id)
            val unsigned = ByteArray(pcm.size) { ((pcm[it].toInt() shr 8) + 128).toByte() }
            val hash = MessageDigest.getInstance("SHA-256").digest(unsigned)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            assertEquals(case.getValue("expectedPcmSha256").jsonPrimitive.content, hash, id)
            val decoded = assertIs<CodecResult.Success<S1SignalDecoding>>(S1SignalDecoder.decode(8000, signal.samples), id)
            assertContentEquals(raw, decoded.value.rawBytes.copyBytes(), id)
            assertTrue(decoded.diagnostics.isEmpty(), id)
        }
    }
}
