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
$AppDir   = "D://智能体//2026-10-17-15-28-34//psych-counseling"

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

# 启动应用（只在没有 java 进程时调用，绝不强杀——H2 文件库禁不起）
function Start-App {
    if ((Get-Process java -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0) { return }

    $jar = Join-Path $AppDir "target\psych-counseling-1.0.0.jar"
    if (-not (Test-Path $jar)) { KLog "app jar missing, cannot start"; return }

    # 把 ai-config.env 里的键值读进当前进程环境（API key、模型名等）
    $envFile = Join-Path $AppDir "ai-config.env"
    if (Test-Path $envFile) {
        Get-Content $envFile -Encoding UTF8 | ForEach-Object {
            if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
                [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
            }
        }
    }

    KLog "starting app"
    $java = Join-Path $AppDir ".tools\jdk\bin\java.exe"
    $runLog = Join-Path $AppDir "run.log"
    Start-Process -FilePath $java `
        -ArgumentList "-Dfile.encoding=UTF-8", "-jar", "target\psych-counseling-1.0.0.jar", "--server.port=8080", "--app.demo-accounts=false" `
        -WorkingDirectory $AppDir `
        -RedirectStandardOutput $runLog -RedirectStandardError ($runLog + ".err") `
        -WindowStyle Hidden
}

function Invoke-Check {
    Invoke-Backup

    if (-not (Test-LocalAlive)) {
        $javaCount = (Get-Process java -ErrorAction SilentlyContinue | Measure-Object).Count
        if ($javaCount -eq 0) {
            KLog "app not running, starting it"
            Start-App
            for ($i = 0; $i -lt 16; $i++) {
                Start-Sleep -Seconds 3
                if (Test-LocalAlive) { KLog "app is up"; break }
            }
        } else {
            KLog "java process exists but 8080 not ready, wait next round"
            return "skip"
        }
    }

    if (-not (Test-LocalAlive)) {
        KLog "app still not responding, skip tunnel"
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
    # 必须把代理清干净：本机会话带着 http_proxy 变量，cpolar 会把本地 8080 的转发
    # 也走代理，表现是「隧道建立成功，但访问返回 404 / i/o timeout」。
    foreach ($k in @("HTTP_PROXY","HTTPS_PROXY","ALL_PROXY","http_proxy","https_proxy","all_proxy")) {
        Remove-Item -Path ("Env:" + $k) -ErrorAction SilentlyContinue
    }
    try { [System.Net.WebRequest]::DefaultWebProxy = New-Object System.Net.WebProxy } catch { }
    # 注意：不能在这里用 -RedirectStandardOutput。父进程退出时管道断开，
    # cpolar 写日志失败会跟着退出（表现为「隧道起来又消失」）。让它自己写文件。
    # 必须用 --log=stdout 加重定向。实测 -log <文件路径> 会让隧道分配异常
    # （日志正常但公网访问一直 404）。重定向依赖父进程存活，所以只适合 -Loop 常驻模式。
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
