package com.digihori.pgp.desktop.localization

/** Only internal source/parser diagnostics pass here; never source text or external tool logs. */
internal fun StudioStrings.transferDetail(message: String): String {
    if (language == StudioLanguage.ENGLISH) return message
    val known = when (message) {
        "Trailing escape character" -> text("transfer.detail.trailingEscape")
        "Raw byte escape requires two hexadecimal digits" -> text("transfer.detail.escapeDigits")
        "BASIC line must start with a line number" -> text("transfer.detail.lineNumber")
        "Invalid BASIC line number" -> text("transfer.detail.invalidLine")
        "BASIC line number must be between 1 and 999" -> text("transfer.detail.oldRange")
        "BASIC line number must be between 1 and 65279" -> text("transfer.detail.newRange")
        "Line number must be followed by whitespace or ':'" -> text("transfer.detail.lineSeparator")
        "Raw byte would conflict with BASIC program framing" -> text("transfer.detail.rawFraming")
        "Special symbol is not supported by PC-1245" -> text("transfer.detail.oldSymbol")
        "PC-1245 BASIC program must start with 0xFF" -> text("transfer.detail.startMarker")
        "PC-1245 BASIC program has no final 0xFF marker" -> text("transfer.detail.endMarker")
        "Data follows the BASIC program end marker" -> text("transfer.detail.afterEnd")
        "Truncated BASIC line number" -> text("transfer.detail.truncatedNumber")
        "Invalid OLD-family BASIC line number" -> text("transfer.detail.oldInvalidLine")
        "BASIC line is missing its 0x00 terminator" -> text("transfer.detail.lineTerminator")
        "Truncated BASIC line" -> text("transfer.detail.truncatedLine")
        "Unterminated string" -> text("transfer.detail.unterminatedString")
        "Raw bytes are not supported by the S1 tokenizer" -> text("transfer.detail.s1Raw")
        "Address must contain exactly four hexadecimal digits" -> text("transfer.detail.addressDigits")
        "Data is missing" -> text("transfer.detail.missingData")
        "Address and data must be separated by whitespace or ':'" -> text("transfer.detail.dumpSeparator")
        "Optional checksum must contain exactly two hexadecimal digits" -> text("transfer.detail.checksumDigits")
        "Data contains a non-hexadecimal character" -> text("transfer.detail.nonHex")
        "Data must contain complete two-digit bytes" -> text("transfer.detail.bytePairs")
        "Assembly produced no data" -> text("transfer.detail.noAssemblyData")
        "Output exceeds address 0xFFFF" -> text("transfer.detail.outputOverflow")
        "Relative target must be a 16-bit address" -> text("transfer.detail.relativeAddress")
        "Forward target is out of range" -> text("transfer.detail.forwardRange")
        "Backward target is out of range" -> text("transfer.detail.backwardRange")
        "Label is outside the 16-bit address space" -> text("transfer.detail.labelOutside")
        "Generated segment is outside the 16-bit address space" -> text("transfer.detail.segmentOutside")
        "Constant dependency chain is too deep" -> text("transfer.detail.equDepth")
        "Missing S1 start marker" -> text("transfer.detail.S1start")
        "Truncated S1 line header" -> text("transfer.detail.S1header")
        "Invalid S1 line length" -> text("transfer.detail.S1length")
        "Missing S1 line terminator" -> text("transfer.detail.S1terminator")
        "Missing S1 end marker" -> text("transfer.detail.S1end")
        "Encoded S1 line is too long" -> text("transfer.detail.S1long")
        "Missing S2 start marker" -> text("transfer.detail.S2start")
        "Truncated S2 line header" -> text("transfer.detail.S2header")
        "Invalid S2 line length" -> text("transfer.detail.S2length")
        "Missing S2 line terminator" -> text("transfer.detail.S2terminator")
        "Missing S2 end marker" -> text("transfer.detail.S2end")
        "Encoded S2 line is too long" -> text("transfer.detail.S2long")
        else -> null
    }
    if (known != null) return known
    Regex("Unknown escape: (.+)").matchEntire(message)?.let { match ->
        return text("transfer.detail.unknownEscape", match.groupValues[1])
    }
    Regex("Invalid raw byte escape: (.+)").matchEntire(message)?.let { match ->
        return text("transfer.detail.invalidEscape", match.groupValues[1])
    }
    Regex("Character '(.+)' is not supported by (.+)").matchEntire(message)?.let { match ->
        return text("transfer.detail.unsupportedCharacter", match.groupValues[1], match.groupValues[2])
    }
    Regex("Undefined symbol: (.+)").matchEntire(message)?.let { match ->
        return text("transfer.detail.undefinedSymbol", match.groupValues[1])
    }
    Regex("Unknown instruction(?: or directive)?: (.+)").matchEntire(message)?.let { match ->
        return text("transfer.detail.unknownInstruction", match.groupValues[1])
    }
    Regex("Layout expression requires an already resolved label: (.+)").matchEntire(message)?.let { match ->
        return text("transfer.detail.layoutSymbol", match.groupValues[1])
    }
    return text("transfer.detail.original", message)
}
