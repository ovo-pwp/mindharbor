@echo off
chcp 936 >nul
title 心屿 - 开发模式

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%.tools\jdk"
set "MVN=%ROOT%.tools\maven\apache-maven-3.9.9\bin\mvn.cmd"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%ROOT%"

echo 启动后端（8080）...
start "心屿后端" cmd /k "%MVN% spring-boot:run"

timeout /t 3 >nul

echo 启动前端（5173）...
cd /d "%ROOT%frontend"
if not exist node_modules (
    echo 首次运行，正在安装前端依赖...
    call npm install
)
start "心屿前端" cmd /k "npm run dev"

timeout /t 5 >nul
start http://localhost:5173
