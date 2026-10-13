param(
    [string]$Source = $(if ($env:PGP_SYNC_SOURCE) { $env:PGP_SYNC_SOURCE } else { 'P:\' }),
    [string]$Destination = $(if ($env:PGP_SYNC_DESTINATION) { $env:PGP_SYNC_DESTINATION } else { 'C:\work\pokecom-go-plus' }),
    [switch]$Preview,
    [switch]$Run
)

$ErrorActionPreference = 'Stop'
$script:ExcludedDirectories = @('.git', '.gradle', '.gradle-local', '.kotlin', '.idea', '.codex', '.agents',
    '.pgp-sync', 'build', 'DerivedData', 'xcuserdata')
$script:ExcludedFiles = @('local.properties', '.DS_Store', '.env', '.env.*', '*.iml', '*.pgrom',
    '*mem.bin', '*bank.bin', '*.jks', '*.keystore', '*.p12', '*.pem', '*.xcuserstate', '*.log', '*.tmp', '*~')

function Test-PgpSyncPath([string]$RelativePath) {
    if ([string]::IsNullOrWhiteSpace($RelativePath) -or $RelativePath -match '[:\\]' -or $RelativePath.StartsWith('/')) {
        return $false
    }
    $parts = $RelativePath.Split('/')
    if (@($parts | Where-Object { $_ -in @('', '.', '..') -or $_ -in $script:ExcludedDirectories }).Count) { return $false }
    if ($parts[0] -eq 'local-data') { return $false }
    foreach ($pattern in $script:ExcludedFiles) { if ($parts[-1] -like $pattern) { return $false } }
    return $true
}

function Assert-PgpSyncDestination([string]$Root, [string]$RelativePath) {
    if (-not (Test-PgpSyncPath $RelativePath)) { throw "Invalid or protected manifest path: $RelativePath" }
    $path = $Root
    foreach ($part in $RelativePath.Split('/')) {
        $path = Join-Path $path $part
        if (Test-Path -LiteralPath $path) {
            $item = Get-Item -LiteralPath $path -Force
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Refusing a link/junction: $path" }
        }
    }
    return $path
}

function Get-PgpSyncFiles([string]$Root, [string]$Prefix = '') {
    foreach ($item in Get-ChildItem -LiteralPath $Root -Force -ErrorAction Stop) {
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
        $relative = if ($Prefix) { "$Prefix/$($item.Name)" } else { $item.Name }
        if ($item.PSIsContainer) {
            if ($item.Name -notin $script:ExcludedDirectories -and $relative -ne 'local-data') {
                Get-PgpSyncFiles $item.FullName $relative
            }
        } elseif (Test-PgpSyncPath $relative) { $relative }
    }
}

function Invoke-PgpSourceSync([string]$SourceRoot, [string]$DestinationRoot, [bool]$IsPreview) {
    $sourceFull = [IO.Path]::GetFullPath($SourceRoot)
    $destinationFull = [IO.Path]::GetFullPath($DestinationRoot)
    $sourcePrefix = $sourceFull.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $destinationPrefix = $destinationFull.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    if ($sourcePrefix.StartsWith($destinationPrefix, [StringComparison]::OrdinalIgnoreCase) -or
        $destinationPrefix.StartsWith($sourcePrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Source and destination must be separate, non-overlapping folders.'
    }
    foreach ($name in @('gradlew.bat', 'settings.gradle.kts')) {
        if (-not (Test-Path -LiteralPath (Join-Path $sourceFull $name) -PathType Leaf)) {
            throw "Shared project is unavailable: $sourceFull ($name missing)"
        }
    }
    # Reject destination links before copying or deleting anything.
    $ancestor = $destinationFull
    while ($ancestor) {
        if (Test-Path -LiteralPath $ancestor) {
            if ((Get-Item -LiteralPath $ancestor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw "Destination contains a link/junction: $ancestor"
            }
        }
        $ancestor = Split-Path -Parent $ancestor
    }
    $stateDirectory = Join-Path $destinationFull '.pgp-sync'
    if (Test-Path -LiteralPath $stateDirectory) {
        if ((Get-Item -LiteralPath $stateDirectory -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw 'Sync state directory must not be a link/junction.'
        }
    }
    $manifestPath = Join-Path $stateDirectory 'files.json'
    if (Test-Path -LiteralPath $manifestPath) {
        if ((Get-Item -LiteralPath $manifestPath -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw 'Sync manifest must not be a link/junction.'
        }
    }
    $current = @(Get-PgpSyncFiles $sourceFull | Sort-Object)
    $currentSet = New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    foreach ($relative in $current) {
        if (-not $currentSet.Add($relative)) { throw "Case-insensitive filename collision: $relative" }
        $null = Assert-PgpSyncDestination $destinationFull $relative
    }
    $previous = @()
    if (Test-Path -LiteralPath $manifestPath) {
        $state = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($state.version -ne 1 -or $state.source -ne $sourceFull -or $null -eq $state.files) {
            throw 'Sync manifest is invalid or belongs to another source. No files were deleted.'
        }
        $previous = @($state.files)
    }
    # Validate the entire old manifest before acting on any deletion.
    foreach ($relative in $previous) { $null = Assert-PgpSyncDestination $destinationFull $relative }
    $removed = @($previous | Where-Object { -not $currentSet.Contains($_) })
    Write-Host "Source:      $sourceFull"
    Write-Host "Destination: $destinationFull"
    Write-Host "Source files: $($current.Count); previously synced files removed on Mac: $($removed.Count)"
    if (-not (Test-Path -LiteralPath $manifestPath)) { Write-Host 'First tracked sync: existing destination-only files will be preserved.' }
    foreach ($relative in $removed) { Write-Host "Remove: $relative" }
    $arguments = @((Join-Path $sourceFull '.'), (Join-Path $destinationFull '.'), '/E', '/FFT', '/XJ',
        '/COPY:DAT', '/DCOPY:T', '/R:2', '/W:1', '/NP', '/XD') + $script:ExcludedDirectories +
        @((Join-Path $sourceFull 'local-data'), '/XF') + $script:ExcludedFiles
    if ($IsPreview) { $arguments += '/L' }
    & robocopy @arguments
    if ($LASTEXITCODE -ge 8) { throw "Robocopy failed (exit $LASTEXITCODE). No files were deleted; manifest unchanged." }
    if ($IsPreview) { return }
    # A dropped share or a changed inventory must not turn into source deletions.
    foreach ($name in @('gradlew.bat', 'settings.gradle.kts')) {
        if (-not (Test-Path -LiteralPath (Join-Path $sourceFull $name) -PathType Leaf)) { throw 'Share disconnected. Manifest unchanged; no deletions performed.' }
    }
    $after = @(Get-PgpSyncFiles $sourceFull | Sort-Object)
    if (@(Compare-Object $current $after).Count) { throw 'Mac source changed while copying. Retry sync; no deletions performed.' }
    foreach ($relative in $current) {
        if (-not (Test-Path -LiteralPath (Assert-PgpSyncDestination $destinationFull $relative) -PathType Leaf)) {
            throw "Copied file missing: $relative. No deletions performed."
        }
    }
    foreach ($relative in $removed) {
        $target = Assert-PgpSyncDestination $destinationFull $relative
        if (Test-Path -LiteralPath $target) {
            if (-not (Test-Path -LiteralPath $target -PathType Leaf)) { throw "Expected a file, found a directory: $target" }
            Remove-Item -LiteralPath $target -Force -ErrorAction Stop
        }
    }
    # Commit the manifest only after copy and deletion succeeded. Empty folders are kept.
    $null = New-Item -ItemType Directory -Path $stateDirectory -Force
    $temporary = Join-Path $stateDirectory ([Guid]::NewGuid().ToString() + '.tmp')
    try {
        @{ version = 1; source = $sourceFull; files = @($current) } | ConvertTo-Json -Depth 4 |
            Set-Content -LiteralPath $temporary -Encoding UTF8
        if (Test-Path -LiteralPath $manifestPath) { [IO.File]::Replace($temporary, $manifestPath, [NullString]::Value) }
        else { [IO.File]::Move($temporary, $manifestPath) }
    } finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
    Write-Host 'Source sync completed. Windows caches, local settings and untracked files are preserved.'
}

if ($MyInvocation.InvocationName -ne '.') {
    try {
        Invoke-PgpSourceSync $Source $Destination $Preview.IsPresent
        if ($Run -and -not $Preview) {
            Push-Location -LiteralPath $Destination
            try { & '.\gradlew.bat' --gradle-user-home .gradle-local :desktopApp:run; exit $LASTEXITCODE }
            finally { Pop-Location }
        }
        exit 0
    } catch { Write-Error $_ -ErrorAction Continue; exit 1 }
}
