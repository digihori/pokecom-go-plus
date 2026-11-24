[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Path,
    [switch]$VerifyCurrent,
    [switch]$VerifyJapanese,
    [switch]$AsProperties
)
$ErrorActionPreference = 'Stop'
$installer = New-Object -ComObject WindowsInstaller.Installer
$database = $null
$view = $null
try {
    $msiPath = (Resolve-Path -LiteralPath $Path).Path
    $database = $installer.OpenDatabase($msiPath, 0)
    $view = $database.OpenView('SELECT `Property`, `Value` FROM `Property`')
    $view.Execute()
    $properties = @{}
    while ($null -ne ($record = $view.Fetch())) {
        try { $properties[$record.StringData(1)] = $record.StringData(2) }
        finally { [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($record) }
    }
    if ($VerifyCurrent) {
        if ($properties['ProductVersion'] -ne '1.9.2' -or
            $properties['UpgradeCode'] -ne '{D173EFD1-57EF-3753-A618-92481F262D95}' -or
            $properties['ProductName'] -ne 'PokecomSC61Studio') {
            throw 'MSI identity/version differs from the current distribution configuration.'
        }
    }
    if ($VerifyJapanese) {
        if ($properties['ProductLanguage'] -ne '1041') { throw 'The MSI ProductLanguage is not Japanese (1041).' }
        $view.Close()
        [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($view)
        $view = $null
        $view = $database.OpenView('SELECT `Text` FROM `Control`')
        $view.Execute()
        $localized = $false
        while ($null -ne ($record = $view.Fetch())) {
            try { if ($record.StringData(1) -match '[\u3041-\u30fe]') { $localized = $true; break } }
            finally { [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($record) }
        }
        if (-not $localized) { throw 'Japanese text is missing from the MSI dialog controls.' }
    }
    if ($AsProperties) {
        $properties
    } else {
        foreach ($name in @('ProductName', 'ProductVersion', 'ProductCode', 'UpgradeCode', 'ProductLanguage')) {
            Write-Output "$name=$($properties[$name])"
        }
    }
} finally {
    if ($null -ne $view) { $view.Close(); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($view) }
    if ($null -ne $database) { [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($database) }
    [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($installer)
}
