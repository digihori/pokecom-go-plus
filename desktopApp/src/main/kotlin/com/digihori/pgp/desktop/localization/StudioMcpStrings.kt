package com.digihori.pgp.desktop.localization

import com.digihori.pgp.desktop.mcp.DesktopMcpRequestSummary

internal fun StudioStrings.mcpRequestDescription(request: DesktopMcpRequestSummary?): String {
    if (request == null) return text("mcp.noRequests")
    val target = request.toolName?.let { " / $it" }.orEmpty()
    return text("mcp.request", request.capturedAt, request.method, target,
        text(if (request.succeeded) "mcp.success" else "mcp.failed"))
}

internal fun StudioStrings.mcpStartError(message: String): String {
    val detail = when (message) {
        "The filesystem does not support secure POSIX permissions or Windows ACLs" -> text("mcp.error.filesystem")
        "MCP credential file permissions are not owner-only" -> text("mcp.error.permissions")
        "Could not verify MCP credential file access" -> text("mcp.error.verify")
        "MCP credential file ACL is not owner-only" -> text("mcp.error.acl")
        "Cannot determine the user home directory" -> text("mcp.error.home")
        "LOCALAPPDATA is not available" -> text("mcp.error.localAppData")
        "Could not start MCP server" -> text("mcp.error.start")
        else -> message
    }
    return text("mcp.startFailed", detail)
}
