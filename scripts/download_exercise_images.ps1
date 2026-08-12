# Vendors free-exercise-db demonstration JPGs into app assets (one-time).
# After this, the app loads images only from the APK — no runtime network.
#
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File scripts/download_exercise_images.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$JsonPath = Join-Path $Root "app\src\main\assets\exercises.json"
$OutRoot = Join-Path $Root "app\src\main\assets\exercises"
$RemoteBase = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

if (-not (Test-Path $JsonPath)) { throw "Missing $JsonPath" }
New-Item -ItemType Directory -Force -Path $OutRoot | Out-Null

$json = Get-Content $JsonPath -Raw | ConvertFrom-Json
$paths = New-Object "System.Collections.Generic.HashSet[string]"
foreach ($e in $json) {
    if ($null -eq $e.images) { continue }
    foreach ($img in $e.images) { [void]$paths.Add([string]$img) }
}
$all = @($paths)
Write-Host "Unique images to vendor: $($all.Count)"

$downloaded = 0
$skipped = 0
$failed = 0
$n = 0

foreach ($item in $all) {
    $n++
    $dest = Join-Path $OutRoot ($item -replace "/", [IO.Path]::DirectorySeparatorChar)
    $dir = Split-Path -Parent $dest
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    if ((Test-Path $dest) -and ((Get-Item $dest).Length -gt 0)) {
        $skipped++
        if ($n % 100 -eq 0) { Write-Host "[$n/$($all.Count)] skipped existing…" }
        continue
    }
    $url = $RemoteBase + ($item -replace "\\", "/")
    try {
        Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing -TimeoutSec 60
        if (-not (Test-Path $dest) -or ((Get-Item $dest).Length -le 0)) {
            throw "Empty file"
        }
        $downloaded++
        if ($n % 50 -eq 0) {
            Write-Host "[$n/$($all.Count)] downloaded $downloaded so far…"
        }
    } catch {
        $failed++
        Write-Warning "Failed $item : $($_.Exception.Message)"
        if (Test-Path $dest) { Remove-Item $dest -Force -ErrorAction SilentlyContinue }
    }
}

$sizeMb = [math]::Round(
    ((Get-ChildItem $OutRoot -Recurse -File -ErrorAction SilentlyContinue |
        Measure-Object -Property Length -Sum).Sum) / 1MB, 1
)
Write-Host "Done. Downloaded: $downloaded  Existing: $skipped  Failed: $failed"
Write-Host "Asset folder size: $sizeMb MB"
Write-Host "Path: $OutRoot"
if ($failed -gt 0) { exit 1 }
