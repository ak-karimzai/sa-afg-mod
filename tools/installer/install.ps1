# GTA San Andreas - Afghanistan mod installer (Windows PowerShell 5+)
# Copies the mod into your game folder. No original game file is modified or overwritten:
# everything goes into new folders (modloader\afghanistan, data\maps\afghan, CLEO).
param([string]$GameDir = "")
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$src = Join-Path $here "game"

function Fail($msg) { Write-Host ""; Write-Host "ERROR: $msg" -ForegroundColor Red; Read-Host "Press Enter to close"; exit 1 }

if (-not $GameDir) {
    Add-Type -AssemblyName System.Windows.Forms
    $dlg = New-Object System.Windows.Forms.FolderBrowserDialog
    $dlg.Description = "Select your GTA San Andreas folder (the one that contains gta_sa.exe)"
    if ($dlg.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { exit 1 }
    $GameDir = $dlg.SelectedPath
}
Write-Host "Game folder: $GameDir"
if (-not (Test-Path (Join-Path $GameDir "gta_sa.exe"))) { Fail "gta_sa.exe not found. Pick the classic GTA San Andreas folder (not the Definitive Edition)." }
$img = Join-Path $GameDir "models\gta3.img"
if (-not (Test-Path $img)) { Fail "models\gta3.img not found." }
if (-not (Test-Path (Join-Path $GameDir "modloader"))) { Fail "ModLoader is not installed (no 'modloader' folder). Install ModLoader first - see README." }
if (-not (Test-Path (Join-Path $GameDir "cleo.asi")) -and -not (Test-Path (Join-Path $GameDir "CLEO.asi"))) {
    Write-Host "WARNING: cleo.asi not found. CLEO 4.4+ or CLEO 5 is required for mission text and the radio." -ForegroundColor Yellow
}
$exe = (Get-Item (Join-Path $GameDir "gta_sa.exe")).Length
if ($exe -ne 14383616) {
    Write-Host "NOTE: gta_sa.exe is not the 1.0 US (14,383,616 bytes) build. Steam/1.01 copies should be downgraded to 1.0 for ModLoader/CLEO." -ForegroundColor Yellow
}

Write-Host "Copying mod files..."
Copy-Item -Path (Join-Path $src "*") -Destination $GameDir -Recurse -Force

# Vanilla streamed IPLs must be replaced by empty ones, otherwise the game crashes without the city IDE/IPLs.
function Get-ImgIpls($path) {
    $names = @()
    $fs = [System.IO.File]::OpenRead($path)
    try {
        $br = New-Object System.IO.BinaryReader($fs)
        $magic = [System.Text.Encoding]::ASCII.GetString($br.ReadBytes(4))
        if ($magic -ne "VER2") { throw "$path is not an IMG v2 archive" }
        $count = $br.ReadUInt32()
        for ($i = 0; $i -lt $count; $i++) {
            [void]$br.ReadUInt32(); [void]$br.ReadUInt16(); [void]$br.ReadUInt16()
            $raw = [System.Text.Encoding]::ASCII.GetString($br.ReadBytes(24))
            $name = $raw.Split([char]0)[0]
            if ($name.ToLower().EndsWith(".ipl")) { $names += $name }
        }
    } finally { $fs.Close() }
    return $names
}
$empty = New-Object byte[] 76
[System.Text.Encoding]::ASCII.GetBytes("bnry").CopyTo($empty, 0)
$empty[0x1C] = 0x4C  # instance offset = header size
$total = 0
foreach ($archive in @("gta3.img", "gta_int.img")) {
    $p = Join-Path $GameDir "models\$archive"
    if (-not (Test-Path $p)) { continue }
    $out = Join-Path $GameDir "modloader\afghanistan\emptied\$archive"
    New-Item -ItemType Directory -Force -Path $out | Out-Null
    foreach ($n in (Get-ImgIpls $p)) {
        [System.IO.File]::WriteAllBytes((Join-Path $out $n), $empty)
        $total++
    }
}
Write-Host "Emptied $total vanilla stream IPL files (inside modloader\afghanistan only)."
Write-Host ""
Write-Host "Installed! Start the game and choose NEW GAME (old saves are not compatible)." -ForegroundColor Green
Write-Host "To remove the mod run UNINSTALL.bat."
Read-Host "Press Enter to close"
