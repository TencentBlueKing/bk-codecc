################################################################################
# PreCI 安装脚本 (Windows)
#
# 流程:
#   1. 先请用户确定安装目录（默认: C:\Users\<用户名>\PreCI）
#   2. 先找出环境变量中, 除了安装目录以外, 所有包含 \PreCI 的目录, 提示用户手动清理
#   3. 如果目标安装目录中已经有文件, 先停止 PreCI 服务，然后只清理需要安装的文件（保留 log 目录，避免被 IDE 插件占用的日志文件导致安装失败）
#   4. 如果需要，则创建目标安装目录
#   5. 将 PreCI 相关文件复制到目标目录 (已在 FilesToCopy 和 DirsToCreate 中指定)
#   6. 设置环境变量
#
# 使用方法:
#   .\install.ps1 [安装目录]
#   或右键 -> "使用 PowerShell 运行"
#
# 作者: PreCI Team
################################################################################

# 设置错误处理
$ErrorActionPreference = "Stop"

################################################################################
# 全局变量
################################################################################

# 获取脚本所在目录（即 bin 目录）
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 默认安装目录 (C:\Users\<用户名>\PreCI)
$DefaultInstallDir = Join-Path $env:USERPROFILE "PreCI"

# 实际安装目录（可通过参数指定）
$InstallDir = ""

# 需要拷贝的文件和目录列表
$FilesToCopy = @(
    "preci.exe",
    "preci-server.exe",
    "preci-mcp.exe",
    "config",
    "checkerset",
    "install.ps1",
    "uninstall.ps1",
    "uninstall_old_preci.ps1",
    "install.bat",
    "uninstall.bat",
    "uninstall_old_preci.bat"
)

# 需要创建的空目录
$DirsToCreate = @(
    "db",
    "log",
    "tool"
)

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
# 系统检测函数
################################################################################

function Get-SystemInfo {
    $os = Get-CimInstance Win32_OperatingSystem
    $arch = $env:PROCESSOR_ARCHITECTURE
    
    Write-Info "检测到系统: $($os.Caption) $arch"
}

################################################################################
# 文件验证函数
################################################################################

function Test-SourceFiles {
    Write-Info "验证源文件..."
    
    $missingFiles = @()
    
    foreach ($file in $FilesToCopy) {
        $sourcePath = Join-Path $ScriptDir $file
        if (-not (Test-Path $sourcePath)) {
            $missingFiles += $file
        }
    }
    
    if ($missingFiles.Count -gt 0) {
        Write-Error-Log "以下必需文件缺失:"
        foreach ($file in $missingFiles) {
            Write-Host "  - $file"
        }
        exit 1
    }
    
    Write-Info "源文件验证通过"
}

################################################################################
# 获取安装目录
################################################################################

function Get-InstallDirectory {
    param([string]$InputDir)
    
    if ($InputDir) {
        $script:InstallDir = $InputDir
    } else {
        # 交互式询问用户
        Write-Host ""
        $userInput = Read-Host "请输入安装目录 [默认: $DefaultInstallDir]"
        
        if ($userInput) {
            $script:InstallDir = $userInput
        } else {
            $script:InstallDir = $DefaultInstallDir
        }
    }
    
    # 展开环境变量
    $script:InstallDir = [System.Environment]::ExpandEnvironmentVariables($script:InstallDir)
    
    # 转换为绝对路径
    if (-not [System.IO.Path]::IsPathRooted($script:InstallDir)) {
        $script:InstallDir = Join-Path (Get-Location) $script:InstallDir
    }
    
    Write-Info "安装目录: $script:InstallDir"
}

################################################################################
# 检查环境变量中的其他 PreCI 目录
################################################################################

function Test-OldPreciEnvironment {
    Write-Info "检查环境变量中的其他 PreCI 安装..."
    
    $oldPreciPaths = @()
    
    # 检查用户级别的 PRECI_HOME 环境变量
    $userPreciHome = [Environment]::GetEnvironmentVariable("PRECI_HOME", "User")
    if ($userPreciHome -and $userPreciHome -ne $InstallDir -and $userPreciHome -match "PreCI") {
        $oldPreciPaths += $userPreciHome
    }
    
    # 检查系统级别的 PRECI_HOME 环境变量
    $systemPreciHome = [Environment]::GetEnvironmentVariable("PRECI_HOME", "Machine")
    if ($systemPreciHome -and $systemPreciHome -ne $InstallDir -and $systemPreciHome -match "PreCI") {
        if ($oldPreciPaths -notcontains $systemPreciHome) {
            $oldPreciPaths += $systemPreciHome
        }
    }
    
    # 检查用户 PATH 中包含 PreCI 的路径
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($userPath) {
        $pathEntries = $userPath -split ';' | Where-Object { $_ -match "PreCI" -and $_ -ne $InstallDir }
        foreach ($entry in $pathEntries) {
            if ($oldPreciPaths -notcontains $entry) {
                $oldPreciPaths += $entry
            }
        }
    }
    
    # 检查系统 PATH 中包含 PreCI 的路径
    $systemPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($systemPath) {
        $pathEntries = $systemPath -split ';' | Where-Object { $_ -match "PreCI" -and $_ -ne $InstallDir }
        foreach ($entry in $pathEntries) {
            if ($oldPreciPaths -notcontains $entry) {
                $oldPreciPaths += $entry
            }
        }
    }
    
    # 如果找到了其他 PreCI 路径，提示用户手动清理
    if ($oldPreciPaths.Count -gt 0) {
        Write-Host ""
        Write-Warn "检测到环境变量中包含其他 PreCI 路径:"
        foreach ($path in $oldPreciPaths) {
            Write-Host "  - $path"
        }
        Write-Host ""
        Write-Host "[WARN] " -ForegroundColor Yellow -NoNewline
        Write-Host "建议手动清理旧的 PreCI 环境变量:" -ForegroundColor White
        Write-Host "  1. 打开系统环境变量设置:"
        Write-Host "     - 按 Win+R，输入: " -NoNewline
        Write-Host "sysdm.cpl" -ForegroundColor Blue
        Write-Host "     - 点击 '高级' -> '环境变量'"
        Write-Host "  2. 在 '用户变量' 和 '系统变量' 中:"
        Write-Host "     - 删除旧的 " -NoNewline
        Write-Host "PRECI_HOME" -ForegroundColor Yellow -NoNewline
        Write-Host " 变量"
        Write-Host "     - 编辑 " -NoNewline
        Write-Host "Path" -ForegroundColor Yellow -NoNewline
        Write-Host " 变量，删除包含旧 PreCI 路径的条目"
        Write-Host "  3. 点击 '确定' 保存更改"
        Write-Host ""
        $confirm = Read-Host "是否继续安装? [y/N]"
        if ($confirm -notmatch '^[yY]$') {
            Write-Info "安装已取消"
            exit 0
        }
    } else {
        Write-Info "未发现其他 PreCI 安装路径"
    }
}

################################################################################
# 准备安装目录
################################################################################

function Initialize-InstallDirectory {
    # 检查目录是否存在且不为空
    if ((Test-Path $InstallDir) -and ((Get-ChildItem $InstallDir -Force -ErrorAction SilentlyContinue).Count -gt 0)) {
        Write-Warn "目录 $InstallDir 已存在且不为空"
        
        # 交互式确认
        $confirm = Read-Host "是否覆盖安装? [y/N]"
        
        if ($confirm -notmatch '^[yY]$') {
            Write-Info "安装已取消"
            exit 0
        }
        
        # 停止 PreCI 服务（避免可执行文件被占用）
        Write-Info "尝试停止 PreCI 服务..."
        $preciExe = Join-Path $InstallDir "preci.exe"
        if (Test-Path $preciExe) {
            try {
                & $preciExe server stop 2>$null
            } catch {
                # 忽略错误
            }
        }
        
        # 查找并停止 preci-server 进程
        $preciProcesses = Get-Process -Name "preci-server" -ErrorAction SilentlyContinue
        if ($preciProcesses) {
            Write-Info "发现 PreCI 进程，正在停止..."
            $preciProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
            Write-Success "PreCI 进程已停止"
        }
        
        # 只清理需要安装的文件和可清理的目录，保留 log 目录（避免被 IDE 插件占用的日志文件导致安装失败）
        Write-Info "清理旧的安装文件..."
        
        # 清理 FilesToCopy 中的文件/目录
        foreach ($file in $FilesToCopy) {
            $targetPath = Join-Path $InstallDir $file
            if (Test-Path $targetPath) {
                try {
                    Remove-Item -Path $targetPath -Recurse -Force -ErrorAction Stop
                    Write-Info "已清理: $file"
                } catch {
                    Write-Warn "无法清理 $file : $($_.Exception.Message)"
                }
            }
        }
        
        # 清理 DirsToCreate 中除 log 以外的目录
        foreach ($dir in $DirsToCreate) {
            if ($dir -eq "log") { continue }
            $targetPath = Join-Path $InstallDir $dir
            if (Test-Path $targetPath) {
                try {
                    Remove-Item -Path $targetPath -Recurse -Force -ErrorAction Stop
                    Write-Info "已清理目录: $dir"
                } catch {
                    Write-Warn "无法清理目录 $dir : $($_.Exception.Message)"
                }
            }
        }
        
        Write-Success "旧文件清理完成"
    }
    
    # 创建安装目录
    if (-not (Test-Path $InstallDir)) {
        Write-Info "创建安装目录: $InstallDir"
        New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
    }
}

################################################################################
# 拷贝文件
################################################################################

function Copy-Files {
    Write-Info "开始拷贝文件..."
    
    $totalFiles = $FilesToCopy.Count
    $current = 0
    
    foreach ($file in $FilesToCopy) {
        $current++
        $source = Join-Path $ScriptDir $file
        $target = Join-Path $InstallDir $file
        
        if (Test-Path $source) {
            Write-Info "[$current/$totalFiles] 拷贝: $file"
            
            if (Test-Path $source -PathType Container) {
                # 拷贝目录
                Copy-Item -Path $source -Destination $target -Recurse -Force
            } else {
                # 拷贝文件
                Copy-Item -Path $source -Destination $target -Force
            }
        } else {
            Write-Warn "跳过不存在的文件: $file"
        }
    }
    
    Write-Success "文件拷贝完成"
}

################################################################################
# 创建必要的目录
################################################################################

function New-Directories {
    Write-Info "创建必要的目录..."
    
    foreach ($dir in $DirsToCreate) {
        $target = Join-Path $InstallDir $dir
        if (-not (Test-Path $target)) {
            New-Item -ItemType Directory -Path $target -Force | Out-Null
            Write-Info "创建目录: $dir"
        }
    }
}

################################################################################
# 设置环境变量
################################################################################

function Set-EnvironmentVariables {
    Write-Info "配置环境变量..."
    
    # 获取当前用户的 PATH 环境变量
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $preciHome = [Environment]::GetEnvironmentVariable("PRECI_HOME", "User")
    
    # 检查是否已经设置过 PRECI_HOME
    if ($preciHome -eq $InstallDir) {
        Write-Info "PRECI_HOME 已配置，跳过"
    } else {
        # 设置 PRECI_HOME
        [Environment]::SetEnvironmentVariable("PRECI_HOME", $InstallDir, "User")
        Write-Success "已设置 PRECI_HOME = $InstallDir"
    }
    
    # 检查 PATH 中是否已包含安装目录
    if ($userPath -split ';' | Where-Object { $_ -eq $InstallDir }) {
        Write-Info "PATH 已包含安装目录，跳过"
    } else {
        # 添加到 PATH
        $newPath = "$InstallDir;$userPath"
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
        Write-Success "已将安装目录添加到 PATH"
    }
    
    # 更新当前会话的环境变量
    $env:PRECI_HOME = $InstallDir
    $env:Path = "$InstallDir;$env:Path"
    
    Write-Host ""
    Write-Warn "环境变量已配置，重新打开终端窗口后生效"
}

################################################################################
# 验证安装
################################################################################

function Test-Installation {
    Write-Info "验证安装..."
    
    $allOk = $true
    
    # 检查可执行文件
    foreach ($exe in @("preci.exe", "preci-server.exe", "preci-mcp.exe")) {
        $exePath = Join-Path $InstallDir $exe
        if (Test-Path $exePath) {
            Write-Info "✓ $exe 已安装"
        } else {
            Write-Error-Log "✗ $exe 未找到"
            $allOk = $false
        }
    }
    
    if ($allOk) {
        Write-Success "安装验证通过"
        return $true
    } else {
        Write-Error-Log "安装验证失败"
        return $false
    }
}

################################################################################
# 打印安装信息
################################################################################

function Show-InstallationInfo {
    Write-Header "PreCI 安装完成"
    
    Write-Host "安装目录: " -NoNewline
    Write-Host $InstallDir -ForegroundColor Green
    Write-Host ""
    Write-Host "快速开始:" -ForegroundColor Cyan
    Write-Host "  1. 重新打开终端窗口使环境变量生效"
    Write-Host ""
    Write-Host "  2. 验证安装:"
    Write-Host "     preci version" -ForegroundColor Blue
    Write-Host ""
    Write-Host "  3. 查看帮助:"
    Write-Host "     preci --help" -ForegroundColor Blue
    Write-Host ""
    Write-Host "卸载方法:" -ForegroundColor Cyan
    Write-Host "  运行卸载脚本:"
    Write-Host "     $InstallDir\uninstall.ps1" -ForegroundColor Blue
    Write-Host ""
    Write-Host "  或手动删除:"
    Write-Host "     Remove-Item -Recurse -Force $InstallDir" -ForegroundColor Blue
    Write-Host "     然后从系统环境变量中删除 PRECI_HOME 和 PATH 中的相关配置"
    Write-Host ""
    
    Write-Header "感谢使用 PreCI"
}

################################################################################
# 清理函数（错误时调用）
################################################################################

function Invoke-Cleanup {
    Write-Error-Log "安装过程中发生错误"
    
    if ($InstallDir -and (Test-Path $InstallDir)) {
        $confirm = Read-Host "是否删除已创建的安装目录? [y/N]"
        if ($confirm -match '^[yY]') {
            Remove-Item -Path $InstallDir -Recurse -Force
            Write-Info "已删除: $InstallDir"
        }
    }
    
    exit 1
}

################################################################################
# 主函数
################################################################################

function Main {
    param([string]$InputDir)
    
    try {
        Write-Header "PreCI 安装程序"

        # 系统检测
        Get-SystemInfo
        
        # 验证源文件
        Test-SourceFiles
        
        # 获取安装目录
        Get-InstallDirectory -InputDir $InputDir
        
        # 检查环境变量中的其他 PreCI 目录
        Test-OldPreciEnvironment
        
        # 准备安装目录
        Initialize-InstallDirectory
        
        # 拷贝文件
        Copy-Files
        
        # 创建必要的目录
        New-Directories
        
        # 设置环境变量
        Set-EnvironmentVariables
        
        # 验证安装
        Test-Installation
        
        # 打印安装信息
        Show-InstallationInfo
        
    } catch {
        Write-Error-Log $_.Exception.Message
        Invoke-Cleanup
    }
}

################################################################################
# 脚本入口
################################################################################

# 执行主函数
Main -InputDir $args[0]
