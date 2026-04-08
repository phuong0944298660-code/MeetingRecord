# 会议纪要应用 - 自动化测试与修复脚本
# 自动诊断并修复常见问题，直到服务正常启动

param(
    [switch]$SkipBuild,
    [switch]$DockerMode
)

$ErrorActionPreference = "Continue"
$script:FixApplied = $false

# 颜色输出函数
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) { Write-Output $args }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Write-Success { Write-ColorOutput Green "[✓] $args" }
function Write-Error { Write-ColorOutput Red "[✗] $args" }
function Write-Warning { Write-ColorOutput Yellow "[!] $args" }
function Write-Info { Write-ColorOutput Cyan "[→] $args" }
function Write-Step { Write-ColorOutput Magenta "`n=== $args ===" }

# 检查环境
function Test-Environment {
    Write-Step "检查环境"

    $issues = @()

    # 检查 Java
    try {
        $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
        if ($javaVersion -match '"(\d+).*"') {
            $majorVersion = [int]$matches[1]
            if ($majorVersion -ge 17) {
                Write-Success "Java 版本: $javaVersion"
            } else {
                Write-Error "Java 版本过低，需要 17+，当前: $javaVersion"
                $issues += "Java版本过低"
            }
        }
    } catch {
        Write-Error "Java 未安装或未添加到 PATH"
        $issues += "Java未安装"
    }

    # 检查 Maven
    try {
        $mvnVersion = mvn -version 2>&1 | Select-String "Apache Maven" | ForEach-Object { $_.ToString() }
        Write-Success "Maven: $mvnVersion"
    } catch {
        Write-Warning "Maven 命令未找到，尝试查找..."
        $mvnPaths = @(
            "C:\ProgramData\chocolatey\lib\maven\apache-maven-*\bin\mvn.cmd",
            "C:\apache-maven-*\bin\mvn.cmd",
            "$env:USERPROFILE\apache-maven-*\bin\mvn.cmd"
        )
        $found = $false
        foreach ($path in $mvnPaths) {
            $matches = Get-ChildItem -Path $path -ErrorAction SilentlyContinue
            if ($matches) {
                $env:PATH = "$($matches[0].DirectoryName);$env:PATH"
                Write-Success "找到 Maven: $($matches[0].FullName)"
                $found = $true
                break
            }
        }
        if (-not $found) {
            Write-Error "Maven 未安装。请运行: choco install maven"
            $issues += "Maven未安装"
        }
    }

    # 检查 Node.js (前端)
    try {
        $nodeVersion = node --version 2>&1
        Write-Success "Node.js: $nodeVersion"
    } catch {
        Write-Warning "Node.js 未安装 (仅影响前端开发)"
    }

    # 检查 Docker
    try {
        $dockerVersion = docker --version 2>&1
        Write-Success "Docker: $dockerVersion"
    } catch {
        Write-Warning "Docker 未安装 (影响容器化部署)"
    }

    return $issues.Count -eq 0
}

# 检查端口占用
function Test-PortConflicts {
    Write-Step "检查端口占用"

    $ports = @(8080, 3306, 6379, 80, 5173)
    $conflicts = @()

    foreach ($port in $ports) {
        $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        if ($connection) {
            $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
            Write-Warning "端口 $port 被占用: $($process.ProcessName) (PID: $($process.Id))"
            $conflicts += @{ Port = $port; Process = $process }
        } else {
            Write-Success "端口 $port 可用"
        }
    }

    if ($conflicts.Count -gt 0) {
        Write-Info "尝试释放冲突端口..."
        foreach ($conflict in $conflicts) {
            if ($conflict.Process.ProcessName -notin @("System", "svchost")) {
                try {
                    Stop-Process -Id $conflict.Process.Id -Force
                    Write-Success "已终止进程 $($conflict.Process.ProcessName)"
                    Start-Sleep 2
                } catch {
                    Write-Error "无法终止进程 $($conflict.Process.ProcessName)，请手动处理"
                }
            }
        }
    }
}

# 修复 pom.xml 版本兼容性问题
function Fix-PomCompatibility {
    Write-Step "检查并修复版本兼容性"

    $pomPath = "..\backend\pom.xml"
    $pomContent = Get-Content $pomPath -Raw

    # 检查 Spring Boot 版本
    $sbPattern = '<version>(3\.[0-9]+\.[0-9]+)</version>'
    if ($pomContent -match $sbPattern) {
        $currentVersion = $matches[1]
        Write-Info "当前 Spring Boot 版本: $currentVersion"

        # 已知兼容的版本组合
        $compatibleVersions = @{
            "spring-boot" = "3.1.12"
            "mybatis-plus" = "3.5.6"
        }

        if ($currentVersion -eq "3.2.4" -or $currentVersion -eq "3.2.0") {
            Write-Warning "检测到不兼容的版本组合: Spring Boot $currentVersion + MyBatis Plus 3.5.6"
            Write-Info "修复: 降级 Spring Boot 到 3.1.12..."

            $pomContent = $pomContent -replace '<version>3\.2\.[0-9]+</version>', "<version>$($compatibleVersions['spring-boot'])</version>"
            Set-Content $pomPath $pomContent -Encoding UTF8

            Write-Success "pom.xml 已更新: Spring Boot $($compatibleVersions['spring-boot'])"
            $script:FixApplied = $true

            # 清理并重新构建
            Write-Info "清理构建缓存..."
            if (Test-Path "..\backend\target") {
                Remove-Item -Recurse -Force "..\backend\target"
            }
            return $true
        } else {
            Write-Success "Spring Boot 版本 $currentVersion 应该兼容"
        }
    }
    return $false
}

# 构建后端
function Build-Backend {
    Write-Step "构建后端"

    Set-Location ..\backend

    try {
        Write-Info "执行 Maven 清理构建..."
        mvn clean package -DskipTests 2>&1 | Tee-Object -Variable buildOutput

        if ($LASTEXITCODE -eq 0) {
            Write-Success "构建成功"
            Set-Location ..\scripts
            return $true
        } else {
            Write-Error "构建失败"
            Set-Location ..\scripts
            return $false
        }
    } catch {
        Write-Error "构建异常: $_"
        Set-Location ..\scripts
        return $false
    }
}

# 测试启动并捕获错误
function Test-Startup {
    Write-Step "测试服务启动"

    $jarPath = "..\backend\target\meeting-record-backend-1.0.0.jar"
    if (-not (Test-Path $jarPath)) {
        Write-Error "找不到 JAR 文件: $jarPath"
        return @{ Success = $false; Error = "JAR_NOT_FOUND" }
    }

    Write-Info "启动服务测试 (最多等待 60 秒)..."

    # 创建临时日志文件
    $logFile = [System.IO.Path]::GetTempFileName()

    # 启动进程
    $process = Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -RedirectStandardOutput $logFile -RedirectStandardError $logFile -PassThru -WindowStyle Hidden

    # 等待并检查日志
    $timeout = 60
    $startTime = Get-Date
    $success = $false
    $errorPattern = $null

    while (((Get-Date) - $startTime).TotalSeconds -lt $timeout) {
        Start-Sleep 2

        # 检查进程是否仍在运行
        if ($process.HasExited) {
            break
        }

        # 读取日志
        if (Test-Path $logFile) {
            $logContent = Get-Content $logFile -Raw -ErrorAction SilentlyContinue

            # 检查成功启动
            if ($logContent -match "Started MeetingRecordApplication") {
                Write-Success "服务启动成功!"
                $success = $true
                break
            }

            # 检查已知错误模式
            if ($logContent -match "Invalid value type for attribute 'factoryBeanObjectType'") {
                $errorPattern = "FACTORY_BEAN_ERROR"
                Write-Error "检测到 MyBatis Plus 兼容性问题"
                break
            }

            if ($logContent -match "Failed to configure a DataSource") {
                $errorPattern = "DATASOURCE_ERROR"
                Write-Error "检测到数据库连接问题"
                break
            }

            if ($logContent -match "Address already in use: bind") {
                $errorPattern = "PORT_IN_USE"
                Write-Error "检测到端口冲突"
                break
            }
        }
    }

    # 终止测试进程
    if (-not $process.HasExited) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }

    # 清理日志文件
    if (Test-Path $logFile) {
        Remove-Item $logFile -Force
    }

    if ($success) {
        return @{ Success = $true }
    } else {
        return @{ Success = $false; Error = $errorPattern }
    }
}

# 修复数据库配置
function Fix-DatabaseConfig {
    Write-Step "修复数据库配置"

    $ymlPath = "..\backend\src\main\resources\application.yml"

    if (-not (Test-Path $ymlPath)) {
        Write-Warning "找不到 application.yml"
        return $false
    }

    $content = Get-Content $ymlPath -Raw

    # 确保使用本地开发配置
    if ($content -match "mysql:3306") {
        Write-Info "修改数据库地址为 localhost..."
        $content = $content -replace "mysql:3306", "localhost:3306"
        $content = $content -replace "redis:", "localhost:"
        Set-Content $ymlPath $content -Encoding UTF8
        Write-Success "数据库配置已更新为本地模式"
        return $true
    }

    return $false
}

# 主修复循环
function Start-AutoFixLoop {
    $maxAttempts = 5
    $attempt = 0

    while ($attempt -lt $maxAttempts) {
        $attempt++
        Write-Step "尝试第 $attempt/$maxAttempts 轮修复"

        # 测试启动
        $result = Test-Startup

        if ($result.Success) {
            Write-Success "服务运行正常!"
            return $true
        }

        # 根据错误类型修复
        switch ($result.Error) {
            "FACTORY_BEAN_ERROR" {
                if (Fix-PomCompatibility) {
                    if (-not (Build-Backend)) {
                        Write-Error "重新构建失败"
                        return $false
                    }
                } else {
                    Write-Error "无法自动修复版本问题，请手动检查 pom.xml"
                    return $false
                }
            }
            "DATASOURCE_ERROR" {
                Write-Warning "数据库未运行，尝试启动 Docker 数据库..."
                try {
                    docker-compose up -d mysql redis 2>&1 | Out-Null
                    Start-Sleep 10
                    Write-Success "数据库容器已启动"
                } catch {
                    Write-Error "无法启动数据库容器，请确保 Docker 运行正常"
                    return $false
                }
            }
            "PORT_IN_USE" {
                Test-PortConflicts
            }
            default {
                Write-Error "未知错误，无法自动修复"
                return $false
            }
        }
    }

    Write-Error "已达到最大尝试次数，无法自动修复"
    return $false
}

# 一键部署函数
function Start-FullDeployment {
    Write-Step "开始一键 Docker 部署"

    Set-Location ..

    try {
        Write-Info "构建并启动所有服务..."
        docker-compose down 2>&1 | Out-Null
        docker-compose up --build -d 2>&1 | Tee-Object -Variable composeOutput

        # 等待服务就绪
        Write-Info "等待服务就绪..."
        Start-Sleep 30

        # 检查容器状态
        $containers = docker-compose ps -q
        $allRunning = $true

        foreach ($container in $containers) {
            $status = docker inspect -f '{{.State.Status}}' $container
            $name = docker inspect -f '{{.Name}}' $container
            if ($status -eq "running") {
                Write-Success "$name 运行中"
            } else {
                Write-Error "$name 未正常运行 (状态: $status)"
                $allRunning = $false
            }
        }

        if ($allRunning) {
            Write-Success "所有服务已启动!"
            Write-Info "访问地址:"
            Write-Info "  前端: http://localhost"
            Write-Info "  后端: http://localhost:8080"
        } else {
            Write-Error "部分服务启动失败，请检查日志: docker-compose logs"
        }
    } catch {
        Write-Error "部署失败: $_"
    }

    Set-Location scripts
}

# ========== 主程序 ==========

Clear-Host
Write-ColorOutput Cyan @"
========================================
  会议纪要应用 - 自动化测试与修复工具
========================================
"@

# 检查环境
if (-not (Test-Environment)) {
    Write-Error "环境检查未通过，请安装缺失的依赖"
    exit 1
}

# 检查端口
Test-PortConflicts

if ($DockerMode) {
    Start-FullDeployment
} else {
    # 本地开发模式
    if (-not $SkipBuild) {
        # 尝试修复并构建
        Fix-PomCompatibility | Out-Null
        if (-not (Build-Backend)) {
            Write-Error "构建失败，终止"
            exit 1
        }
    }

    # 启动自动修复循环
    if (Start-AutoFixLoop) {
        Write-Step "全部完成"
        Write-Success "服务已准备就绪!"
        Write-Info "启动命令: java -jar backend/target/meeting-record-backend-1.0.0.jar"
        Write-Info "或者运行: mvn spring-boot:run -f backend/pom.xml"
    } else {
        Write-Error "自动修复失败，请查看日志或手动排查"
        exit 1
    }
}

Write-ColorOutput Cyan "`n按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
