#!/usr/bin/env bash

################################################################################
# PreCI 卸载脚本
#
# 功能说明:
#   - 停止 PreCI 服务
#   - 清理环境变量配置
#   - 删除安装目录及所有文件
#   - 支持 Linux/macOS (ARM64/AMD64) 环境
#
# 使用方法:
#   ./uninstall.sh [安装目录]
#
# 作者: PreCI Team
################################################################################

set -e  # 遇到错误立即退出
set -u  # 使用未定义变量时报错

################################################################################
# 颜色和样式定义
################################################################################

# 检测是否支持彩色输出
if [[ -t 1 ]] && command -v tput >/dev/null 2>&1; then
    RED=$(tput setaf 1)
    GREEN=$(tput setaf 2)
    YELLOW=$(tput setaf 3)
    BLUE=$(tput setaf 4)
    BOLD=$(tput bold)
    RESET=$(tput sgr0)
else
    RED=""
    GREEN=""
    YELLOW=""
    BLUE=""
    BOLD=""
    RESET=""
fi

################################################################################
# 日志输出函数
################################################################################

log_info() {
    echo "${GREEN}[INFO]${RESET} $*"
}

log_warn() {
    echo "${YELLOW}[WARN]${RESET} $*"
}

log_error() {
    echo "${RED}[ERROR]${RESET} $*" >&2
}

log_success() {
    echo "${GREEN}[SUCCESS]${RESET} $*"
}

print_header() {
    echo ""
    echo "${BOLD}${BLUE}============================================${RESET}"
    echo "${BOLD}${BLUE}$*${RESET}"
    echo "${BOLD}${BLUE}============================================${RESET}"
    echo ""
}

################################################################################
# 全局变量
################################################################################

# 获取脚本所在目录（即安装目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 默认卸载目录（脚本所在目录）
DEFAULT_UNINSTALL_DIR="$SCRIPT_DIR"

# 实际卸载目录（可通过参数指定）
UNINSTALL_DIR=""

################################################################################
# Shell 配置文件检测
################################################################################

detect_shell_configs() {
    local configs=()

    # 常见的 shell 配置文件
    local possible_configs=(
        "${HOME}/.bashrc"
        "${HOME}/.bash_profile"
        "${HOME}/.zshrc"
        "${HOME}/.profile"
    )

    # 只返回存在的配置文件
    for config in "${possible_configs[@]}"; do
        if [[ -f "$config" ]]; then
            configs+=("$config")
        fi
    done

    printf '%s\n' "${configs[@]}"
}

################################################################################
# 获取卸载目录
################################################################################

get_uninstall_directory() {
    # 如果通过命令行参数指定了目录
    if [[ $# -gt 0 ]]; then
        UNINSTALL_DIR="$1"
    else
        # 默认使用脚本所在目录
        UNINSTALL_DIR="$DEFAULT_UNINSTALL_DIR"
    fi

    # 展开 ~ 为实际的 HOME 目录
    UNINSTALL_DIR="${UNINSTALL_DIR/#\~/$HOME}"

    # 转换为绝对路径
    if [[ -d "$UNINSTALL_DIR" ]]; then
        UNINSTALL_DIR="$(cd "$UNINSTALL_DIR" && pwd)"
    fi

    log_info "卸载目录: ${UNINSTALL_DIR}"
}

################################################################################
# 验证卸载目录
################################################################################

validate_uninstall_directory() {
    if [[ ! -d "$UNINSTALL_DIR" ]]; then
        log_error "目录不存在: ${UNINSTALL_DIR}"
        exit 1
    fi

    # 检查是否是 PreCI 安装目录（通过检查关键文件）
    local is_preci_dir=false

    if [[ -f "${UNINSTALL_DIR}/preci" ]] || [[ -f "${UNINSTALL_DIR}/preci-server" ]] || [[ -f "${UNINSTALL_DIR}/preci-mcp" ]]; then
        is_preci_dir=true
    fi

    if [[ "$is_preci_dir" == false ]]; then
        log_warn "目录 ${UNINSTALL_DIR} 似乎不是 PreCI 安装目录"
        read -p "是否继续卸载? [y/N]: " confirm
        [[ ! "$confirm" =~ ^[yY]$ ]] && exit 0
    fi
}

################################################################################
# 确认卸载
################################################################################

confirm_uninstall() {
    print_header "PreCI 卸载确认"

    echo "${YELLOW}警告: 此操作将:${RESET}"
    echo "  1. 停止 PreCI 服务"
    echo "  2. 删除目录: ${BOLD}${UNINSTALL_DIR}${RESET}"
    echo "  3. 清理环境变量配置"
    echo ""
    echo "${RED}此操作不可恢复!${RESET}"
    echo ""

    read -p "确认卸载 PreCI? [y/N]: " confirm

    case "$confirm" in
        [yY]|[yY][eE][sS])
            log_info "开始卸载..."
            ;;
        *)
            log_info "卸载已取消"
            exit 0
            ;;
    esac
}

################################################################################
# 停止 PreCI 服务
################################################################################

stop_preci_service() {
    log_info "停止 PreCI 服务..."

    local preci_bin="${UNINSTALL_DIR}/preci"

    if [[ -x "$preci_bin" ]]; then
        # 尝试停止服务
        if "$preci_bin" server stop 2>/dev/null; then
            log_success "PreCI 服务已停止"
        else
            log_warn "PreCI 服务可能未运行或停止失败"
        fi
    else
        log_warn "未找到 preci 可执行文件，跳过服务停止"
    fi

    # 等待一下确保进程完全退出
    sleep 1
}

################################################################################
# 清理环境变量
################################################################################

clean_environment() {
    log_info "清理环境变量..."

    # 兼容 Bash 3.2+ (macOS/Linux 所有版本) 的数组读取方式
    local configs=()
    local line
    while IFS= read -r line; do
        [[ -n "$line" ]] && configs+=("$line")
    done < <(detect_shell_configs)

    if [[ ${#configs[@]} -eq 0 ]]; then
        log_warn "未找到 shell 配置文件"
        return 0
    fi

    local cleaned=false

    for config in "${configs[@]}"; do
        # 检查配置文件中是否包含 PreCI 环境变量
        if grep -q "# PreCI Environment" "$config" 2>/dev/null; then
            log_info "清理配置文件: ${config}"

            # 创建备份
            cp "$config" "${config}.preci.backup"
            log_info "已创建备份: ${config}.preci.backup"

            # 删除 PreCI 相关的环境变量配置
            # 使用 sed 删除从 "# PreCI Environment" 开始到下一个空行或文件末尾的所有行
            if [[ "$(uname -s)" == "Darwin" ]]; then
                # macOS 的 sed 需要不同的参数
                sed -i '' '/# PreCI Environment/,/^$/d' "$config"
            else
                # Linux 的 sed
                sed -i '/# PreCI Environment/,/^$/d' "$config"
            fi

            log_success "已清理: ${config}"
            cleaned=true
        fi
    done

    if [[ "$cleaned" == false ]]; then
        log_info "未找到需要清理的环境变量配置"
    else
        echo ""
        log_warn "环境变量已清理，请运行以下命令使更改生效:"
        for config in "${configs[@]}"; do
            if [[ -f "$config" ]]; then
                echo "  ${BOLD}source ${config}${RESET}"
            fi
        done
        echo "或者重新打开终端窗口"
    fi
}

################################################################################
# 删除安装目录
################################################################################

remove_installation() {
    log_info "删除安装目录..."

    if [[ ! -d "$UNINSTALL_DIR" ]]; then
        log_warn "目录不存在: ${UNINSTALL_DIR}"
        return 0
    fi

    # 安全检查：确保不会删除重要目录
    case "$UNINSTALL_DIR" in
        /|/bin|/usr|/usr/bin|/usr/local|/etc|/var|/home|/root)
            log_error "拒绝删除系统目录: ${UNINSTALL_DIR}"
            exit 1
            ;;
        "$HOME"|"$HOME/")
            log_error "拒绝删除用户主目录: ${UNINSTALL_DIR}"
            exit 1
            ;;
    esac

    # 如果脚本在要删除的目录中，先切换到上级目录
    if [[ "$PWD" == "$UNINSTALL_DIR"* ]]; then
        cd "$HOME"
    fi

    # 删除目录
    if rm -rf "$UNINSTALL_DIR"; then
        log_success "已删除: ${UNINSTALL_DIR}"
    else
        log_error "删除目录失败: ${UNINSTALL_DIR}"
        exit 1
    fi
}

################################################################################
# 打印卸载完成信息
################################################################################

print_uninstall_info() {
    print_header "PreCI 卸载完成"

    echo "${GREEN}PreCI 已成功卸载${RESET}"
    echo ""
    echo "${BOLD}后续步骤:${RESET}"
    echo "  1. 重新加载 shell 配置或重启终端"
    echo "  2. 如需恢复配置文件，可使用备份文件 (*.preci.backup)"
    echo ""

    print_header "感谢使用 PreCI"
}

################################################################################
# 清理函数（错误时调用）
################################################################################

cleanup_on_error() {
    log_error "卸载过程中发生错误"
    log_info "部分文件可能未被清理，请手动检查"
    exit 1
}

################################################################################
# 主函数
################################################################################

main() {
    # 设置错误处理
    trap cleanup_on_error ERR

    print_header "PreCI 卸载程序"

    # 获取卸载目录
    get_uninstall_directory "$@"

    # 验证卸载目录
    validate_uninstall_directory

    # 确认卸载
    confirm_uninstall

    # 停止 PreCI 服务
    stop_preci_service

    # 清理环境变量
    clean_environment

    # 删除安装目录
    remove_installation

    # 打印卸载完成信息
    print_uninstall_info
}

################################################################################
# 脚本入口
################################################################################

# 检查是否以 root 身份运行（不推荐）
if [[ $EUID -eq 0 ]]; then
    log_warn "不建议以 root 身份运行此脚本"
    read -p "是否继续? [y/N]: " confirm
    [[ ! "$confirm" =~ ^[yY]$ ]] && exit 1
fi

# 执行主函数
main "$@"