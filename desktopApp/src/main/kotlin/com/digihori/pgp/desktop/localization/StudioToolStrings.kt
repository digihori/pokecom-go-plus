package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.wav.*
import com.digihori.pgp.desktop.wav.DesktopWavResult
import com.digihori.pgp.desktop.message

// UI-only conversion. The operation result and exported report retain original text.
private val toolMessages = mapOf(
    "Transfer format could not be determined" to "tool.auto.undetermined",
    "No transfer format passed validation" to "tool.auto.none",
    "Multiple transfer formats passed validation" to "tool.auto.multiple",
    "Expected S2 BASIC type 27 or 37" to "tool.s2Basic.expectedType",
    "Select an S2 target machine to interpret BASIC tokens." to "tool.s2Basic.selectMachine",
    "Unsupported WAV target machine." to "tool.s2Basic.unsupportedTarget",
    "S1 BASIC filename must contain at most seven printable ASCII characters" to "tool.s1Basic.0",
    "S1 BASIC header is truncated" to "tool.s1Basic.1",
    "Expected S1 BASIC type 07" to "tool.s1Basic.2",
    "S1 BASIC filename terminator is missing" to "tool.s1Basic.3",
    "S1 BASIC filename characters or padding are invalid" to "tool.s1Basic.4",
    "S1 BASIC chunk checksum is missing" to "tool.s1Basic.5",
    "S1 BASIC line is truncated" to "tool.s1Basic.6",
    "S1 BASIC FF FF footer is missing" to "tool.s1Basic.7",
    "S1 BASIC line number or length is invalid" to "tool.s1Basic.8",
    "S1 BASIC line must end with 0D" to "tool.s1Basic.9",
    "S1 BASIC body exceeds 65536 bytes" to "tool.s1Basic.10",
    "S1 BASIC body must contain a line" to "tool.s1Basic.11",
    "S1 BASIC tail checksum is missing" to "tool.s1Basic.12",
    "S1 BASIC has trailing raw data" to "tool.s1Basic.13",
    "S1 BASIC image must begin and end with FF" to "tool.s1Basic.14",
    "S1 BASIC body must contain 1..65536 bytes of line records" to "tool.s1Basic.15",
    "S1 BASIC line header is truncated" to "tool.s1Basic.16",
    "S1 BASIC checksum mismatch" to "tool.s1Basic.17",
    "Select an S1 target machine to interpret BASIC tokens." to "tool.s1Basic.18",
    "S1 BASIC text cannot preserve all bytes for the selected machine" to "tool.s1Basic.19",
    "S2 BASIC WAV is not implemented." to "tool.s1.basicUnsupported",
    "S1/S2 signal failed" to "tool.s1.0",
    "S1 binary requires a nonempty body within the 16-bit address space" to "tool.s1.1",
    "S1 binary header is truncated" to "tool.s1.2",
    "Expected S1 binary type 67" to "tool.s1.3",
    "S1 binary filename markers are invalid" to "tool.s1.4",
    "S1 binary reserved metadata must be zero" to "tool.s1.5",
    "S1 binary filename contains invalid characters or padding" to "tool.s1.6",
    "S1 binary metadata exceeds the 16-bit address space" to "tool.s1.7",
    "S1 binary body or footer is truncated" to "tool.s1.8",
    "S1 binary FF FF footer is missing" to "tool.s1.9",
    "S1 binary has trailing raw data" to "tool.s1.10",
    "S1 binary checksum mismatch" to "tool.s1.11",
    "S1/S2 signal requires a sample rate of 8000, 16000, 24000, 32000, 44100, or 48000 Hz" to "tool.s1.12",
    "S1/S2 signal ends with an incomplete raw byte" to "tool.s1.13",
    "S1/S2 byte framing or carrier was lost; waiting for a new leader" to "tool.s1.14",
    "No complete S1/S2 raw byte could be decoded" to "tool.s1.15",
    "S1/S2 signal exceeds the supported PCM sample count" to "tool.s1.16",
    "Unsupported target" to "tool.detail.unsupportedTarget",
    "Invalid memory dump" to "tool.detail.invalidMemoryDump",
    "Invalid memory image" to "tool.detail.invalidMemoryImage",
    "BASIC conversion failed" to "tool.detail.basicConversionFailed",
    "Encode failed" to "tool.detail.encodeFailed",
    "WAV container failed" to "tool.detail.wavContainerFailed",
    "PCM normalization failed" to "tool.detail.pcmNormalizationFailed",
    "OLD signal failed" to "tool.detail.oldSignalFailed",
    "Operation failed" to "tool.detail.operationFailed",
    "Select an OLD target machine to interpret BASIC tokens." to "tool.detail.selectAnOLDTargetMachineToInterpretBASICTokens",
    "BASIC program must begin with FF." to "tool.detail.basicProgramMustBeginWithFF",
    "BASIC program must end with FF." to "tool.detail.basicProgramMustEndWithFF",
    "OLD BASIC body must end with F0." to "tool.detail.oldBASICBodyMustEndWithF0",
    "OLD BASIC body must end with F0" to "tool.detail.oldBASICBodyMustEndWithF0WithPeriod",
    "OLD BASIC body contains data after its F0 terminator" to "tool.detail.oldBASICBodyContainsDataAfterItsF0Terminator",
    "OLD binary body must not be empty" to "tool.detail.oldBinaryBodyMustNotBeEmpty",
    "OLD binary start address must be between 0000 and FFFF" to "tool.detail.oldBinaryStartAddressMustBeBetween0000AndFFFF",
    "OLD binary body exceeds the 16-bit end offset" to "tool.detail.oldBinaryBodyExceedsThe16BitEndOffset",
    "OLD binary body exceeds the 16-bit address space" to "tool.detail.oldBinaryBodyExceedsThe16BitAddressSpace",
    "OLD payload is shorter than its type and filename section" to "tool.detail.oldPayloadIsShorterThanItsTypeAndFilenameSection",
    "OLD filename block is missing its F5 terminator" to "tool.detail.oldFilenameBlockIsMissingItsF5Terminator",
    "OLD BASIC body is missing its F0 terminator" to "tool.detail.oldBASICBodyIsMissingItsF0Terminator",
    "OLD binary payload is missing metadata" to "tool.detail.oldBinaryPayloadIsMissingMetadata",
    "OLD binary metadata exceeds the 16-bit address space" to "tool.detail.oldBinaryMetadataExceedsThe16BitAddressSpace",
    "OLD binary reserved metadata differs from 00 00 00 60" to "tool.detail.oldBinaryReservedMetadataDiffersFrom00000060",
    "OLD body is missing a checksum byte" to "tool.detail.oldBodyIsMissingAChecksumByte",
    "OLD binary body is shorter than its declared length" to "tool.detail.oldBinaryBodyIsShorterThanItsDeclaredLength",
    "OLD filename checksum mismatch" to "tool.detail.oldFilenameChecksumMismatch",
    "OLD binary metadata checksum mismatch" to "tool.detail.oldBinaryMetadataChecksumMismatch",
    "OLD body checksum mismatch" to "tool.detail.oldBodyChecksumMismatch",
    "WAV is shorter than its RIFF header" to "tool.detail.wavIsShorterThanItsRIFFHeader",
    "WAV RIFF signature is missing" to "tool.detail.wavRIFFSignatureIsMissing",
    "WAV WAVE signature is missing" to "tool.detail.wavWAVESignatureIsMissing",
    "WAV RIFF size is invalid" to "tool.detail.wavRIFFSizeIsInvalid",
    "WAV is shorter than its declared RIFF size" to "tool.detail.wavIsShorterThanItsDeclaredRIFFSize",
    "WAV contains a truncated chunk header" to "tool.detail.wavContainsATruncatedChunkHeader",
    "WAV chunk is too large" to "tool.detail.wavChunkIsTooLarge",
    "WAV chunk exceeds the declared RIFF size" to "tool.detail.wavChunkExceedsTheDeclaredRIFFSize",
    "WAV contains more than one fmt chunk" to "tool.detail.wavContainsMoreThanOneFmtChunk",
    "WAV contains more than one data chunk" to "tool.detail.wavContainsMoreThanOneDataChunk",
    "WAV odd-length chunk is missing its pad byte" to "tool.detail.wavOddLengthChunkIsMissingItsPadByte",
    "WAV fmt chunk is missing" to "tool.detail.wavFmtChunkIsMissing",
    "WAV data chunk is missing" to "tool.detail.wavDataChunkIsMissing",
    "WAV data does not contain complete sample frames" to "tool.detail.wavDataDoesNotContainCompleteSampleFrames",
    "WAV fmt chunk is shorter than 16 bytes" to "tool.detail.wavFmtChunkIsShorterThan16Bytes",
    "WAV block alignment does not match its PCM format" to "tool.detail.wavBlockAlignmentDoesNotMatchItsPCMFormat",
    "WAV sample rate must be positive" to "tool.detail.wavSampleRateMustBePositive",
    "WAV data exceeds the supported RIFF size" to "tool.detail.wavDataExceedsTheSupportedRIFFSize",
    "OLD signal exceeds the supported PCM sample count" to "tool.detail.oldSignalExceedsTheSupportedPCMSampleCount",
    "No complete OLD raw byte could be decoded from the PCM signal" to "tool.detail.noCompleteOLDRawByteCouldBeDecodedFromThePCMSignal",
    "OLD signal ends with an incomplete raw byte" to "tool.detail.oldSignalEndsWithAnIncompleteRawByte",
    "The byte values do not fit this format." to "tool.detail.theByteValuesDoNotFitThisFormat"
)
private val toolPatterns = listOf(
    Regex("Reserved metadata: (.*)") to "tool.s1.reservedMetadata",
    Regex("Detected format: (.*)") to "tool.auto.detected",
    Regex("Validated S2 (.+), filename: (.*)") to "tool.s2Basic.validated",
    Regex("Validated S1 PASSWORD_PROTECTED_BASIC, filename: (.*)") to "tool.s1Basic.protectedValidated",
    Regex("Validated S1 BASIC, filename: (.*)") to "tool.s1Basic.validated",
    Regex("Validated S1 BINARY, filename: (.*)") to "tool.s1.validated",
    Regex("Encoded for (.+)") to "tool.pattern.encodedFor",
    Regex("(\\d+) Hz / (\\d+) channel\\(s\\) / (\\d+)-bit") to "tool.pattern.hzChannelSBit",
    Regex("(\\d+) samples / (.+) seconds") to "tool.pattern.samplesSeconds",
    Regex("(\\d+) raw bytes / (\\d+) nibble sync points") to "tool.pattern.rawBytesNibbleSyncPoints",
    Regex("Validated OLD (.+), filename: (.*)") to "tool.pattern.validatedOLDFilename",
    Regex("(\\d+) logical bytes(.*)") to "tool.pattern.logicalBytes",
    Regex("Only OLD WAV is currently implemented for (.+)\\.") to "tool.pattern.onlyOLDWAVIsCurrentlyImplementedFor",
    Regex("Unknown OLD payload type (.+)") to "tool.pattern.unknownOLDPayloadType",
    Regex("Undefined OLD filename byte (.+)") to "tool.pattern.undefinedOLDFilenameByte",
    Regex("OLD payload has (\\d+) trailing raw byte\\(s\\)") to "tool.pattern.oldPayloadHasTrailingRawByteS",
    Regex("WAV has (\\d+) byte\\(s\\) after its RIFF container") to "tool.pattern.wavHasByteSAfterItsRIFFContainer",
    Regex("WAV encoding (\\d+) is not PCM") to "tool.pattern.wavEncodingIsNotPCM",
    Regex("WAV channel count (\\d+) is not supported") to "tool.pattern.wavChannelCountIsNotSupported",
    Regex("WAV sample rate (\\d+) Hz is not supported") to "tool.pattern.wavSampleRateHzIsNotSupported",
    Regex("WAV (\\d+)-bit PCM is not supported") to "tool.pattern.wavBitPCMIsNotSupported",
    Regex("OLD signal sample rate (\\d+) Hz is not supported") to "tool.pattern.oldSignalSampleRateHzIsNotSupported",
    Regex("(?:OLD )?WAV binary data must be contiguous; gap before 0x([0-9A-F]+)\\.") to "tool.pattern.oldWAVBinaryDataMustBeContiguousGapBefore0x",
    Regex("BASIC at (\\d+): (.+)") to "tool.pattern.basicAt",
    Regex("Enter (\\d+) byte values\\.") to "tool.pattern.enterByteValues",
    Regex("Expected (\\d+) byte values but found (\\d+)\\.") to "tool.pattern.expectedByteValuesButFound",
    Regex("Invalid byte value at position (\\d+): (.*)") to "tool.pattern.invalidByteValueAtPosition",
    Regex("Byte (\\d+) contains bits outside the (\\d+)\u00d7(\\d+) pattern") to "tool.pattern.byteContainsBitsOutsideThePattern",
    Regex("Expected (\\d+) bytes but received (\\d+)") to "tool.pattern.expectedBytesButReceived"
)

internal fun StudioStrings.toolDetail(message: String): String {
    if (language == StudioLanguage.ENGLISH) return message
    toolMessages[message]?.let { return text(it) }
    if (message.startsWith("S2 BASIC")) {
        toolMessages[message.replace("S2 BASIC", "S1 BASIC")]?.let { return text(it).replace("S1 BASIC", "S2 BASIC") }
    }
    for ((pattern, key) in toolPatterns) {
        val match = pattern.matchEntire(message) ?: continue
        val args = match.groupValues.drop(1).toMutableList()
        if (key in setOf("tool.pattern.validatedOLDFilename", "tool.s2Basic.validated")) {
            OldPayloadType.entries.firstOrNull { it.name == args[0] }?.let { args[0] = text("wav.payload.${it.name}") }
        }
        if (key == "tool.pattern.basicAt") args[1] = transferDetail(args[1])
        return text(key, *args.toTypedArray())
    }
    return text("tool.original", message)
}

internal fun StudioStrings.wavSummary(summary: String): String = summary.lineSequence().joinToString("\n") { toolDetail(it) }
internal fun StudioStrings.characterInputError(message: String): String = toolDetail(message)

internal fun CodecDiagnostic.uiDescription(strings: StudioStrings): String = buildString {
    append(strings.text("wav.diagnostic", strings.text("wav.severity.${severity.name}"),
        strings.text("wav.stage.${stage.name}"), code.name, strings.toolDetail(message)))
    range?.let { append(strings.text("wav.range", strings.text("wav.kind.${it.kind.name}"), it.offset.toString(), it.endExclusive.toString())) }
    if (details.isNotEmpty()) {
        append(" ")
        append(details.entries.joinToString { (key, value) ->
            strings.text("wav.detailItem", if (key in setOf("expected", "actual")) strings.text("wav.detail.$key") else key, value)
        })
    }
}

internal fun DesktopWavResult.uiError(strings: StudioStrings): String = when {
    compileError != null -> compileError.message(targetMachine.orEmpty(), strings)
    memoryError != null -> memoryError.message(strings)
    else -> error?.let { strings.toolDetail(it) }.orEmpty()
}
