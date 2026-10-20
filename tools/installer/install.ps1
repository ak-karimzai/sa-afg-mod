# GTA San Andreas - Afghanistan mod installer (Windows PowerShell 5+). Same steps as install.py.
# 1. checks that gta_sa.exe is the 1.0 US executable
# 2. installs the bundled ModLoader, CLEO 5 and SilentPatch if they are missing
# 3. copies the mod into new folders (modloader\afghanistan, data\maps\afghan, CLEO)
# The only original file that can change is vorbisFile.dll (CLEO's ASI loader replaces it); it is backed up first.
param([string]$GameDir = "", [switch]$Force, [switch]$NoRequirements)
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$src = Join-Path $here "game"
$req = Join-Path $here "requirements"
$EP_10US = 0x424570  # entry point RVA of the 1.0 US exe (HOODLUM / Compact)

function Fail($msg) { Write-Host ""; Write-Host "ERROR: $msg" -ForegroundColor Red; Read-Host "Press Enter to close"; exit 1 }

if (-not $GameDir) {
    Add-Type -AssemblyName System.Windows.Forms
    $dlg = New-Object System.Windows.Forms.FolderBrowserDialog
    $dlg.Description = "Select your GTA San Andreas folder (the one that contains gta_sa.exe)"
    if ($dlg.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { exit 1 }
    $GameDir = $dlg.SelectedPath
}
Write-Host "Game folder: $GameDir"
$exePath = Join-Path $GameDir "gta_sa.exe"
if (-not (Test-Path $exePath)) { Fail "gta_sa.exe not found. Pick the classic GTA San Andreas folder (not the Definitive Edition)." }
$img = Join-Path $GameDir "models\gta3.img"
if (-not (Test-Path $img)) { Fail "models\gta3.img not found." }

# --- 1. game version
$fs = [System.IO.File]::OpenRead($exePath)
try {
    $br = New-Object System.IO.BinaryReader($fs)
    [void]$fs.Seek(0x3C, "Begin"); $pe = $br.ReadInt32()
    [void]$fs.Seek($pe + 0x28, "Begin"); $ep = $br.ReadUInt32()
} finally { $fs.Close() }
if ($ep -ne $EP_10US) {
    $msg = "gta_sa.exe is not the 1.0 US executable that ModLoader and CLEO need.`n" +
           "Steam / Rockstar Launcher copies: downgrade to 1.0 first.`n" +
           "Old disc copies (copy protected 1.0): replace gta_sa.exe with the 1.0 US 'Compact' exe.`n" +
           "See README, section 'Getting the 1.0 US exe'. Run install.ps1 -Force to install anyway."
    if (-not $Force) { Fail $msg }
    Write-Host "WARNING: $msg" -ForegroundColor Yellow
}

# --- 2. requirements
function Install-Zip($archive, $skipTop, $skipFiles) {
    $tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("afgmod_" + [guid]::NewGuid())
    Expand-Archive -Path (Join-Path $req $archive) -DestinationPath $tmp -Force
    try {
        Get-ChildItem -Path $tmp -Recurse -File -Force | ForEach-Object {
            $rel = $_.FullName.Substring($tmp.Length + 1)
            $top = $rel.Split([char]'\')[0].ToLower()
            if ($skipTop -contains $top -or $skipFiles -contains $rel.ToLower()) { return }
            $dst = Join-Path $GameDir $rel
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dst) | Out-Null
            Copy-Item -LiteralPath $_.FullName -Destination $dst -Force
        }
    } finally { Remove-Item -Recurse -Force $tmp }
}
if (-not $NoRequirements -and (Test-Path $req)) {
    if (Test-Path (Join-Path $GameDir "modloader.asi")) { Write-Host "ModLoader: already installed" }
    else { Install-Zip "modloader-0.3.10.zip" @("readme.txt", "leia-me.txt") @(); Write-Host "ModLoader: installed" }

    if (Test-Path (Join-Path $GameDir "cleo.asi")) { Write-Host "CLEO 5: already installed" }
    else {
        $skip = @()
        if ((Test-Path (Join-Path $GameDir "vorbisHooked.dll")) -or (Test-Path (Join-Path $GameDir "dinput8.dll"))) {
            $skip = @("vorbisfile.dll", "vorbishooked.dll")  # an ASI loader is already there; keep it
        } elseif (Test-Path (Join-Path $GameDir "vorbisFile.dll")) {
            $bak = Join-Path $GameDir "_afghanmod_backup"
            New-Item -ItemType Directory -Force -Path $bak | Out-Null
            Copy-Item (Join-Path $GameDir "vorbisFile.dll") (Join-Path $bak "vorbisFile.dll") -Force
            Write-Host "  original vorbisFile.dll backed up to _afghanmod_backup\ (CLEO ships it as vorbisHooked.dll)"
        }
        Install-Zip "CLEO-5.4.0-SilentASILoader.zip" @("cleo_readme") $skip
        Write-Host "CLEO 5: installed"
    }

    if (Test-Path (Join-Path $GameDir "SilentPatchSA.asi")) { Write-Host "SilentPatch: already installed" }
    else { Install-Zip "SilentPatchSA-1.1-build34.1.zip" @("readme.txt") @(); Write-Host "SilentPatch: installed" }
}
if (-not (Test-Path (Join-Path $GameDir "modloader"))) { Fail "ModLoader is not installed (no 'modloader' folder). See README." }
if (-not (Test-Path (Join-Path $GameDir "cleo.asi"))) {
    Write-Host "WARNING: cleo.asi not found. CLEO 4.4+ or CLEO 5 is required for mission text and the radio." -ForegroundColor Yellow
}

# ModLoader's AutoRefresh fails on network drives and floods modloader.log. The value must not carry an
# inline comment: ModLoader reads 'false ; comment' as true.
$mlData = Join-Path $GameDir "modloader\.data"
$cfg = Join-Path $mlData "config.ini"
if (-not (Test-Path $cfg) -and (Test-Path (Join-Path $mlData "config.ini.0"))) { Copy-Item (Join-Path $mlData "config.ini.0") $cfg }
if (Test-Path $cfg) {
    $text = [System.IO.File]::ReadAllText($cfg)
    $new = [regex]::Replace($text, '(?im)^[ \t]*AutoRefresh[ \t]*=.*?(\r?)$', 'AutoRefresh = false$1')
    if ($new -ne $text) { [System.IO.File]::WriteAllText($cfg, $new); Write-Host "ModLoader: AutoRefresh disabled" }
}

# --- 3. the mod
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
