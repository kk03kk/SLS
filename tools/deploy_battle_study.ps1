param(
    [string]$Bundle = 'local\archives\act2-battle-r1.tar.gz',
    [string]$Login = 'hengzhi@xlogin.comp.nus.edu.sg',
    [string]$Jump = 'hengzhi@sjump.comp.nus.edu.sg'
)
$ErrorActionPreference = 'Stop'
$taskBundle = (Resolve-Path -LiteralPath $Bundle).Path
$taskReceipt = Get-Content -LiteralPath ($taskBundle + '.json') -Raw | ConvertFrom-Json
$taskHash = (Get-FileHash -LiteralPath $taskBundle -Algorithm SHA256).Hash.ToLowerInvariant()
if ($taskHash -ne $taskReceipt.sha256) { throw 'Bundle bytes changed; refuse upload.' }
& scp -J $Jump $taskBundle ($Login + ':~/act2-battle-r1.tar.gz')
if ($LASTEXITCODE -ne 0) { throw 'SCP failed; no server execution attempted.' }
Write-Host 'Upload finished. Log in, pull the matching source, then install/inspect.'
Write-Host ('ssh -J ' + $Jump + ' ' + $Login)
Write-Host ('Required Git commit: ' + $taskReceipt.git_commit)
