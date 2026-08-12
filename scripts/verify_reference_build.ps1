[CmdletBinding()]
param(
    [string]$GameDirectory = "D:\Steam\steamapps\common\SlayTheSpire",
    [string]$WorkshopDirectory = "D:\Steam\steamapps\workshop\content\646570"
)

$ErrorActionPreference = "Stop"
$projectDirectory = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $projectDirectory "reference_build.json"
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json

$targets = @(
    [pscustomobject]@{
        Id = "game"
        Path = Join-Path $GameDirectory $manifest.game.relative_path
        Bytes = [int64]$manifest.game.bytes
        Sha256 = [string]$manifest.game.sha256
    }
)

foreach ($mod in $manifest.oracle_mods) {
    $targets += [pscustomobject]@{
        Id = [string]$mod.id
        Path = Join-Path (Join-Path $WorkshopDirectory $mod.workshop_id) $mod.jar
        Bytes = [int64]$mod.bytes
        Sha256 = [string]$mod.sha256
    }
}

$results = foreach ($target in $targets) {
    if (-not (Test-Path -LiteralPath $target.Path -PathType Leaf)) {
        [pscustomobject]@{
            Id = $target.Id
            Status = "MISSING"
            Path = $target.Path
            Bytes = $null
            Sha256 = $null
        }
        continue
    }

    $file = Get-Item -LiteralPath $target.Path
    $hash = (Get-FileHash -LiteralPath $target.Path -Algorithm SHA256).Hash.ToLowerInvariant()
    $matches = $file.Length -eq $target.Bytes -and $hash -eq $target.Sha256
    [pscustomobject]@{
        Id = $target.Id
        Status = if ($matches) { "MATCH" } else { "MISMATCH" }
        Path = $target.Path
        Bytes = $file.Length
        Sha256 = $hash
    }
}

$results | Format-Table -AutoSize
$failures = @($results | Where-Object { $_.Status -ne "MATCH" })
if ($failures.Count -gt 0) {
    throw "Reference build verification failed for: $($failures.Id -join ', ')"
}

Write-Output "REFERENCE_BUILD_OK"
