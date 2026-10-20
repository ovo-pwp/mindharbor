@echo off
chcp 936 >nul
title 心屿 - AI 心理健康咨询系统

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%.tools\jdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%ROOT%"

echo ============================================
echo   心屿 · AI 心理健康咨询系统
echo ============================================
echo.

if not exist "%ROOT%target\psych-counseling-1.0.0.jar" (
    echo [错误] 未找到 jar 包，请先执行构建：
    echo        .tools\maven\apache-maven-3.9.9\bin\mvn.cmd clean package -DskipTests
    pause
    exit /b 1
)

echo 正在启动，首次启动需要初始化数据库，请稍候...
echo 启动后请用浏览器打开：http://localhost:8080
echo.
echo 演示账号（密码均为 123456）：
echo   患者    zhangxiaoyu
echo   咨询师  liyisheng
echo   管理员  admin
echo.

rem 读取 AI 接入配置（可选）：不配则自动降级为本地知识库 + 技能模式
if exist "%ROOT%ai-config.env" (
    for /f "usebackq tokens=1,* delims==" %%a in (`findstr /v /b "#" "%ROOT%ai-config.env"`) do (
        if not "%%a"=="" set "%%a=%%b"
    )
    echo [AI] 已加载 ai-config.env，将连接真实大模型
) else (
    echo [AI] 未找到 ai-config.env，将使用本地知识库 + 技能模式
)
echo.
echo 关闭此窗口即停止服务。
echo ============================================

"%JAVA_HOME%\bin\java" -Dfile.encoding=UTF-8 -jar "%ROOT%target\psych-counseling-1.0.0.jar" --server.port=8080

pause
