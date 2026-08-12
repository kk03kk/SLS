[CmdletBinding()]
param(
    [int]$StartupTimeoutSeconds = 90,
    [switch]$AllowExisting,
    [Nullable[UInt64]]$MathSeed = $null
)

$ErrorActionPreference = "Stop"

$gameDir = "D:\Steam\steamapps\common\SlayTheSpire"
$java = Join-Path $gameDir "jre\bin\javaw.exe"
$gameJar = Join-Path $gameDir "desktop-1.0.jar"
$mts = "D:\Steam\steamapps\workshop\content\646570\1605060445\ModTheSpire.jar"
$baseMod = "D:\Steam\steamapps\workshop\content\646570\1605833019\BaseMod.jar"
$communicationMod = "D:\Steam\steamapps\workshop\content\646570\2131373661\CommunicationMod.jar"
$parityModSource = Join-Path (Split-Path -Parent $PSScriptRoot) "oracle_mod\build\SpirecommParity.jar"
$localModsDir = Join-Path $gameDir "mods"
$parityMod = Join-Path $localModsDir "SpirecommParity.jar"
$communicationConfig = Join-Path $env:LOCALAPPDATA "ModTheSpire\CommunicationMod\config.properties"
$projectDir = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $projectDir "logs\launcher"

$required = @($java, $gameJar, $mts, $baseMod, $communicationMod, $parityModSource, $communicationConfig)
foreach ($path in $required) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required launch file is missing: $path"
    }
}

New-Item -ItemType Directory -Path $localModsDir -Force | Out-Null
Copy-Item -LiteralPath $parityModSource -Destination $parityMod -Force

$configText = Get-Content -LiteralPath $communicationConfig -Raw
if ($configText -notmatch '(?m)^runAtGameStart=true\s*$') {
    throw "CommunicationMod runAtGameStart=true is not configured"
}
if ($configText -notmatch '(?m)^command=.+python(?:\.exe)?\s+.+\.py\s*$') {
    throw "CommunicationMod command does not point to a Python entry point"
}

$existing = @(
    Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^javaw?\.exe$' -and
        ($_.CommandLine -like '*ModTheSpire.jar*' -or $_.CommandLine -like '*desktop-1.0.jar*')
    }
)
if ($existing.Count -gt 0 -and -not $AllowExisting) {
    $ids = ($existing.ProcessId -join ', ')
    throw "Slay the Spire/ModTheSpire is already running (PID: $ids)"
}
if ($existing.Count -gt 0) {
    $existing | Select-Object ProcessId, ExecutablePath, CommandLine
    return
}

New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$stdoutLog = Join-Path $logDir "$stamp.stdout.log"
$stderrLog = Join-Path $logDir "$stamp.stderr.log"
$protocolLog = Join-Path $projectDir "logs\act1_corpus_protocol.jsonl"
$protocolLength = if (Test-Path -LiteralPath $protocolLog) {
    (Get-Item -LiteralPath $protocolLog).Length
} else {
    0
}

# ModTheSpire resolves desktop-1.0.jar relative to the working directory.
# --mods selects workshop mods by their ModTheSpire.json IDs and implies
# --skip-launcher. Steam's UI is intentionally not part of this launch path.
$arguments = @(
    '-Xmx2G',
    '-Dfile.encoding=UTF-8'
)
if ($null -ne $MathSeed) {
    $arguments += "-Dspirecomm.math_seed=$MathSeed"
}
$arguments += @(
    '-jar', $mts,
    '--skip-launcher',
    '--skip-intro',
    '--mods', 'basemod,CommunicationMod,spirecomm-parity'
)
$process = Start-Process -FilePath $java `
    -ArgumentList $arguments `
    -WorkingDirectory $gameDir `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
$pythonStarted = $false
$combatStateReceived = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 500
    $process.Refresh()
    if ($process.HasExited) {
        $stderr = if (Test-Path -LiteralPath $stderrLog) {
            Get-Content -LiteralPath $stderrLog -Raw
        } else { "" }
        throw "ModTheSpire exited during startup (code $($process.ExitCode)). $stderr"
    }
    $pythonRunning = @(
        Get-CimInstance Win32_Process | Where-Object {
            $_.Name -eq 'python.exe' -and $_.CommandLine -match 'spirecomm[/\\].+\.py'
        }
    ).Count -gt 0
    $pythonStarted = $pythonStarted -or $pythonRunning
    if (Test-Path -LiteralPath $protocolLog) {
        $currentProtocolLength = (Get-Item -LiteralPath $protocolLog).Length
        if ($currentProtocolLength -gt $protocolLength) {
            $stream = [System.IO.File]::Open(
                $protocolLog,
                [System.IO.FileMode]::Open,
                [System.IO.FileAccess]::Read,
                [System.IO.FileShare]::ReadWrite
            )
            try {
                [void]$stream.Seek($protocolLength, [System.IO.SeekOrigin]::Begin)
                $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
                $newProtocol = $reader.ReadToEnd()
            } finally {
                if ($null -ne $reader) { $reader.Dispose() } else { $stream.Dispose() }
            }
            $combatStateReceived = $newProtocol -match '\"room_phase\"\s*:\s*\"COMBAT\"'
        }
    }
    if ($combatStateReceived) {
        break
    }
    if ($pythonStarted -and -not $pythonRunning) {
        throw "CommunicationMod started Python, but it exited before a combat state was received. Check $projectDir\logs\act1_corpus_errors.jsonl and $stdoutLog"
    }
}

if (-not $combatStateReceived) {
    throw "Game stayed alive, but no CommunicationMod combat state arrived within $StartupTimeoutSeconds seconds. Check $stdoutLog and $stderrLog"
}

[pscustomobject]@{
    Status = "READY"
    JavaPid = $process.Id
    PythonStarted = $pythonStarted
    CombatStateReceived = $combatStateReceived
    StdoutLog = $stdoutLog
    StderrLog = $stderrLog
    WorkingDirectory = $gameDir
    Mods = "basemod,CommunicationMod,spirecomm-parity"
    MathSeed = if ($null -eq $MathSeed) { "derived from game seed" } else { [string]$MathSeed }
}
