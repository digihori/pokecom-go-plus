package com.digihori.pgp.desktop.application.development

private val basicLine = Regex("""[^\r\n]+""")
private val lineNumber = Regex("""^\uFEFF?[ \t]*[0-9]+[ \t]*""")
private val decimalCall = Regex("""(?i)([ \t]*CALL[ \t]*)([0-9]+)([ \t]*)""")

/** Format bare decimal CALL operands in numbered BASIC source before Studio reviews it. */
internal fun formatBasicCallAddresses(machineId: String, source: String): String {
    if (machineId !in GeneratedProjectFiles.MACHINES) return source
    return basicLine.replace(source) { formatLine(it.value) }
}

private fun formatLine(line: String): String {
    val number = lineNumber.find(line) ?: return line
    // Raw byte escapes can encode quotes, delimiters or keywords outside the visible text.
    if ("\\x" in line) return line
    return buildString {
        append(number.value)
        var start = number.range.last + 1
        var at = start
        var quoted = false
        var conditional = false
        while (at < line.length) {
            if (at == start) {
                val head = line.skipHorizontalSpace(start)
                if (line.startsWith("REM", head, ignoreCase = true)) break
                conditional = line.keywordAt("IF", head)
            }
            val character = line[at]
            if (character == '"') quoted = !quoted
            if (!quoted) {
                if (character == ':' || character == '\'') {
                    append(formatStatement(line.substring(start, at)))
                    start = at
                    if (character == '\'') break
                    append(character)
                    start++
                } else if (conditional && at > start && line[at - 1] in " \t" && line.keywordAt("THEN", at)) {
                    val end = at + 4
                    append(line, start, end)
                    start = end
                    at = end
                    continue
                }
            }
            at++
        }
        append(if (at == line.length && !quoted) formatStatement(line.substring(start)) else line.substring(start))
    }
}

private fun String.skipHorizontalSpace(start: Int): Int {
    var at = start
    while (at < length && this[at] in " \t") at++
    return at
}

private fun String.keywordAt(keyword: String, at: Int): Boolean =
    startsWith(keyword, at, ignoreCase = true) && (getOrNull(at + keyword.length) == ' ' || getOrNull(at + keyword.length) == '\t')

private fun formatStatement(statement: String): String {
    val call = decimalCall.matchEntire(statement) ?: return statement
    val address = call.groupValues[2].trimStart('0').ifEmpty { "0" }.toIntOrNull() ?: return statement
    if (address !in 0..0xffff) return statement
    val hex = address.toString(16).uppercase().padStart(4, '0')
    val prefix = call.groupValues[1]
    val separator = if (prefix.last() in " \t") "" else " "
    return "$prefix$separator&$hex" + call.groupValues[3]
}
