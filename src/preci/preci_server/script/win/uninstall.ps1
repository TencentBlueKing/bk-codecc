################################################################################
# PreCI 卸载脚本 (Windows)
# 
# 功能说明:
#   - 停止 PreCI 服务
#   - 清理环境变量配置
#   - 删除安装目录及所有文件
#   - 支持 Windows 10/11 (AMD64/ARM64) 环境
#
# 使用方法:
#   .\uninstall.ps1 [安装目录]
#   或右键 -> "使用 PowerShell 运行"
#
# 作者: PreCI Team
################################################################################

# 设置错误处理
$ErrorActionPreference = "Stop"

################################################################################
# 全局变量
################################################################################

# 获取脚本所在目录（即安装目录）
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 默认卸载目录（脚本所在目录）
$DefaultUninstallDir = $ScriptDir

# 实际卸载目录（可通过参数指定）
$UninstallDir = ""

################################################################################
# 日志输出函数
################################################################################

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error-Log {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host $Message -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
}

################################################################################
# 获取卸载目录
################################################################################

function Get-UninstallDirectory {
    param([string]$InputDir)
    
    if ($InputDir) {
        $script:UninstallDir = $InputDir
    } else {
        # 默认使用脚本所在目录
        $script:UninstallDir = $DefaultUninstallDir
    }
    
    # 展开环境变量
    $script:UninstallDir = [System.Environment]::ExpandEnvironmentVariables($script:UninstallDir)
    
    # 转换为绝对路径
    if (-not [System.IO.Path]::IsPathRooted($script:UninstallDir)) {
        $script:UninstallDir = Join-Path (Get-Location) $script:UninstallDir
    }
    
    Write-Info "卸载目录: $script:UninstallDir"
}

################################################################################
# 验证卸载目录
################################################################################

function Test-UninstallDirectory {
    if (-not (Test-Path $UninstallDir)) {
        Write-Error-Log "目录不存在: $UninstallDir"
        exit 1
    }
    
    # 检查是否是 PreCI 安装目录（通过检查关键文件）
    $isPreciDir = $false
    
    if ((Test-Path (Join-Path $UninstallDir "preci.exe")) -or 
        (Test-Path (Join-Path $UninstallDir "preci-server.exe")) -or
        (Test-Path (Join-Path $UninstallDir "preci-mcp.exe"))) {
        $isPreciDir = $true
    }
    
    if (-not $isPreciDir) {
        Write-Warn "目录 $UninstallDir 似乎不是 PreCI 安装目录"
        $confirm = Read-Host "是否继续卸载? [y/N]"
        if ($confirm -notmatch '^[yY]') {
            exit 0
        }
    }
}

################################################################################
# 确认卸载
################################################################################

function Confirm-Uninstall {
    Write-Header "PreCI 卸载确认"
    
    Write-Host "警告: 此操作将:" -ForegroundColor Yellow
    Write-Host "  1. 停止 PreCI 服务"
    Write-Host "  2. 删除目录: $UninstallDir" -ForegroundColor White
    Write-Host "  3. 清理环境变量配置"
    Write-Host ""
    Write-Host "此操作不可恢复!" -ForegroundColor Red
    Write-Host ""
    
    $confirm = Read-Host "确认卸载 PreCI? [y/N]"
    
    if ($confirm -match '^[yY]') {
        Write-Info "开始卸载..."
    } else {
        Write-Info "卸载已取消"
        exit 0
    }
}

################################################################################
# 停止 PreCI 服务
################################################################################

function Stop-PreciService {
    Write-Info "停止 PreCI 服务..."
    
    $preciBin = Join-Path $UninstallDir "preci.exe"
    
    if (Test-Path $preciBin) {
        try {
            # 尝试停止服务
            & $preciBin server stop 2>$null
            Write-Success "PreCI 服务已停止"
        } catch {
            Write-Warn "PreCI 服务可能未运行或停止失败"
        }
    } else {
        Write-Warn "未找到 preci.exe，跳过服务停止"
    }
    
    # 等待一下确保进程完全退出
    Start-Sleep -Seconds 1
    
    # 强制结束可能残留的进程
    $processes = @("preci", "preci-server", "preci-mcp")
    foreach ($proc in $processes) {
        $running = Get-Process -Name $proc -ErrorAction SilentlyContinue
        if ($running) {
            Write-Info "强制结束进程: $proc"
            Stop-Process -Name $proc -Force -ErrorAction SilentlyContinue
        }
    }
}

################################################################################
# 清理环境变量
################################################################################

function Remove-EnvironmentVariables {
    Write-Info "清理环境变量..."
    
    $cleaned = $false
    
    # 获取当前用户的环境变量
    $preciHome = [Environment]::GetEnvironmentVariable("PRECI_HOME", "User")
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    
    # 清理 PRECI_HOME
    if ($preciHome) {
        Write-Info "清理 PRECI_HOME 环境变量"
        [Environment]::SetEnvironmentVariable("PRECI_HOME", $null, "User")
        $cleaned = $true
    }
    
    # 清理 PATH 中的安装目录
    if ($userPath) {
        $pathArray = $userPath -split ';' | Where-Object { $_ -and $_ -ne $UninstallDir }
        $newPath = $pathArray -join ';'
        
        if ($newPath -ne $userPath) {
            Write-Info "从 PATH 中移除安装目录"
            [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
            $cleaned = $true
        }
    }
    
    if ($cleaned) {
        Write-Success "环境变量已清理"
        Write-Host ""
        Write-Warn "环境变量已清理，重新打开终端窗口后生效"
    } else {
        Write-Info "未找到需要清理的环境变量配置"
    }
}

################################################################################
# 删除安装目录
################################################################################

function Remove-Installation {
    Write-Info "删除安装目录..."
    
    if (-not (Test-Path $UninstallDir)) {
        Write-Warn "目录不存在: $UninstallDir"
        return
    }
    
    # 安全检查：确保不会删除重要目录
    $protectedDirs = @(
        $env:SystemRoot,
        $env:ProgramFiles,
        ${env:ProgramFiles(x86)},
        $env:USERPROFILE,
        $env:SystemDrive + "\"
    )
    
    foreach ($protected in $protectedDirs) {
        if ($protected -and ($UninstallDir -eq $protected)) {
            Write-Error-Log "拒绝删除系统目录: $UninstallDir"
            exit 1
        }
    }
    
    # 如果当前在要删除的目录中，先切换到用户目录
    if ($PWD.Path.StartsWith($UninstallDir)) {
        Set-Location $env:USERPROFILE
    }
    
    try {
        # 删除目录
        Remove-Item -Path $UninstallDir -Recurse -Force
        Write-Success "已删除: $UninstallDir"
    } catch {
        Write-Error-Log "删除目录失败: $UninstallDir"
        Write-Error-Log $_.Exception.Message
        exit 1
    }
}

################################################################################
# 打印卸载完成信息
################################################################################

function Show-UninstallInfo {
    Write-Header "PreCI 卸载完成"
    
    Write-Host "PreCI 已成功卸载" -ForegroundColor Green
    Write-Host ""
    Write-Host "后续步骤:" -ForegroundColor Cyan
    Write-Host "  1. 重新打开终端窗口使环境变量更改生效"
    Write-Host ""
    
    Write-Header "感谢使用 PreCI"
}

################################################################################
# 清理函数（错误时调用）
################################################################################

function Invoke-ErrorCleanup {
    Write-Error-Log "卸载过程中发生错误"
    Write-Info "部分文件可能未被清理，请手动检查"
    exit 1
}

################################################################################
# 主函数
################################################################################

function Main {
    param([string]$InputDir)
    
    try {
        Write-Header "PreCI 卸载程序"
        
        # 获取卸载目录
        Get-UninstallDirectory -InputDir $InputDir
        
        # 验证卸载目录
        Test-UninstallDirectory
        
        # 确认卸载
        Confirm-Uninstall
        
        # 停止 PreCI 服务
        Stop-PreciService
        
        # 清理环境变量
        Remove-EnvironmentVariables
        
        # 删除安装目录
        Remove-Installation
        
        # 打印卸载完成信息
        Show-UninstallInfo
        
    } catch {
        Write-Error-Log $_.Exception.Message
        Invoke-ErrorCleanup
    }
}

################################################################################
# 脚本入口
################################################################################

# 执行主函数
Main -InputDir $args[0]
