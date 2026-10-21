@echo off
chcp 936 >nul
title 心屿 - 公网隧道（cpolar）
echo ============================================
echo   心屿 · 公网隧道
echo ============================================
echo.

if not exist "C:/cpolar/cpolar/cpolar.exe" (
    echo [错误] 未找到 cpolar，请先安装
    pause
    exit /b 1
)

echo 正在检查本地服务...
curl -s -o nul --noproxy "*" --max-time 3 http://localhost:8080/api/public/health
if errorlevel 1 (
    echo [提示] 本地 8080 没有响应，请先启动「启动系统.bat」
    echo.
)

echo 正在建立隧道，请稍候...
echo 下面的 https 地址就是别人可以访问的网址。
echo 关闭此窗口即断开隧道。
echo ============================================
echo.

"C:/cpolar/cpolar/cpolar.exe" http 8080 --log=stdout

pause
