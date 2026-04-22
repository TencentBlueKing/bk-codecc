#!/usr/bin/env bash

################################################################################
# PreCI 安装脚本
#
# 流程:
#   1. 先请用户确定安装目录（默认: ~/PreCI）
#   2. 先找出环境变量中, 除了安装目录以外, 所有包含 /PreCI 的目录, 询问用户是否要删除这些环境变量, 若用户选是, 则删除; 否则不管它们
#   3. 如果目标安装目录中已经有文件, 先停止 PreCI 服务，然后只清理需要安装的文件（保留 log 目录，避免被 IDE 插件占用的日志文件导致安装失败）
#   4. 如果需要，则创建目标安装目录, 并设置权限
#   5. 将 PreCI 相关文件复制到目标目录 (已在 FILES_TO_COPY 和 DIRS_TO_CREATE 中指定)
#   6. 设置环境变量
#
# 使用方法:
#   ./install.sh [安装目录]
#
# 作者: PreCI Team
################################################################################

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

# 获取脚本所在目录（即 bin 目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 默认安装目录
DEFAULT_INSTALL_DIR="${HOME}/PreCI"

# 实际安装目录（可通过参数指定）
INSTALL_DIR=""

# 需要拷贝的文件和目录列表
FILES_TO_COPY=(
    "preci"
    "preci-server"
    "preci-mcp"
    "config"
    "checkerset"
    "install.sh"
    "uninstall.sh"
    "uninstall_old_preci.sh"
)

# 需要创建的空目录
DIRS_TO_CREATE=(
    "db"
    "log"
    "tool"
)

################################################################################
# 系统检测函数
################################################################################

detect_os() {
    local os_type=""
    local arch=""

    # 检测操作系统
    case "$(uname -s)" in
        Linux*)
            os_type="Linux"
            ;;
        Darwin*)
            os_type="macOS"
            ;;
        *)
            log_error "不支持的操作系统: $(uname -s)"
            exit 1
            ;;
    esac

    # 检测架构
    case "$(uname -m)" in
        x86_64|amd64)
            arch="AMD64"
            ;;
        arm64|aarch64)
            arch="ARM64"
            ;;
        *)
            log_warn "未识别的架构: $(uname -m)"
            arch="$(uname -m)"
            ;;
    esac

    log_info "检测到系统: ${os_type} ${arch}"
}

################################################################################
# Shell 配置文件检测
################################################################################

detect_shell_rc() {
    local shell_rc=""

    # 通过 $SHELL 环境变量检测用户的默认 shell
    local user_shell="${SHELL##*/}"  # 提取 shell 名称（去掉路径）
    
    case "$user_shell" in
        zsh)
            shell_rc="${HOME}/.zshrc"
            ;;
        bash)
            # macOS 优先使用 .bash_profile
            if [[ "$(uname -s)" == "Darwin" ]] && [[ -f "${HOME}/.bash_profile" ]]; then
                shell_rc="${HOME}/.bash_profile"
            else
                shell_rc="${HOME}/.bashrc"
            fi
            ;;
        *)
            # 如果无法从 $SHELL 判断，则回退到检测当前运行环境
            if [[ -n "${ZSH_VERSION:-}" ]]; then
                shell_rc="${HOME}/.zshrc"
            elif [[ -n "${BASH_VERSION:-}" ]]; then
                if [[ "$(uname -s)" == "Darwin" ]] && [[ -f "${HOME}/.bash_profile" ]]; then
                    shell_rc="${HOME}/.bash_profile"
                else
                    shell_rc="${HOME}/.bashrc"
                fi
            else
                # 最后的默认选项
                shell_rc="${HOME}/.profile"
            fi
            ;;
    esac

    # 如果配置文件不存在，创建它
    if [[ ! -f "$shell_rc" ]]; then
        touch "$shell_rc"
        log_info "创建配置文件: $shell_rc"
    fi

    echo "$shell_rc"
}

################################################################################
# 文件验证函数
################################################################################

validate_source_files() {
    log_info "验证源文件..."

    local missing_files=()

    for file in "${FILES_TO_COPY[@]}"; do
        if [[ ! -e "${SCRIPT_DIR}/${file}" ]]; then
            missing_files+=("$file")
        fi
    done

    if [[ ${#missing_files[@]} -gt 0 ]]; then
        log_error "以下必需文件缺失:"
        for file in "${missing_files[@]}"; do
            echo "  - $file"
        done
        exit 1
    fi

    log_info "源文件验证通过"
}

################################################################################
# 获取安装目录
################################################################################

get_install_directory() {
    # 如果通过命令行参数指定了目录
    if [[ $# -gt 0 ]]; then
        INSTALL_DIR="$1"
    else
        # 交互式询问用户
        echo ""
        read -p "请输入安装目录 [默认: ${DEFAULT_INSTALL_DIR}]: " user_input

        if [[ -n "$user_input" ]]; then
            INSTALL_DIR="$user_input"
        else
            INSTALL_DIR="$DEFAULT_INSTALL_DIR"
        fi
    fi

    # 展开 ~ 为实际的 HOME 目录
    INSTALL_DIR="${INSTALL_DIR/#\~/$HOME}"

    # 转换为绝对路径
    if [[ -d "$(dirname "$INSTALL_DIR")" ]]; then
        INSTALL_DIR="$(cd "$(dirname "$INSTALL_DIR")" && pwd)/$(basename "$INSTALL_DIR")"
    fi

    log_info "安装目录: ${INSTALL_DIR}"
}

################################################################################
# 检查环境变量中的其他 PreCI 目录
################################################################################

check_old_preci_env() {
    log_info "检查环境变量中的其他 PreCI 安装..."

    local shell_rc
    shell_rc=$(detect_shell_rc)

    # 查找所有包含 /PreCI 的环境变量路径
    local old_preci_paths=()
    
    # 从 shell 配置文件中查找
    if [[ -f "$shell_rc" ]]; then
        while IFS= read -r line; do
            # 匹配包含 PreCI 的路径
            if [[ "$line" =~ PRECI_HOME=\"([^\"]+)\" ]] || [[ "$line" =~ PRECI_HOME=([^[:space:]]+) ]]; then
                local path="${BASH_REMATCH[1]}"
                # 展开 ~ 为实际路径
                path="${path/#\~/$HOME}"
                # 排除当前安装目录
                if [[ "$path" != "$INSTALL_DIR" ]] && [[ "$path" =~ PreCI ]]; then
                    old_preci_paths+=("$path")
                fi
            fi
        done < "$shell_rc"
    fi

    # 从当前 PATH 中查找
    IFS=':' read -ra PATH_ARRAY <<< "$PATH"
    for path in "${PATH_ARRAY[@]}"; do
        if [[ "$path" =~ PreCI ]] && [[ "$path" != "$INSTALL_DIR" ]]; then
            # 检查是否已在列表中
            local found=false
            if [[ ${#old_preci_paths[@]} -gt 0 ]]; then
                for existing in "${old_preci_paths[@]}"; do
                    if [[ "$existing" == "$path" ]]; then
                        found=true
                        break
                    fi
                done
            fi
            if [[ "$found" == false ]]; then
                old_preci_paths+=("$path")
            fi
        fi
    done

    # 如果找到了其他 PreCI 路径，提示用户手动清理
    if [[ ${#old_preci_paths[@]} -gt 0 ]]; then
        echo ""
        log_warn "检测到环境变量中包含其他 PreCI 路径:"
        for path in "${old_preci_paths[@]}"; do
            echo "  - $path"
        done
        echo ""
        log_warn "${BOLD}建议手动清理旧的 PreCI 环境变量:${RESET}"
        echo "  1. 编辑 Shell 配置文件: ${BLUE}${shell_rc}${RESET}"
        echo "  2. 删除包含旧 PreCI 路径的以下内容:"
        echo "     - ${YELLOW}PRECI_HOME${RESET} 相关的行"
        echo "     - ${YELLOW}PATH${RESET} 中包含旧 PreCI 路径的部分"
        echo "     - 以 ${YELLOW}# PreCI Environment${RESET} 开头的注释块"
        echo "  3. 保存文件并重新加载: ${BLUE}source ${shell_rc}${RESET}"
        echo ""
        read -p "是否继续安装? [y/N]: " confirm
        if [[ ! "$confirm" =~ ^[yY]$ ]]; then
            log_info "安装已取消"
            exit 0
        fi
    else
        log_info "未发现其他 PreCI 安装路径"
    fi
}

################################################################################
# 准备安装目录
################################################################################

prepare_install_directory() {
    # 检查目录是否存在且不为空
    if [[ -d "$INSTALL_DIR" ]] && [[ -n "$(ls -A "$INSTALL_DIR" 2>/dev/null)" ]]; then
        log_warn "目录 ${INSTALL_DIR} 已存在且不为空"

        # 交互式确认
        read -p "是否覆盖安装? [y/N]: " confirm

        if [[ ! "$confirm" =~ ^[yY]$ ]]; then
            log_info "安装已取消"
            exit 0
        fi

        # 停止 PreCI 服务（避免可执行文件被占用）
        log_info "尝试停止 PreCI 服务..."
        if [[ -x "${INSTALL_DIR}/preci" ]]; then
            "${INSTALL_DIR}/preci" server --stop 2>/dev/null || true
        elif command -v preci >/dev/null 2>&1; then
            preci server --stop 2>/dev/null || true
        fi

        # 查找并杀死 preci-server 进程
        local preci_pids
        preci_pids=$(pgrep -f "preci-server" 2>/dev/null || true)
        if [[ -n "$preci_pids" ]]; then
            log_info "发现 PreCI 进程，正在停止..."
            echo "$preci_pids" | xargs kill -15 2>/dev/null || true
            sleep 2
            # 如果还在运行，强制杀死
            preci_pids=$(pgrep -f "preci-server" 2>/dev/null || true)
            if [[ -n "$preci_pids" ]]; then
                echo "$preci_pids" | xargs kill -9 2>/dev/null || true
            fi
            log_success "PreCI 进程已停止"
        fi

        # 只清理需要安装的文件，保留 log 目录（避免被 IDE 插件占用的日志文件导致安装失败）
        log_info "清理旧的安装文件..."
        for file in "${FILES_TO_COPY[@]}"; do
            local target_path="${INSTALL_DIR}/${file}"
            if [[ -e "$target_path" ]]; then
                if rm -rf "$target_path" 2>/dev/null; then
                    log_info "已清理: ${file}"
                else
                    log_warn "无法清理 ${file}，将尝试覆盖"
                fi
            fi
        done
        
        # 清理 DIRS_TO_CREATE 中除 log 以外的目录
        for dir in "${DIRS_TO_CREATE[@]}"; do
            if [[ "$dir" == "log" ]]; then continue; fi
            local target_path="${INSTALL_DIR}/${dir}"
            if [[ -d "$target_path" ]]; then
                if rm -rf "$target_path" 2>/dev/null; then
                    log_info "已清理目录: ${dir}"
                else
                    log_warn "无法清理目录 ${dir}，将尝试覆盖"
                fi
            fi
        done
        log_success "旧文件清理完成"
    fi

    # 创建安装目录
    if [[ ! -d "$INSTALL_DIR" ]]; then
        log_info "创建安装目录: ${INSTALL_DIR}"
        if ! mkdir -p "$INSTALL_DIR"; then
            log_error "无法创建目录: ${INSTALL_DIR}"
            exit 1
        fi
    fi
    
    # 设置目录权限
    chmod 755 "$INSTALL_DIR"
}

################################################################################
# 拷贝文件
################################################################################

copy_files() {
    log_info "开始拷贝文件..."

    local total_files=${#FILES_TO_COPY[@]}
    local current=0

    for file in "${FILES_TO_COPY[@]}"; do
        current=$((current + 1))
        local source="${SCRIPT_DIR}/${file}"
        local target="${INSTALL_DIR}/${file}"

        if [[ -e "$source" ]]; then
            log_info "[${current}/${total_files}] 拷贝: ${file}"

            if [[ -d "$source" ]]; then
                # 拷贝目录
                cp -r "$source" "$target"
            else
                # 拷贝文件
                cp "$source" "$target"

                # 如果是可执行文件，设置执行权限
                if [[ "$file" == "preci" ]] || [[ "$file" == "preci-server" ]] || [[ "$file" == "preci-mcp" ]]; then
                    chmod +x "$target"
                fi
            fi
        else
            log_warn "跳过不存在的文件: ${file}"
        fi
    done

    log_success "文件拷贝完成"
}

################################################################################
# 创建必要的目录
################################################################################

create_directories() {
    log_info "创建必要的目录..."

    for dir in "${DIRS_TO_CREATE[@]}"; do
        local target="${INSTALL_DIR}/${dir}"
        if [[ ! -d "$target" ]]; then
            mkdir -p "$target"
            log_info "创建目录: ${dir}"
        fi
    done
}

################################################################################
# 设置环境变量
################################################################################

setup_environment() {
    local shell_rc
    shell_rc=$(detect_shell_rc)

    log_info "配置环境变量..."

    # 检查是否已经设置过环境变量
    if grep -q "# PreCI Environment" "$shell_rc" 2>/dev/null; then
        log_info "环境变量已配置，跳过"
        return 0
    fi

    # 添加环境变量配置
    cat >> "$shell_rc" << EOF

# PreCI Environment
# Added by PreCI installer on $(date '+%Y-%m-%d %H:%M:%S')
export PRECI_HOME="${INSTALL_DIR}"
export PATH="\${PRECI_HOME}:\${PATH}"
EOF

    log_success "环境变量已添加到: ${shell_rc}"

    # 提示用户如何使环境变量生效
    echo ""
    log_warn "请运行以下命令使环境变量立即生效:"
    echo "  ${BOLD}source ${shell_rc}${RESET}"
    echo "或者重新打开终端窗口"
}

################################################################################
# 验证安装
################################################################################

verify_installation() {
    log_info "验证安装..."

    local all_ok=true

    # 检查可执行文件
    for exe in "preci" "preci-server" "preci-mcp"; do
        if [[ -x "${INSTALL_DIR}/${exe}" ]]; then
            log_info "✓ ${exe} 已安装"
        else
            log_error "✗ ${exe} 未找到或不可执行"
            all_ok=false
        fi
    done

    if [[ "$all_ok" == true ]]; then
        log_success "安装验证通过"
        return 0
    else
        log_error "安装验证失败"
        return 1
    fi
}

################################################################################
# 打印安装信息
################################################################################

print_installation_info() {
    print_header "PreCI 安装完成"

    echo "${GREEN}安装目录:${RESET} ${INSTALL_DIR}"
    echo ""
    echo "${BOLD}快速开始:${RESET}"
    echo "  1. 使环境变量生效:"
    echo "     ${BLUE}source $(detect_shell_rc)${RESET}"
    echo ""
    echo "  2. 验证安装:"
    echo "     ${BLUE}preci version${RESET}"
    echo ""
    echo "  3. 查看帮助:"
    echo "     ${BLUE}preci --help${RESET}"
    echo ""
    echo "${BOLD}卸载方法:${RESET}"
    echo "  运行卸载脚本:"
    echo "     ${BLUE}${INSTALL_DIR}/uninstall.sh${RESET}"
    echo ""
    echo "  或手动删除:"
    echo "     ${BLUE}rm -rf ${INSTALL_DIR}${RESET}"
    echo "     然后从 $(detect_shell_rc) 中删除 PreCI 相关的环境变量"
    echo ""

    print_header "感谢使用 PreCI"
}

################################################################################
# 清理函数（错误时调用）
################################################################################

cleanup_on_error() {
    local exit_code=$?
    log_error "安装过程中发生错误 (退出码: ${exit_code})"

    if [[ -n "${INSTALL_DIR:-}" ]] && [[ -d "$INSTALL_DIR" ]]; then
        read -p "是否删除已创建的安装目录? [y/N]: " confirm
        if [[ "$confirm" =~ ^[yY]$ ]]; then
            rm -rf "$INSTALL_DIR"
            log_info "已删除: ${INSTALL_DIR}"
        fi
    fi

    exit 1
}

################################################################################
# 主函数
################################################################################

main() {
    print_header "PreCI 安装程序"

    # 系统检测
    detect_os

    # 验证源文件
    validate_source_files

    # 获取安装目录
    get_install_directory "$@"

    # 检查环境变量中的其他 PreCI 目录
    check_old_preci_env

    # 准备安装目录
    prepare_install_directory

    # 拷贝文件
    copy_files

    # 创建必要的目录
    create_directories

    # 设置环境变量
    setup_environment

    # 验证安装
    if ! verify_installation; then
        log_error "安装验证失败，请检查日志"
        exit 1
    fi

    # 打印安装信息
    print_installation_info
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