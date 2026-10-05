param(
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Serial = 'emulator-5554'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$destination = Join-Path $PSScriptRoot '..\docs\demo-images'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
& $AdbPath -s $Serial shell mkdir -p /sdcard/Pictures/MiniGalleryDemo
if ($LASTEXITCODE -ne 0) { throw 'Khong ket noi duoc thiet bi ADB.' }
for ($index = 1; $index -le 10; $index++) {
    $name = 'demo_{0:D2}.png' -f $index
    $path = Join-Path $destination $name
    $bitmap = [System.Drawing.Bitmap]::new(640, 480)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 30 + $index * 16, 80 + $index * 10, 190 - $index * 9))
    $font = [System.Drawing.Font]::new('Arial', 42)
    try {
        $graphics.Clear($brush.Color)
        $graphics.FillEllipse([System.Drawing.Brushes]::White, 430, 40, 120, 120)
        $graphics.FillRectangle([System.Drawing.Brushes]::MidnightBlue, 0, 330, 640, 150)
        $graphics.DrawString(('DEMO {0:D2}' -f $index), $font, [System.Drawing.Brushes]::White, 40, 365)
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $font.Dispose()
        $brush.Dispose()
        $graphics.Dispose()
        $bitmap.Dispose()
    }
    & $AdbPath -s $Serial push $path "/sdcard/Pictures/MiniGalleryDemo/$name"
    if ($LASTEXITCODE -ne 0) { throw "ADB push failed: $name" }
    & $AdbPath -s $Serial shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Pictures/MiniGalleryDemo/$name"
    if ($LASTEXITCODE -ne 0) { throw "Media scan failed: $name" }
}
Write-Output 'Da nap 10 anh PNG vao Pictures/MiniGalleryDemo; khong xoa anh co san.'
