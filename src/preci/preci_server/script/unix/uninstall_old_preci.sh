#!/bin/bash

################################################################################
# 旧版 PreCI 系统卸载脚本 (Unix/Linux/macOS)
# 
# 功能说明:
#   - 停止旧版 PreCI 相关服务和进程
#   - 删除旧版 PreCI 安装目录
#   - 清理环境变量配置
#
# 使用方法:
#   ./uninstall_old_preci.sh [安装目录]
#
# 示例:
#   ./uninstall_old_preci.sh                    # 使用默认目录 ~/PreCI
#   ./uninstall_old_preci.sh /opt/PreCI         # 指定目录
#
################################################################################

set -e  # 遇到错误立即退出

#=============================================================================
# 颜色定义
#=============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

#=============================================================================
# 日志函数
#=============================================================================
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

#=============================================================================
# 打印横幅
#=============================================================================
print_banner() {
    echo -e "${BLUE}"
    echo "=============================================="
    echo "   旧版 PreCI 系统卸载脚本"
    echo "=============================================="
    echo -e "${NC}"
}

#=============================================================================
# 检测操作系统
#=============================================================================
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
    else
        log_error "不支持的操作系统: $OSTYPE"
        exit 1
    fi
    log_info "检测到操作系统: $OS"
}

#=============================================================================
# 停止旧版 PreCI 服务
#=============================================================================
stop_old_preci_service() {
    log_info "正在停止旧版 PreCI 服务..."
    
    # 尝试使用旧版命令停止服务
    if [ -f "$INSTALL_DIR/preci" ]; then
        log_info "尝试执行: $INSTALL_DIR/preci server --stop"
        if ! "$INSTALL_DIR/preci" server --stop 2>/dev/null; then
            log_error "停止服务失败"
            log_warning "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020#%E5%8D%B8%E8%BD%BD-PreCI-server"
            return 1
        fi
        sleep 2
    else
        log_warning "未找到 preci 可执行文件，跳过停止服务步骤"
    fi
    
    log_success "服务停止命令已执行"
    return 0
}

#=============================================================================
# 执行 agent 卸载脚本
#=============================================================================
run_agent_uninstall() {
    log_info "正在执行 agent 卸载脚本..."
    
    local agent_uninstall="$INSTALL_DIR/agent/uninstall"
    
    if [ -f "$agent_uninstall" ]; then
        log_info "尝试执行: $agent_uninstall"
        
        # 确保脚本有执行权限
        chmod +x "$agent_uninstall" 2>/dev/null || true
        
        if ! "$agent_uninstall" 2>/dev/null; then
            log_error "执行 agent 卸载脚本失败"
            log_warning "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020#%E5%8D%B8%E8%BD%BD-PreCI-server"
            return 1
        fi
        
        log_success "agent 卸载脚本执行完成"
    else
        log_warning "未找到 agent 卸载脚本: $agent_uninstall"
        log_info "跳过 agent 卸载步骤"
    fi
    
    return 0
}

#=============================================================================
# 清理环境变量
#=============================================================================
clean_environment_variables() {
    log_info "正在清理环境变量..."
    
    # 检测当前使用的 shell
    local shell_configs=()
    
    if [ -n "$BASH_VERSION" ]; then
        [ -f "$HOME/.bashrc" ] && shell_configs+=("$HOME/.bashrc")
        [ -f "$HOME/.bash_profile" ] && shell_configs+=("$HOME/.bash_profile")
    fi
    
    if [ -n "$ZSH_VERSION" ] || [ -f "$HOME/.zshrc" ]; then
        [ -f "$HOME/.zshrc" ] && shell_configs+=("$HOME/.zshrc")
    fi
    
    # 如果没有检测到配置文件，尝试常见的配置文件
    if [ ${#shell_configs[@]} -eq 0 ]; then
        [ -f "$HOME/.profile" ] && shell_configs+=("$HOME/.profile")
        [ -f "$HOME/.bashrc" ] && shell_configs+=("$HOME/.bashrc")
        [ -f "$HOME/.zshrc" ] && shell_configs+=("$HOME/.zshrc")
    fi
    
    local cleaned=false
    
    for config_file in "${shell_configs[@]}"; do
        if [ -f "$config_file" ]; then
            # 备份配置文件
            cp "$config_file" "${config_file}.bak.$(date +%Y%m%d_%H%M%S)"
            
            # 删除 PreCI 相关的环境变量配置
            # 匹配包含 PreCI、PRECI_HOME 或指向 PreCI 目录的行
            if grep -q "PreCI\|PRECI_HOME" "$config_file" 2>/dev/null; then
                log_info "清理配置文件: $config_file"
                
                # 使用 sed 删除相关行
                if [[ "$OS" == "macos" ]]; then
                    sed -i '' '/PreCI/d; /PRECI_HOME/d' "$config_file"
                else
                    sed -i '/PreCI/d; /PRECI_HOME/d' "$config_file"
                fi
                
                cleaned=true
            fi
        fi
    done
    
    if [ "$cleaned" = true ]; then
        log_success "环境变量已清理"
        log_warning "请执行以下命令使配置生效，或重新打开终端:"
        for config_file in "${shell_configs[@]}"; do
            [ -f "$config_file" ] && echo "  source $config_file"
        done
    else
        log_info "未发现需要清理的环境变量配置"
    fi
}

#=============================================================================
# 删除安装目录
#=============================================================================
remove_installation_directory() {
    log_info "正在删除安装目录: $INSTALL_DIR"
    
    # 安全检查：确保不是根目录或用户主目录
    if [ "$INSTALL_DIR" = "/" ] || [ "$INSTALL_DIR" = "$HOME" ] || [ -z "$INSTALL_DIR" ]; then
        log_error "安全检查失败: 不能删除根目录或用户主目录"
        exit 1
    fi
    
    # 检查目录是否存在
    if [ ! -d "$INSTALL_DIR" ]; then
        log_warning "目录不存在: $INSTALL_DIR"
        return 0
    fi
    
    # 再次确认
    echo -e "${YELLOW}警告: 即将删除目录及其所有内容: $INSTALL_DIR${NC}"
    read -p "确认删除? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        log_warning "用户取消删除操作"
        exit 0
    fi
    
    # 删除目录
    if ! rm -rf "$INSTALL_DIR" 2>/dev/null; then
        log_error "删除目录失败"
        return 1
    fi
    
    log_success "安装目录已删除"
    return 0
}

#=============================================================================
# 主函数
#=============================================================================
main() {
    print_banner
    
    # 检测操作系统
    detect_os
    
    # 确定安装目录
    if [ -n "$1" ]; then
        INSTALL_DIR="$1"
    else
        INSTALL_DIR="$HOME/PreCI"
    fi
    
    log_info "目标卸载目录: $INSTALL_DIR"
    
    # 检查目录是否存在
    if [ ! -d "$INSTALL_DIR" ]; then
        log_warning "目录不存在: $INSTALL_DIR"
        log_info "可能旧版 PreCI 已经被卸载，或安装在其他位置"
        read -p "是否继续清理环境变量? (yes/no): " continue_clean
        if [ "$continue_clean" = "yes" ]; then
            clean_environment_variables
        fi
        exit 0
    fi
    
    echo ""
    log_warning "即将卸载旧版 PreCI 系统"
    log_warning "安装目录: $INSTALL_DIR"
    echo ""
    read -p "确认继续? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        log_info "用户取消卸载"
        exit 0
    fi
    
    echo ""
    
    # 执行卸载步骤
    # 步骤1: 停止服务
    if ! stop_old_preci_service; then
        exit 1
    fi
    echo ""
    
    # 步骤2: 执行 agent 卸载脚本
    if ! run_agent_uninstall; then
        exit 1
    fi
    echo ""
    
    # 步骤3: 清理环境变量
    clean_environment_variables
    echo ""
    
    # 步骤4: 删除安装目录
    if ! remove_installation_directory; then
        log_error "删除目录失败"
        log_warning "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020#%E5%8D%B8%E8%BD%BD-PreCI-server"
        exit 1
    fi
    echo ""
    
    # 完成
    echo -e "${GREEN}"
    echo "=============================================="
    echo "   旧版 PreCI 卸载完成!"
    echo "=============================================="
    echo -e "${NC}"
    echo ""
    log_info "建议重新打开终端以确保环境变量生效"
}

# 执行主函数
main "$@"
