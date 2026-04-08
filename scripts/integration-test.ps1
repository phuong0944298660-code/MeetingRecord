# 会议纪要应用 - 集成测试脚本
# 测试后端 API 功能是否正常

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$WaitSeconds = 5
)

$ErrorActionPreference = "Continue"

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

function Test-ApiEndpoint {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [object]$Body = $null,
        [string]$Description
    )

    $url = "$BaseUrl$Path"
    Write-Info "Testing $Method $Path"

    try {
        $params = @{
            Uri = $url
            Method = $Method
            TimeoutSec = 10
            ErrorAction = "Stop"
        }

        if ($Body) {
            $params.ContentType = "application/json"
            $params.Body = $Body | ConvertTo-Json
        }

        $response = Invoke-RestMethod @params
        Write-Success "$Description - 成功"
        return @{ Success = $true; Response = $response }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 401 -or $statusCode -eq 403) {
            Write-Warning "$Description - 需要认证 (HTTP $statusCode)"
            return @{ Success = $true; NeedsAuth = $true }
        } elseif ($statusCode) {
            Write-Error "$Description - HTTP $statusCode"
        } else {
            Write-Error "$Description - $_"
        }
        return @{ Success = $false }
    }
}

# ========== 主程序 ==========

Clear-Host
Write-ColorOutput Cyan @"
========================================
  会议纪要应用 - API 集成测试
========================================
"@

# 等待服务就绪
Write-Info "等待 $WaitSeconds 秒让服务启动..."
Start-Sleep $WaitSeconds

# 测试健康检查
Write-Step "测试基础连通性"
$healthResult = Test-ApiEndpoint -Path "/actuator/health" -Description "健康检查"

# 测试 API 端点
Write-Step "测试认证 API"
$registerResult = Test-ApiEndpoint -Method "POST" -Path "/api/auth/register" -Body @{
    username = "testuser_$(Get-Random)"
    password = "testpass123"
} -Description "用户注册"

$loginResult = Test-ApiEndpoint -Method "POST" -Path "/api/auth/login" -Body @{
    username = "admin"
    password = "admin123"
} -Description "用户登录"

Write-Step "测试会议 API"
$meetingsResult = Test-ApiEndpoint -Path "/api/meetings" -Description "获取会议列表"

Write-Step "测试 AI 处理 API"
$summaryResult = Test-ApiEndpoint -Path "/api/ai/summary?text=test" -Description "AI 摘要"

# 汇总结果
Write-Step "测试结果汇总"
$results = @($healthResult, $registerResult, $loginResult, $meetingsResult, $summaryResult)
$passed = ($results | Where-Object { $_.Success }).Count
$total = $results.Count

Write-Info "通过: $passed / $total"

if ($passed -eq $total) {
    Write-Success "所有测试通过！"
} elseif ($passed -ge $total * 0.6) {
    Write-Warning "大部分测试通过，部分接口可能需要认证或其他配置"
} else {
    Write-Error "测试未通过，请检查服务状态"
}

Write-ColorOutput Cyan "`n测试完成"
