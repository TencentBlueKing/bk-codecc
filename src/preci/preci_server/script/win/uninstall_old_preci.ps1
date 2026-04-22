################################################################################
# 旧版 PreCI 系统卸载脚本 (Windows)
# 
# 功能说明:
#   - 停止旧版 PreCI 相关服务和进程
#   - 删除旧版 PreCI 安装目录
#   - 清理环境变量配置
#
# 使用方法:
#   .\uninstall_old_preci.ps1 [安装目录]
#
# 示例:
#   .\uninstall_old_preci.ps1                           # 使用默认目录 C:\Users\<用户名>\PreCI
#   .\uninstall_old_preci.ps1 "C:\Program Files\PreCI"  # 指定目录
#
################################################################################

# 设置错误时停止执行
$ErrorActionPreference = "Stop"

#=============================================================================
# 颜色输出函数
#=============================================================================
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Type = "Info"
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    
    switch ($Type) {
        "Info"    { Write-Host "[$timestamp] [INFO] $Message" -ForegroundColor Cyan }
        "Success" { Write-Host "[$timestamp] [SUCCESS] $Message" -ForegroundColor Green }
        "Warning" { Write-Host "[$timestamp] [WARNING] $Message" -ForegroundColor Yellow }
        "Error"   { Write-Host "[$timestamp] [ERROR] $Message" -ForegroundColor Red }
    }
}

#=============================================================================
# 打印横幅
#=============================================================================
function Show-Banner {
    Write-Host ""
    Write-Host "==============================================" -ForegroundColor Cyan
    Write-Host "   旧版 PreCI 系统卸载脚本 (Windows)" -ForegroundColor Cyan
    Write-Host "==============================================" -ForegroundColor Cyan
    Write-Host ""
}

#=============================================================================
# 停止旧版 PreCI 服务
#=============================================================================
function Stop-OldPreCIService {
    param([string]$InstallDir)
    
    Write-ColorOutput "正在停止旧版 PreCI 服务..." "Info"
    
    # 尝试使用旧版命令停止服务
    $preciExe = Join-Path $InstallDir "preci.exe"
    if (Test-Path $preciExe) {
        try {
            Write-ColorOutput "尝试执行: $preciExe server --stop" "Info"
            & $preciExe server --stop 2>$null
            if ($LASTEXITCODE -ne 0) {
                throw "停止服务失败"
            }
            Start-Sleep -Seconds 2
        } catch {
            Write-ColorOutput "停止服务失败" "Error"
            Write-ColorOutput "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020#%E5%8D%B8%E8%BD%BD-PreCI-server" "Warning"
            return $false
        }
    } else {
        Write-ColorOutput "未找到 preci.exe，跳过停止服务步骤" "Warning"
    }
    
    Write-ColorOutput "服务停止命令已执行" "Success"
    return $true
}

#=============================================================================
# 执行 agent 卸载脚本
#=============================================================================
function Invoke-AgentUninstall {
    param([string]$InstallDir)
    
    Write-ColorOutput "正在执行 agent 卸载脚本..." "Info"
    
    $agentUninstall = Join-Path $InstallDir "agent\uninstall.bat"
    if (-not (Test-Path $agentUninstall)) {
        $agentUninstall = Join-Path $InstallDir "agent\uninstall.ps1"
    }
    if (-not (Test-Path $agentUninstall)) {
        $agentUninstall = Join-Path $InstallDir "agent\uninstall"
    }
    
    if (Test-Path $agentUninstall) {
        try {
            Write-ColorOutput "尝试执行: $agentUninstall" "Info"
            
            if ($agentUninstall -match '\.ps1$') {
                & $agentUninstall 2>$null
            } else {
                cmd /c "$agentUninstall" 2>$null
            }
            
            if ($LASTEXITCODE -ne 0) {
                throw "执行失败"
            }
            
            Write-ColorOutput "agent 卸载脚本执行完成" "Success"
        } catch {
            Write-ColorOutput "执行 agent 卸载脚本失败" "Error"
            Write-ColorOutput "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020#%E5%8D%B8%E8%BD%BD-PreCI-server" "Warning"
            return $false
        }
    } else {
        Write-ColorOutput "未找到 agent 卸载脚本" "Warning"
        Write-ColorOutput "跳过 agent 卸载步骤" "Info"
    }
    
    return $true
}

#=============================================================================
# 清理环境变量
#=============================================================================
function Remove-EnvironmentVariables {
    Write-ColorOutput "正在清理环境变量..." "Info"
    
    $cleaned = $false
    
    try {
        # 获取用户级别的环境变量
        $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
        $preciHome = [Environment]::GetEnvironmentVariable("PRECI_HOME", "User")
        
        # 清理 PRECI_HOME
        if ($preciHome) {
            Write-ColorOutput "删除环境变量: PRECI_HOME = $preciHome" "Info"
            [Environment]::SetEnvironmentVariable("PRECI_HOME", $null, "User")
            $cleaned = $true
        }
        
        # 清理 PATH 中的 PreCI 相关路径
        if ($userPath) {
            $pathArray = $userPath -split ';' | Where-Object { $_ -and $_ -notmatch 'PreCI' }
            $newPath = $pathArray -join ';'
            
            if ($newPath -ne $userPath) {
                Write-ColorOutput "清理 PATH 环境变量中的 PreCI 相关路径" "Info"
                [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
                $cleaned = $true
            }
        }
        
        if ($cleaned) {
            Write-ColorOutput "环境变量已清理" "Success"
            Write-ColorOutput "请重新打开终端窗口以使环境变量生效" "Warning"
        } else {
            Write-ColorOutput "未发现需要清理的环境变量配置" "Info"
        }
        
    } catch {
        Write-ColorOutput "清理环境变量时出错: $_" "Error"
    }
}

#=============================================================================
# 删除安装目录
#=============================================================================
function Remove-InstallationDirectory {
    param([string]$InstallDir)
    
    Write-ColorOutput "正在删除安装目录: $InstallDir" "Info"
    
    # 安全检查：确保不是系统关键目录
    $systemDirs = @(
        $env:SystemRoot,
        $env:ProgramFiles,
        $env:USERPROFILE,
        "C:\",
        "C:\Windows",
        "C:\Program Files",
        "C:\Program Files (x86)"
    )
    
    foreach ($sysDir in $systemDirs) {
        if ($InstallDir -eq $sysDir) {
            Write-ColorOutput "安全检查失败: 不能删除系统关键目录" "Error"
            throw "安全检查失败"
        }
    }
    
    # 检查目录是否存在
    if (-not (Test-Path $InstallDir)) {
        Write-ColorOutput "目录不存在: $InstallDir" "Warning"
        return
    }
    
    # 再次确认
    Write-Host ""
    Write-ColorOutput "警告: 即将删除目录及其所有内容: $InstallDir" "Warning"
    $confirm = Read-Host "确认删除? (yes/no)"
    
    if ($confirm -ne "yes") {
        Write-ColorOutput "用户取消删除操作" "Warning"
        exit 0
    }
    
    try {
        # 删除目录（包括只读文件）
        Remove-Item -Path $InstallDir -Recurse -Force -ErrorAction Stop
        Write-ColorOutput "安装目录已删除" "Success"
        return $true
    } catch {
        Write-ColorOutput "删除目录失败: $_" "Error"
        
        # 尝试使用 cmd 的 rd 命令
        Write-ColorOutput "尝试使用备用方法删除..." "Info"
        try {
            cmd /c "rd /s /q `"$InstallDir`"" 2>$null
            if (-not (Test-Path $InstallDir)) {
                Write-ColorOutput "安装目录已删除" "Success"
                return $true
            } else {
                throw "删除失败"
            }
        } catch {
            Write-ColorOutput "无法删除目录，可能有文件被占用" "Error"
            return $false
        }
    }
}

#=============================================================================
# 主函数
#=============================================================================
function Main {
    param([string]$InstallDir)
    
    Show-Banner
    
    # 确定安装目录
    if (-not $InstallDir) {
        $InstallDir = Join-Path $env:USERPROFILE "PreCI"
    }
    
    Write-ColorOutput "目标卸载目录: $InstallDir" "Info"
    
    # 检查目录是否存在
    if (-not (Test-Path $InstallDir)) {
        Write-ColorOutput "目录不存在: $InstallDir" "Warning"
        Write-ColorOutput "可能旧版 PreCI 已经被卸载，或安装在其他位置" "Info"
        $continueClear = Read-Host "是否继续清理环境变量? (yes/no)"
        if ($continueClear -eq "yes") {
            Remove-EnvironmentVariables
        }
        exit 0
    }
    
    Write-Host ""
    Write-ColorOutput "即将卸载旧版 PreCI 系统" "Warning"
    Write-ColorOutput "安装目录: $InstallDir" "Warning"
    Write-Host ""
    $confirm = Read-Host "确认继续? (yes/no)"
    
    if ($confirm -ne "yes") {
        Write-ColorOutput "用户取消卸载" "Info"
        exit 0
    }
    
    Write-Host ""
    
    try {
        # 执行卸载步骤
        # 步骤1: 停止服务
        if (-not (Stop-OldPreCIService -InstallDir $InstallDir)) {
            exit 1
        }
        Write-Host ""
        
        # 步骤2: 执行 agent 卸载脚本
        if (-not (Invoke-AgentUninstall -InstallDir $InstallDir)) {
            exit 1
        }
        Write-Host ""
        
        # 步骤3: 清理环境变量
        Remove-EnvironmentVariables
        Write-Host ""
        
        # 步骤4: 删除安装目录
        if (-not (Remove-InstallationDirectory -InstallDir $InstallDir)) {
            Write-ColorOutput "删除目录失败" "Error"
            Write-ColorOutput "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020#%E5%8D%B8%E8%BD%BD-PreCI-server" "Warning"
            exit 1
        }
        Write-Host ""
        
        # 完成
        Write-Host ""
        Write-Host "==============================================" -ForegroundColor Green
        Write-Host "   旧版 PreCI 卸载完成!" -ForegroundColor Green
        Write-Host "==============================================" -ForegroundColor Green
        Write-Host ""
        Write-ColorOutput "建议重新打开终端以确保环境变量生效" "Info"
        
    } catch {
        Write-Host ""
        Write-ColorOutput "卸载过程中出现错误: $_" "Error"
        Write-Host ""
        exit 1
    }
}

# 执行主函数
Main -InstallDir $args[0]
