$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) {
    $jbr = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path -LiteralPath $jbr) { $env:JAVA_HOME = $jbr }
}
if ($env:JAVA_HOME) { $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
$projects = Get-ChildItem -LiteralPath $PSScriptRoot -Directory -Filter 'Bai*'
foreach ($project in $projects) {
    Push-Location $project.FullName
    try {
        & .\gradlew.bat assembleDebug lintDebug --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Build/lint failed: $($project.Name)" }
    } finally { Pop-Location }
}
Write-Output 'Build/lint passed for all seven projects.'
