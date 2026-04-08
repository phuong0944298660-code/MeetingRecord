#!/bin/bash

# 会议纪要应用 - 自动化测试与修复脚本 (Linux/macOS)
# 自动诊断并修复常见问题，直到服务正常启动

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

FIX_APPLIED=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 输出函数
success() { echo -e "${GREEN}[✓] $1${NC}"; }
error() { echo -e "${RED}[✗] $1${NC}"; }
warning() { echo -e "${YELLOW}[!] $1${NC}"; }
info() { echo -e "${CYAN}[→] $1${NC}"; }
step() { echo -e "\n${MAGENTA}=== $1 ===${NC}"; }

# 检查环境
check_environment() {
    step "检查环境"

    local issues=()

    # 检查 Java
    if command -v java >/dev/null 2>&1; then
        java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        major_version=$(echo "$java_version" | cut -d. -f1)
        if [ "$major_version" -ge 17 ] 2>/dev/null || [ "${java_version%%.*}" -ge 17 ] 2>/dev/null; then
            success "Java 版本: $java_version"
        else
            error "Java 版本过低，需要 17+"
            issues+=("java_version")
        fi
    else
        error "Java 未安装"
        issues+=("java")
    fi

    # 检查 Maven
    if command -v mvn >/dev/null 2>&1; then
        mvn_version=$(mvn -version 2>&1 | head -1)
        success "Maven: $mvn_version"
    else
        error "Maven 未安装"
        issues+=("maven")
    fi

    # 检查 Node.js
    if command -v node >/dev/null 2>&1; then
        node_version=$(node --version)
        success "Node.js: $node_version"
    else
        warning "Node.js 未安装"
    fi

    # 检查 Docker
    if command -v docker >/dev/null 2>&1; then
        docker_version=$(docker --version)
        success "Docker: $docker_version"
    else
        warning "Docker 未安装"
    fi

    [ ${#issues[@]} -eq 0 ]
}

# 检查端口占用
check_ports() {
    step "检查端口占用"

    local ports=(8080 3306 6379)
    local conflicts=()

    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            pid=$(lsof -Pi :$port -sTCP:LISTEN -t | head -1)
            proc=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
            warning "端口 $port 被占用: $proc (PID: $pid)"
            conflicts+=("$port:$pid")
        else
            success "端口 $port 可用"
        fi
    done

    if [ ${#conflicts[@]} -gt 0 ]; then
        info "尝试释放冲突端口..."
        for conflict in "${conflicts[@]}"; do
            pid=$(echo "$conflict" | cut -d: -f2)
            kill -9 "$pid" 2>/dev/null && success "已终止 PID $pid" || warning "无法终止 PID $pid"
        done
        sleep 2
    fi
}

# 修复 pom.xml
fix_pom() {
    step "检查并修复版本兼容性"

    local pom_path="$PROJECT_DIR/backend/pom.xml"

    if [ ! -f "$pom_path" ]; then
        error "找不到 pom.xml"
        return 1
    fi

    # 检查 Spring Boot 版本
    current_version=$(grep -oP '(?<=<version>)(3\.[0-9]+\.[0-9]+)(?=</version>)' "$pom_path" | head -1)
    info "当前 Spring Boot 版本: $current_version"

    if [[ "$current_version" == "3.2."* ]] || [[ "$current_version" == "3.2.0" ]]; then
        warning "检测到不兼容版本: $current_version"
        info "修复: 降级 Spring Boot 到 3.1.12..."

        sed -i 's/<version>3\.2\.[0-9]<\/version>/<version>3.1.12<\/version>/g' "$pom_path"

        success "pom.xml 已更新"
        FIX_APPLIED=true

        # 清理构建
        info "清理构建缓存..."
        rm -rf "$PROJECT_DIR/backend/target"
        return 0
    fi

    success "版本兼容"
    return 1
}

# 构建后端
build_backend() {
    step "构建后端"

    cd "$PROJECT_DIR/backend"

    info "执行 Maven 清理构建..."
    if mvn clean package -DskipTests; then
        success "构建成功"
        cd "$SCRIPT_DIR"
        return 0
    else
        error "构建失败"
        cd "$SCRIPT_DIR"
        return 1
    fi
}

# 测试启动
test_startup() {
    step "测试服务启动"

    local jar_path="$PROJECT_DIR/backend/target/meeting-record-backend-1.0.0.jar"

    if [ ! -f "$jar_path" ]; then
        error "找不到 JAR 文件"
        echo "JAR_NOT_FOUND"
        return 1
    fi

    info "启动服务测试 (最多等待 60 秒)..."

    local log_file=$(mktemp)
    java -jar "$jar_path" > "$log_file" 2>&1 &
    local pid=$!

    local success=false
    local error_pattern=""

    for i in {1..30}; do
        sleep 2

        if ! kill -0 $pid 2>/dev/null; then
            break
        fi

        if grep -q "Started MeetingRecordApplication" "$log_file" 2>/dev/null; then
            success=true
            break
        fi

        if grep -q "Invalid value type for attribute 'factoryBeanObjectType'" "$log_file" 2>/dev/null; then
            error_pattern="FACTORY_BEAN_ERROR"
            error "检测到 MyBatis Plus 兼容性问题"
            break
        fi

        if grep -q "Failed to configure a DataSource" "$log_file" 2>/dev/null; then
            error_pattern="DATASOURCE_ERROR"
            error "检测到数据库连接问题"
            break
        fi
    done

    # 终止测试进程
    kill -9 $pid 2>/dev/null || true
    rm -f "$log_file"

    if [ "$success" = true ]; then
        success "服务启动成功!"
        return 0
    else
        if [ -n "$error_pattern" ]; then
            echo "$error_pattern"
        fi
        return 1
    fi
}

# 自动修复循环
autofix_loop() {
    local max_attempts=5
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        attempt=$((attempt + 1))
        step "尝试第 $attempt/$max_attempts 轮修复"

        # 尝试修复 pom
        fix_pom

        # 重新构建
        if [ "$FIX_APPLIED" = true ]; then
            if ! build_backend; then
                error "构建失败"
                return 1
            fi
        fi

        # 测试启动
        if test_startup; then
            success "服务运行正常!"
            return 0
        fi

        FIX_APPLIED=false
    done

    error "已达到最大尝试次数"
    return 1
}

# Docker 部署
docker_deploy() {
    step "开始一键 Docker 部署"

    cd "$PROJECT_DIR"

    info "构建并启动所有服务..."
    docker-compose down 2>/dev/null || true
    docker-compose up --build -d

    info "等待服务就绪..."
    sleep 30

    local all_running=true
    for container in $(docker-compose ps -q); do
        local status=$(docker inspect -f '{{.State.Status}}' "$container")
        local name=$(docker inspect -f '{{.Name}}' "$container")
        if [ "$status" = "running" ]; then
            success "$name 运行中"
        else
            error "$name 未正常运行"
            all_running=false
        fi
    done

    if [ "$all_running" = true ]; then
        success "所有服务已启动!"
        info "访问地址:"
        info "  前端: http://localhost"
        info "  后端: http://localhost:8080"
    else
        error "部分服务启动失败"
        docker-compose logs
    fi

    cd "$SCRIPT_DIR"
}

# ========== 主程序 ==========

clear
echo "========================================"
echo "  会议纪要应用 - 自动化测试与修复工具"
echo "========================================"

# 解析参数
DOCKER_MODE=false
SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --docker)
            DOCKER_MODE=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        *)
            shift
            ;;
    esac
done

if [ "$DOCKER_MODE" = true ]; then
    docker_deploy
else
    # 检查环境
    if ! check_environment; then
        error "环境检查未通过"
        exit 1
    fi

    # 检查端口
    check_ports

    # 自动修复循环
    if autofix_loop; then
        step "全部完成"
        success "服务已准备就绪!"
        info "启动命令: java -jar backend/target/meeting-record-backend-1.0.0.jar"
    else
        error "自动修复失败"
        exit 1
    fi
fi
