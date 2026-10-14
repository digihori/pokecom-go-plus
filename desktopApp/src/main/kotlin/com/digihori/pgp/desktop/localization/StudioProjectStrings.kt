package com.digihori.pgp.desktop.localization

internal fun StudioStrings.projectOperationError(message: String): String {
    val known = when (message) {
        "Project name must not be blank." -> text("project.blankName")
        "The filesystem root cannot be renamed." -> text("project.operation.rootRename")
        "Select an existing source file." -> text("project.operation.existingSource")
        "Supported source files are .bas, .asm and .dmp." -> text("project.operation.sourceTypes")
        "The src directory must be inside the project." -> text("project.operation.srcOutside")
        "Assembler timed out" -> text("project.operation.assemblerTimeout")
        "SC61 did not generate a DMP file" -> text("project.operation.noDmp")
        "SC61 DMP exceeds 2 MiB" -> text("project.operation.dmpLimit")
        "Assembler executable is required" -> text("project.operation.assemblerRequired")
        "Timeout must be 1..3600 seconds" -> text("project.operation.assemblerTimeoutRange")
        else -> null
    }
    if (known != null) return known
    val folder = Regex("A folder named (.+) already exists\\.").matchEntire(message)
    if (folder != null) return text("project.operation.folderExists", folder.groupValues[1])
    val exit = Regex("SC61 assembly failed \\(exit code (-?\\d+)\\)").matchEntire(message)
    if (exit != null) return text("project.operation.sc61Failed", exit.groupValues[1])
    return editorError(message)
}
