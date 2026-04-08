@echo off
title Meeting Record - 启动所有服务

cd /d "%~dp0\.."

echo ========================================
echo   Meeting Record - 启动所有服务
echo ========================================
echo.

:: 检查环境
echo [检查] Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] Java 未安装
    pause
    exit /b 1
)
echo [OK] Java

echo [检查] Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    if exist "C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin\mvn.cmd" (
        set "PATH=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin;%PATH%"
        echo [OK] Maven 已添加
    ) else (
        echo [错误] Maven 未安装
        pause
        exit /b 1
    )
) else (
    echo [OK] Maven
)

echo [检查] Node.js...
node --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Node.js 未安装
    pause
    exit /b 1
)
echo [OK] Node.js

:: 释放端口
echo [检查] 端口 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    timeout /t 2 /nobreak >nul
)
echo [OK] 端口 8080

echo [检查] 端口 5173...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    timeout /t 2 /nobreak >nul
)
echo [OK] 端口 5173

:: 检查并修复后端
echo [检查] 后端 JAR...
if not exist "backend\target\meeting-record-backend-1.0.0.jar" (
    echo [修复] 构建后端...
    cd backend
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo [错误] 构建失败
        pause
        exit /b 1
    )
    cd ..
    echo [OK] 构建完成
) else (
    echo [OK] JAR 存在
)

:: 检查并修复前端
echo [检查] 前端依赖...
cd frontend
if not exist "node_modules" (
    echo [修复] 安装依赖...
    call npm install
    if errorlevel 1 (
        echo [错误] 安装失败
        cd ..
        pause
        exit /b 1
    )
    echo [OK] 安装完成
) else (
    echo [OK] 依赖存在
)
cd ..

:: 启动服务
echo.
echo ========================================
echo   正在启动所有服务...
echo ========================================
echo.

echo [启动] 后端 (http://localhost:8080)
start "Backend" cmd /k "cd /d %~dp0..\backend && java -jar target\meeting-record-backend-1.0.0.jar"

timeout /t 5 /nobreak >nul

echo [启动] 前端 (http://localhost:5173)
start "Frontend" cmd /k "cd /d %~dp0..\frontend && npm run dev"

echo.
echo ========================================
echo   所有服务已启动！
echo ========================================
echo.
echo [访问地址]
echo   前端: http://localhost:5173
echo   后端: http://localhost:8080
echo.
echo 按任意键关闭此窗口（服务继续运行）
pause >nul
