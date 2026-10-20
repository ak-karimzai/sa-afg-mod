# Removes the Afghanistan mod. ModLoader, CLEO and SilentPatch stay installed (other mods may use them).
param([string]$GameDir = "")
$ErrorActionPreference = "Stop"
if (-not $GameDir) {
    Add-Type -AssemblyName System.Windows.Forms
    $dlg = New-Object System.Windows.Forms.FolderBrowserDialog
    $dlg.Description = "Select your GTA San Andreas folder"
    if ($dlg.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { exit 1 }
    $GameDir = $dlg.SelectedPath
}
$paths = @("modloader\afghanistan", "data\maps\afghan", "CLEO\afghan_radio", "CLEO\afghan_radio.cs", "CLEO\CLEO_TEXT\afghan.fxt")
foreach ($p in $paths) {
    $full = Join-Path $GameDir $p
    if (Test-Path $full) { Remove-Item -Recurse -Force $full; Write-Host "removed $p" }
}
Write-Host "Afghanistan mod removed (ModLoader, CLEO and SilentPatch were left installed). Load a vanilla save or start a new game." -ForegroundColor Green
Read-Host "Press Enter to close"
