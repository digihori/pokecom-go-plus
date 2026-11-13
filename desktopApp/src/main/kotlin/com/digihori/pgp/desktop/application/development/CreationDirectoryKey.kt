package com.digihori.pgp.desktop.application.development

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Win32Exception
import com.sun.jna.platform.win32.WinBase.FILE_ID_INFO
import com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinNT.*
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

internal fun creationDirectoryKey(path: Path): Any {
    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
    if (Platform.isWindows()) return windowsDirectoryKey(path)
    return checkNotNull(attributes.fileKey()) { "The filesystem does not expose a directory identity" }
}

private data class WindowsDirectoryKey(val volume: Long, val fileId: List<Int>)

private fun windowsDirectoryKey(path: Path): WindowsDirectoryKey {
    // The JDK 21 Windows provider returns null from BasicFileAttributes.fileKey().
    val kernel = Kernel32.INSTANCE
    val handle = kernel.CreateFile(
        path.toString(), FILE_READ_ATTRIBUTES, FILE_SHARE_READ or FILE_SHARE_WRITE or FILE_SHARE_DELETE,
        null, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS or FILE_FLAG_OPEN_REPARSE_POINT, null,
    )
    if (handle == INVALID_HANDLE_VALUE) throw Win32Exception(kernel.GetLastError())
    try {
        val info = FILE_ID_INFO()
        val fileIdInfoClass = 18 // FILE_INFO_BY_HANDLE_CLASS.FileIdInfo
        if (!kernel.GetFileInformationByHandleEx(handle, fileIdInfoClass, info.pointer, DWORD(info.size().toLong()))) {
            throw Win32Exception(kernel.GetLastError())
        }
        info.read()
        return WindowsDirectoryKey(info.VolumeSerialNumber, info.FileId.Identifier.map { it.toInt() })
    } finally {
        kernel.CloseHandle(handle)
    }
}
