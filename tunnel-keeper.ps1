# MindHarbor tunnel keeper
# Keeps the cpolar tunnel alive, refreshes the public URL file, and makes a
# daily backup of the H2 database.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tunnel-keeper.ps1          (one pass)
#   powershell -ExecutionPolicy Bypass -File tunnel-keeper.ps1 -Loop    (daemon, 30 min cycle)

$ErrorActionPreference = "SilentlyContinue"

$Cpolar   = "C:\cpolar\cpolar\cpolar.exe"
$Root     = $PSScriptRoot
$LogFile  = Join-Path $Root "cpolar.log"
$OutFile  = Join-Path $env:USERPROFILE "Desktop\mindharbor-url.txt"
$MyLog    = Join-Path $Root "tunnel-keeper.log"
$Health   = "http://localhost:8080/api/public/health"
$Loop     = $args -contains "-Loop"

# 数据库与备份位置
$DbFile   = "D:\智能体\2026-10-17-15-28-34\psych-counseling\data\mindharbor.mv.db"
$BakDir   = Join-Path $Root "backup"
$KeepDays = 7

function KLog($m) {
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $m"
    Add-Content -Path $MyLog -Value $line -Encoding UTF8
}

function Get-TunnelUrl {
    if (-not (Test-Path $LogFile)) { return $null }
    $m = Select-String -Path $LogFile -Pattern "Tunnel established at (https://[a-zA-Z0-9.\-]+)" | Select-Object -Last 1
    if ($m) { return $m.Matches[0].Groups[1].Value }
    return $null
}

function Set-UrlFile($u) {
    Set-Content -Path $OutFile -Encoding UTF8 -Value @(
        "MindHarbor public URL",
        "",
        $u,
        "",
        ("updated: " + (Get-Date -Format 'yyyy-MM-dd HH:mm')),
        "(free plan rotates the domain every 24h, this file refreshes itself)"
    )
}

function Test-LocalAlive {
    try {
        $r = Invoke-WebRequest -Uri $Health -TimeoutSec 4 -UseBasicParsing
        return ($r.StatusCode -eq 200)
    } catch { return $false }
}

function Test-UrlAlive($u) {
    try {
        $r = Invoke-WebRequest -Uri ($u + "/api/public/health") -TimeoutSec 8 -UseBasicParsing
        return ($r.StatusCode -eq 200)
    } catch { return $false }
}

# 每天备份一次数据库，只留最近 7 份。
# H2 是单文件库，进程异常退出可能损坏，备份是唯一的保险。
function Invoke-Backup {
    if (-not (Test-Path $DbFile)) { return }
    if (-not (Test-Path $BakDir)) {
        New-Item -ItemType Directory -Force -Path $BakDir | Out-Null
    }

    $today = Get-Date -Format 'yyyy-MM-dd'
    $dst = Join-Path $BakDir ("mindharbor-" + $today + ".db")
    if (Test-Path $dst) { return }

    # 这台机器上 Copy-Item 和 File.Copy 都会抛 Win32 error 0，但文件其实已复制成功。
    # 所以不信任调用返回值，改用「目标文件存在且大小一致」来判断结果。
    try { [IO.File]::Copy($DbFile, $dst, $true) } catch { }

    $srcBytes = (Get-Item $DbFile).Length
    $okSize = (Test-Path $dst) -and ((Get-Item $dst).Length -eq $srcBytes)

    if ($okSize) {
        KLog ("db backup ok -> " + (Split-Path $dst -Leaf) + " (" + [math]::Round($srcBytes / 1KB) + "KB)")

        $olds = @(Get-ChildItem $BakDir -Filter "mindharbor-*.db" | Sort-Object Name -Descending)
        if ($olds.Count -gt $KeepDays) {
            $olds | Select-Object -Skip $KeepDays | ForEach-Object {
                Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
            }
        }
    } else {
        KLog "db backup failed: file missing or size mismatch"
    }
}

function Invoke-Check {
    Invoke-Backup

    if (-not (Test-LocalAlive)) {
        KLog "local 8080 not responding, skip"
        return "skip"
    }

    $url = Get-TunnelUrl
    $running = (Get-Process cpolar -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0

    if ($running -and $url -and (Test-UrlAlive $url)) {
        KLog "tunnel ok: $url"
        $old = ""
        if (Test-Path $OutFile) { $old = Get-Content $OutFile -Raw }
        if ($old -notlike "*$url*") { Set-UrlFile $url }
        return "ok"
    }

    if ($running) { KLog "process alive but unreachable, restarting" }
    if ($running) { Get-Process cpolar | Stop-Process -Force; Start-Sleep -Seconds 2 }

    KLog "starting cpolar"
    $env:HTTP_PROXY = ""
    $env:HTTPS_PROXY = ""
    Start-Process -FilePath $Cpolar -ArgumentList "http", "8080", "--log=stdout" `
        -RedirectStandardOutput $LogFile -RedirectStandardError ($LogFile + ".err") `
        -WindowStyle Hidden

    $newUrl = $null
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        $newUrl = Get-TunnelUrl
        if ($newUrl) { break }
    }

    if ($newUrl) {
        Set-UrlFile $newUrl
        KLog "tunnel established: $newUrl"
        if ($url -and $url -ne $newUrl) {
            Add-Type -AssemblyName System.Windows.Forms
            [System.Windows.Forms.MessageBox]::Show(("Public URL changed:" + "`r`n`r`n" + $newUrl), "MindHarbor tunnel", "OK", "Information") | Out-Null
        }
        return "restarted"
    }

    KLog "failed: no url within 20s"
    return "failed"
}

do {
    $result = Invoke-Check
    if (-not $Loop) { break }
    Start-Sleep -Seconds 1800
} while ($true)
