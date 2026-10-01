# ============================================================================
# merge-all.ps1 -- one-click Forgix merge build for all deobfuscated MC versions
#
# Usage:
#   .\merge-all.ps1                 # merge all mergeable versions (26.1 / 26.2 / 26.3)
#   .\merge-all.ps1 26.3            # merge only the given version
#   .\merge-all.ps1 26.2 26.3       # merge the given subset
#
# NOTE:
#   1.21.11 is still obfuscated (fabric side is remapped back to intermediary,
#   so class bytes always split) and does NOT take part in Forgix merging.
#   To build it, use the per-loader subprojects directly:
#     .\gradlew.bat 1.21.11-fabric:build 1.21.11-neoforge:build
# ============================================================================
param([string[]]$Versions)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$Mergeable = @('26.1', '26.2', '26.3')
$targets = if ($Versions) { $Versions } else { $Mergeable }

foreach ($v in $targets) {
    if ($Mergeable -notcontains $v) {
        Write-Host "SKIP ${v}: not mergeable (obfuscated version or not registered in settings.gradle.kts)" -ForegroundColor Yellow
        continue
    }
    Write-Host "==== Merging loaders for MC $v ====" -ForegroundColor Cyan
    # Pass args as an array with the call operator: prevents PowerShell from
    # re-splitting "-Pforgix.mc=<v>" (the '.' inside the property name).
    $argList = @('mergeJars', "-Pforgix.mc=$v", '--console=plain')
    & "$PSScriptRoot\gradlew.bat" @argList
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] MC $v (exit=$LASTEXITCODE), aborting" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "[ OK ] MC $v -> build\merged\*+$v-*.jar" -ForegroundColor Green
}

Write-Host ''
Write-Host 'Merged artifacts (build\merged):' -ForegroundColor Green
Get-ChildItem build\merged\*.jar |
    Select-Object Name,
        @{ n = 'KB'; e = { [math]::Round($_.Length / 1KB, 1) } },
        LastWriteTime |
    Format-Table -AutoSize
