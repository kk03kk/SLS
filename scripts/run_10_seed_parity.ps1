[CmdletBinding()]
param(
    [int]$Count = 10,
    [int]$StartupTimeoutSeconds = 120,
    [int]$BattleTimeoutSeconds = 300,
    [UInt64[]]$Seeds = @()
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $PSScriptRoot
$launcher = Join-Path $PSScriptRoot "start_original_sts.ps1"
$python = "D:\Anaconda\envs\DL\python.exe"
$runStamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $projectDir "logs\parity_10_seed\$runStamp"
New-Item -ItemType Directory -Path $runDir -Force | Out-Null

if ($Seeds.Count -eq 0) {
    $bytes = New-Object byte[] 8
    $generated = @()
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        while ($generated.Count -lt $Count) {
            $generator.GetBytes($bytes)
            # CommunicationMod accepts a decimal seed. Keep it in the positive
            # signed-64-bit range for compatibility with Java Long parsing.
            $candidate = [BitConverter]::ToUInt64($bytes, 0) -band [UInt64]0x7FFFFFFFFFFFFFFF
            if ($candidate -notin $generated) { $generated += $candidate }
        }
    } finally {
        $generator.Dispose()
    }
    $Seeds = $generated
} else {
    $Count = $Seeds.Count
}

$manifest = [ordered]@{
    scope = "Ironclad A0, first combat of each independent game seed"
    seeds = @($Seeds | ForEach-Object { [string]$_ })
    started_at = (Get-Date).ToString("o")
}
$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runDir "manifest.json") -Encoding UTF8

$saveDir = "D:\Steam\steamapps\common\SlayTheSpire\saves"
$saveNames = @("IRONCLAD.autosave", "IRONCLAD.autosave.backUp")
$saveBackupDir = Join-Path $runDir "save_backup"
New-Item -ItemType Directory -Path $saveBackupDir -Force | Out-Null
$saveManifest = @{}
foreach ($name in $saveNames) {
    $source = Join-Path $saveDir $name
    $saveManifest[$name] = Test-Path -LiteralPath $source -PathType Leaf
    if ($saveManifest[$name]) {
        Copy-Item -LiteralPath $source -Destination (Join-Path $saveBackupDir $name) -Force
    }
}

$oldSeeds = $env:STS_CORPUS_SEEDS
$oldOutputDir = $env:STS_CORPUS_OUTPUT_DIR
$launch = $null
try {
    $env:STS_CORPUS_SEEDS = ($Seeds | ForEach-Object { [string]$_ }) -join ","
    $env:STS_CORPUS_OUTPUT_DIR = $runDir
    Write-Host "Launching one original-game process for $Count seeds"
    # Omit -MathSeed: the parity mod deterministically derives math_seed from
    # each new game seed, which is required when seeds share one JVM.
    $launch = & $launcher -StartupTimeoutSeconds $StartupTimeoutSeconds
    $deadline = (Get-Date).AddSeconds($BattleTimeoutSeconds * $Count)
    $completed = 0
    while ($completed -lt $Count -and (Get-Date) -lt $deadline) {
        $next = @(Get-ChildItem -LiteralPath $runDir -Filter "seed-*.json" -File).Count
        if ($next -ne $completed) {
            $completed = $next
            Write-Host "Original traces complete: $completed/$Count"
        }
        Start-Sleep -Milliseconds 500
    }
    if ($completed -lt $Count) {
        throw "Only $completed/$Count battle traces completed before timeout"
    }
} finally {
    if ($null -ne $launch -and $null -ne $launch.JavaPid) {
        $ownedProcess = Get-Process -Id $launch.JavaPid -ErrorAction SilentlyContinue
        if ($null -ne $ownedProcess) {
            Stop-Process -Id $launch.JavaPid -Force
            $ownedProcess.WaitForExit(10000) | Out-Null
        }
    }
    foreach ($name in $saveNames) {
        $target = Join-Path $saveDir $name
        if ($saveManifest[$name]) {
            Copy-Item -LiteralPath (Join-Path $saveBackupDir $name) -Destination $target -Force
        } elseif (Test-Path -LiteralPath $target -PathType Leaf) {
            Remove-Item -LiteralPath $target -Force
        }
    }
    $env:STS_CORPUS_SEEDS = $oldSeeds
    $env:STS_CORPUS_OUTPUT_DIR = $oldOutputDir
}

$report = Join-Path $runDir "report.json"
& $python (Join-Path $PSScriptRoot "report_seed_parity.py") $runDir --output $report
$reportExit = $LASTEXITCODE
Write-Host "Report: $report"
exit $reportExit
