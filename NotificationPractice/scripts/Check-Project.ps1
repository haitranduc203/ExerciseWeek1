param(
    [switch]$ConnectedTests,
    [string]$Serial
)
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path -Parent $PSScriptRoot
$taskTemp = Join-Path $taskProjectRoot '.tmp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
$taskPreviousTemp = $env:TEMP
$taskPreviousTmp = $env:TMP
$taskPreviousJavaOptions = $env:JAVA_TOOL_OPTIONS
$taskPreviousSerial = $env:ANDROID_SERIAL
Push-Location $taskProjectRoot
try {
    $env:TEMP = $taskTemp
    $env:TMP = $taskTemp
    $taskJavaTemp = $taskTemp.Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = "$taskPreviousJavaOptions -Djdk.net.unixdomain.tmpdir=$taskJavaTemp".Trim()
    if ($Serial) { $env:ANDROID_SERIAL = $Serial }
    $taskGradleTasks = @(':app:assembleDebug', ':app:lintDebug')
    if ($ConnectedTests) { $taskGradleTasks += ':app:connectedDebugAndroidTest' }
    & .\gradlew.bat @taskGradleTasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
    $env:TEMP = $taskPreviousTemp
    $env:TMP = $taskPreviousTmp
    $env:JAVA_TOOL_OPTIONS = $taskPreviousJavaOptions
    $env:ANDROID_SERIAL = $taskPreviousSerial
}