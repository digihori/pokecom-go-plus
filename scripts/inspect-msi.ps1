[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Path,
    [switch]$VerifyCurrent
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
    foreach ($name in @('ProductName', 'ProductVersion', 'ProductCode', 'UpgradeCode')) {
        Write-Output "$name=$($properties[$name])"
    }
    if ($VerifyCurrent) {
        if ($properties['ProductVersion'] -ne '1.6.0' -or
            $properties['UpgradeCode'] -ne '{D173EFD1-57EF-3753-A618-92481F262D95}' -or
            $properties['ProductName'] -ne 'PokecomGOStudio') {
            throw 'MSI identity/version differs from the current distribution configuration.'
        }
    }
} finally {
    if ($null -ne $view) { $view.Close(); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($view) }
    if ($null -ne $database) { [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($database) }
    [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($installer)
}
