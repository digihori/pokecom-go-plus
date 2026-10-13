# Runs only against a newly created temporary directory; no SMB or JDK required.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'sync-from-mac.ps1')
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('pgp-sync-test-' + [Guid]::NewGuid())
$sourceRoot = Join-Path $testRoot 'source with spaces'
$destinationRoot = Join-Path $testRoot 'destination with spaces'
$script:CopyFailure = $false
$script:ChangeSource = $false
$script:CopyCalls = 0
function Write-Fixture([string]$Root, [string]$Relative, [string]$Text) {
    $path = Join-Path $Root $Relative
    $null = New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force
    Set-Content -LiteralPath $path -Value $Text -Encoding UTF8
}
function Assert-Fixture([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "FAIL: $Message" }
}
function Assert-Stopped([scriptblock]$Action, [string]$Expected) {
    $caught = $false
    try { & $Action } catch {
        if ($_.Exception.Message -notlike "*$Expected*") { throw }
        $caught = $true
    }
    Assert-Fixture $caught "Expected failure: $Expected"
}
# Mock native copy only; exercise the real inventory, validation, deletion and manifest code.
function robocopy {
    param([Parameter(ValueFromRemainingArguments = $true)][object[]]$Arguments)
    $script:CopyCalls++
    $global:LASTEXITCODE = 1
    if ($script:CopyFailure) { $global:LASTEXITCODE = 8; return }
    if ($Arguments -contains '/L') { return }
    foreach ($relative in Get-PgpSyncFiles $sourceRoot) {
        $target = Join-Path $destinationRoot $relative
        $null = New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force
        Copy-Item -LiteralPath (Join-Path $sourceRoot $relative) -Destination $target -Force
    }
    if ($script:ChangeSource) { Write-Fixture $sourceRoot 'changed-during-copy.txt' 'new' }
}
try {
    Write-Fixture $sourceRoot 'gradlew.bat' 'fixture'
    Write-Fixture $sourceRoot 'settings.gradle.kts' 'fixture'
    Write-Fixture $sourceRoot 'src/日本語.txt' 'source'
    Write-Fixture $sourceRoot 'build/mac.txt' 'exclude'
    Write-Fixture $sourceRoot '.gradle-local/mac.txt' 'exclude'
    Write-Fixture $sourceRoot 'local-data/rom.bin' 'exclude'
    Write-Fixture $sourceRoot 'local.properties' 'exclude'
    Write-Fixture $sourceRoot 'sample.pgrom' 'exclude'
    Write-Fixture $destinationRoot 'windows-only.txt' 'keep'
    Write-Fixture $destinationRoot 'build/windows.txt' 'keep'
    Write-Fixture $destinationRoot 'local.properties' 'keep'
    Invoke-PgpSourceSync $sourceRoot $destinationRoot $false
    $manifest = Join-Path $destinationRoot '.pgp-sync/files.json'
    $before = Get-Content -LiteralPath $manifest -Raw
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'src/日本語.txt')) 'Unicode source copied'
    Assert-Fixture ((Get-Content -LiteralPath (Join-Path $destinationRoot 'local.properties')) -eq 'keep') 'Local settings preserved'
    Assert-Fixture (-not (Test-Path -LiteralPath (Join-Path $destinationRoot 'build/mac.txt'))) 'Build excluded'
    Assert-Fixture (-not (Test-Path -LiteralPath (Join-Path $destinationRoot 'local-data/rom.bin'))) 'ROM folder excluded'
    Assert-Fixture (-not (Test-Path -LiteralPath (Join-Path $destinationRoot 'sample.pgrom'))) 'ROM package excluded'
    Remove-Item -LiteralPath (Join-Path $sourceRoot 'src/日本語.txt')
    Write-Fixture $sourceRoot 'src/renamed.txt' 'renamed'
    Invoke-PgpSourceSync $sourceRoot $destinationRoot $true
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'src/日本語.txt')) 'Preview does not delete'
    Assert-Fixture ((Get-Content -LiteralPath $manifest -Raw) -eq $before) 'Preview preserves manifest'
    $script:CopyFailure = $true
    Assert-Stopped { Invoke-PgpSourceSync $sourceRoot $destinationRoot $false } 'Robocopy failed'
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'src/日本語.txt')) 'Failed copy does not delete'
    Assert-Fixture ((Get-Content -LiteralPath $manifest -Raw) -eq $before) 'Failed copy preserves manifest'
    $script:CopyFailure = $false
    $script:ChangeSource = $true
    Assert-Stopped { Invoke-PgpSourceSync $sourceRoot $destinationRoot $false } 'source changed'
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'src/日本語.txt')) 'Concurrent source change does not delete'
    Assert-Fixture ((Get-Content -LiteralPath $manifest -Raw) -eq $before) 'Concurrent source change preserves manifest'
    $script:ChangeSource = $false
    Remove-Item -LiteralPath (Join-Path $sourceRoot 'changed-during-copy.txt')
    Invoke-PgpSourceSync $sourceRoot $destinationRoot $false
    Assert-Fixture (-not (Test-Path -LiteralPath (Join-Path $destinationRoot 'src/日本語.txt'))) 'Former synced source deleted'
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'src/renamed.txt')) 'Renamed source present'
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'windows-only.txt')) 'Untracked Windows file preserved'
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'build/windows.txt')) 'Windows build output preserved'
    $goodManifest = Get-Content -LiteralPath $manifest -Raw
    foreach ($invalid in @('../outside.txt', 'build/windows.txt', 'local.properties', 'C:/outside.txt')) {
        @{version=1; source=[IO.Path]::GetFullPath($sourceRoot); files=@($invalid)} |
            ConvertTo-Json | Set-Content -LiteralPath $manifest -Encoding UTF8
        $calls = $script:CopyCalls
        Assert-Stopped { Invoke-PgpSourceSync $sourceRoot $destinationRoot $false } 'Invalid or protected'
        Assert-Fixture ($script:CopyCalls -eq $calls) 'Invalid manifest stops before copying'
    }
    Set-Content -LiteralPath $manifest -Value $goodManifest -Encoding UTF8
    Assert-Stopped { Invoke-PgpSourceSync $sourceRoot $sourceRoot $false } 'non-overlapping'
    Remove-Item -LiteralPath (Join-Path $sourceRoot 'settings.gradle.kts')
    Assert-Stopped { Invoke-PgpSourceSync $sourceRoot $destinationRoot $false } 'unavailable'
    Assert-Fixture (Test-Path -LiteralPath (Join-Path $destinationRoot 'src/renamed.txt')) 'Missing source does not delete'
    Write-Host 'PASS: first sync, rename/delete, preview, protected files, failures, concurrent changes, unsafe manifest and unavailable share.'
} finally {
    if (Test-Path -LiteralPath $testRoot) { Remove-Item -LiteralPath $testRoot -Recurse -Force }
    Remove-Item Function:robocopy
}
