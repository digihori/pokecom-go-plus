[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$EnglishMsi,
    [string]$AppImage = "$PSScriptRoot/../desktopApp/build/compose/binaries/main/app/PokecomSC61Studio",
    [string]$Destination = "$PSScriptRoot/../desktopApp/build/compose/binaries/main/msi-ja"
)

$ErrorActionPreference = 'Stop'
$identity = & "$PSScriptRoot/inspect-msi.ps1" -Path $EnglishMsi -VerifyCurrent -AsProperties
$appPath = (Resolve-Path -LiteralPath $AppImage).Path
$resourcePath = (Resolve-Path -LiteralPath "$PSScriptRoot/../desktopApp/packaging/windows/ja").Path
$iconPath = (Resolve-Path -LiteralPath "$PSScriptRoot/../desktopApp/icons/pokecom-go.ico").Path
$destinationPath = (New-Item -ItemType Directory -Path $Destination -Force).FullName

$jpackage = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/jpackage.exe' } else { 'jpackage.exe' }
$options = $env:JAVA_TOOL_OPTIONS
try {
    # jpackage selects its primary WiX UI culture using its own JVM locale.
    $env:JAVA_TOOL_OPTIONS = "$options -Duser.language=ja -Duser.country=JP".Trim()
    & $jpackage --type msi --app-image $appPath --dest $destinationPath `
        --name $identity.ProductName --app-version $identity.ProductVersion --vendor $identity.Manufacturer `
        --description 'A multiplatform pocket-computer development environment' --icon $iconPath `
        --win-upgrade-uuid ($identity.UpgradeCode.Trim('{}')) --win-menu --win-menu-group 'Pokecom SC61 Studio' `
        --win-dir-chooser --resource-dir $resourcePath
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE." }
} finally {
    $env:JAVA_TOOL_OPTIONS = $options
}

$msi = Join-Path $destinationPath "$($identity.ProductName)-$($identity.ProductVersion).msi"
& "$PSScriptRoot/inspect-msi.ps1" -Path $msi -VerifyCurrent -VerifyJapanese
