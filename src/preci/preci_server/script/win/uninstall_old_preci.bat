@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 旧版 PreCI 系统卸载脚本 (Windows BAT)
::
:: 功能说明:
::   - 作为 uninstall_old_preci.ps1 的兜底方案，适用于没有 PowerShell 的环境
::   - 停止旧版 PreCI 相关服务和进程
::   - 删除旧版 PreCI 安装目录
::   - 清理环境变量配置
::
:: 使用方法:
::   uninstall_old_preci.bat [安装目录]
::
:: 示例:
::   uninstall_old_preci.bat                           :: 使用默认目录
::   uninstall_old_preci.bat "C:\Program Files\PreCI"  :: 指定目录
::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:: 默认安装目录
set "DEFAULT_INSTALL_DIR=%USERPROFILE%\PreCI"
set "INSTALL_DIR="

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 主入口
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

echo.
echo ==============================================
echo    旧版 PreCI 系统卸载脚本 (Windows)
echo ==============================================
echo.

:: 确定安装目录
if not "%~1"=="" (
    set "INSTALL_DIR=%~1"
) else (
    set "INSTALL_DIR=%DEFAULT_INSTALL_DIR%"
)

call :WriteLog "目标卸载目录: %INSTALL_DIR%" "Info"

:: 检查目录是否存在
if not exist "%INSTALL_DIR%" (
    call :WriteLog "目录不存在: %INSTALL_DIR%" "Warning"
    call :WriteLog "可能旧版 PreCI 已经被卸载，或安装在其他位置" "Info"
    set /p "CONFIRM=是否继续清理环境变量? [yes/no]: "
    if /i "!CONFIRM!"=="yes" (
        call :RemoveEnvironmentVariables
    )
    goto :EOF
)

echo.
call :WriteLog "即将卸载旧版 PreCI 系统" "Warning"
call :WriteLog "安装目录: %INSTALL_DIR%" "Warning"
echo.
set /p "CONFIRM=确认继续? [yes/no]: "

if /i not "%CONFIRM%"=="yes" (
    call :WriteLog "用户取消卸载" "Info"
    goto :EOF
)

echo.

:: 步骤1: 停止服务
call :StopOldPreCIService
if errorlevel 1 goto :ErrorExit
echo.

:: 步骤2: 执行 agent 卸载脚本
call :InvokeAgentUninstall
if errorlevel 1 goto :ErrorExit
echo.

:: 步骤3: 清理环境变量
call :RemoveEnvironmentVariables
echo.

:: 步骤4: 删除安装目录
call :RemoveInstallationDirectory
if errorlevel 1 goto :ErrorExit
echo.

:: 完成
echo.
echo ==============================================
echo    旧版 PreCI 卸载完成!
echo ==============================================
echo.
call :WriteLog "建议重新打开终端以确保环境变量生效" "Info"

goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 日志输出函数
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:WriteLog
set "MSG=%~1"
set "TYPE=%~2"

for /f "tokens=1-3 delims=/ " %%a in ('date /t') do set "DATESTAMP=%%a-%%b-%%c"
for /f "tokens=1-2 delims=: " %%a in ('time /t') do set "TIMESTAMP=%%a:%%b"

if /i "%TYPE%"=="Info"    echo [%DATESTAMP% %TIMESTAMP%] [INFO] %MSG%
if /i "%TYPE%"=="Success" echo [%DATESTAMP% %TIMESTAMP%] [SUCCESS] %MSG%
if /i "%TYPE%"=="Warning" echo [%DATESTAMP% %TIMESTAMP%] [WARNING] %MSG%
if /i "%TYPE%"=="Error"   echo [%DATESTAMP% %TIMESTAMP%] [ERROR] %MSG%
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 停止旧版 PreCI 服务
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:StopOldPreCIService
call :WriteLog "正在停止旧版 PreCI 服务..." "Info"

set "PRECI_EXE=%INSTALL_DIR%\preci.exe"
if exist "%PRECI_EXE%" (
    call :WriteLog "尝试执行: %PRECI_EXE% server --stop" "Info"
    "%PRECI_EXE%" server --stop 2>nul
    if !errorlevel! NEQ 0 (
        call :WriteLog "停止服务失败" "Error"
        call :WriteLog "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020" "Warning"
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
) else (
    call :WriteLog "未找到 preci.exe，跳过停止服务步骤" "Warning"
)

call :WriteLog "服务停止命令已执行" "Success"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 执行 agent 卸载脚本
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:InvokeAgentUninstall
call :WriteLog "正在执行 agent 卸载脚本..." "Info"

set "AGENT_UNINSTALL="

:: 优先查找 bat 版本
if exist "%INSTALL_DIR%\agent\uninstall.bat" (
    set "AGENT_UNINSTALL=%INSTALL_DIR%\agent\uninstall.bat"
)
:: 其次查找无扩展名版本
if not defined AGENT_UNINSTALL (
    if exist "%INSTALL_DIR%\agent\uninstall" (
        set "AGENT_UNINSTALL=%INSTALL_DIR%\agent\uninstall"
    )
)

if defined AGENT_UNINSTALL (
    call :WriteLog "尝试执行: %AGENT_UNINSTALL%" "Info"
    call "%AGENT_UNINSTALL%" 2>nul
    if !errorlevel! NEQ 0 (
        call :WriteLog "执行 agent 卸载脚本失败" "Error"
        call :WriteLog "请参考手动卸载文档: https://iwiki.woa.com/p/4015420020" "Warning"
        exit /b 1
    )
    call :WriteLog "agent 卸载脚本执行完成" "Success"
) else (
    :: 检查是否有 ps1 版本但没有 bat 版本
    if exist "%INSTALL_DIR%\agent\uninstall.ps1" (
        call :WriteLog "仅发现 ps1 版本的 agent 卸载脚本，尝试通过 cmd 调用..." "Warning"
        powershell.exe -ExecutionPolicy Bypass -File "%INSTALL_DIR%\agent\uninstall.ps1" 2>nul
        if !errorlevel! NEQ 0 (
            call :WriteLog "执行 agent 卸载脚本失败（PowerShell 可能不可用）" "Warning"
        ) else (
            call :WriteLog "agent 卸载脚本执行完成" "Success"
        )
    ) else (
        call :WriteLog "未找到 agent 卸载脚本" "Warning"
        call :WriteLog "跳过 agent 卸载步骤" "Info"
    )
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 清理环境变量
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:RemoveEnvironmentVariables
call :WriteLog "正在清理环境变量..." "Info"

set "CLEANED=0"

:: 清理 PRECI_HOME
set "CURRENT_PRECI_HOME="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v PRECI_HOME 2^>nul') do set "CURRENT_PRECI_HOME=%%B"

if defined CURRENT_PRECI_HOME (
    call :WriteLog "删除环境变量: PRECI_HOME = %CURRENT_PRECI_HOME%" "Info"
    reg delete "HKCU\Environment" /v PRECI_HOME /f >nul 2>&1
    set "CLEANED=1"
)

:: 清理 PATH 中的 PreCI 相关路径
set "USER_PATH="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v Path 2^>nul') do set "USER_PATH=%%B"

if defined USER_PATH (
    set "NEW_PATH="
    set "PATH_CHANGED=0"

    for %%P in ("!USER_PATH:;=" "!") do (
        set "ENTRY=%%~P"
        if not "!ENTRY!"=="" (
            echo !ENTRY! | findstr /i "PreCI" >nul 2>&1
            if !errorlevel!==0 (
                call :WriteLog "从 PATH 中移除: !ENTRY!" "Info"
                set "PATH_CHANGED=1"
            ) else (
                if defined NEW_PATH (
                    set "NEW_PATH=!NEW_PATH!;!ENTRY!"
                ) else (
                    set "NEW_PATH=!ENTRY!"
                )
            )
        )
    )

    if !PATH_CHANGED!==1 (
        call :WriteLog "清理 PATH 环境变量中的 PreCI 相关路径" "Info"
        if defined NEW_PATH (
            reg add "HKCU\Environment" /v Path /t REG_EXPAND_SZ /d "!NEW_PATH!" /f >nul 2>&1
        ) else (
            reg delete "HKCU\Environment" /v Path /f >nul 2>&1
        )
        set "CLEANED=1"
    )
)

if %CLEANED%==1 (
    call :WriteLog "环境变量已清理" "Success"
    call :WriteLog "请重新打开终端窗口以使环境变量生效" "Warning"
) else (
    call :WriteLog "未发现需要清理的环境变量配置" "Info"
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 删除安装目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:RemoveInstallationDirectory
call :WriteLog "正在删除安装目录: %INSTALL_DIR%" "Info"

:: 安全检查：确保不是系统关键目录
for %%D in ("%SystemRoot%" "%ProgramFiles%" "%USERPROFILE%" "C:\") do (
    if /i "%INSTALL_DIR%"=="%%~D" (
        call :WriteLog "安全检查失败: 不能删除系统关键目录" "Error"
        exit /b 1
    )
)

:: 检查目录是否存在
if not exist "%INSTALL_DIR%" (
    call :WriteLog "目录不存在: %INSTALL_DIR%" "Warning"
    goto :EOF
)

:: 再次确认
echo.
call :WriteLog "警告: 即将删除目录及其所有内容: %INSTALL_DIR%" "Warning"
set /p "CONFIRM=确认删除? [yes/no]: "

if /i not "%CONFIRM%"=="yes" (
    call :WriteLog "用户取消删除操作" "Warning"
    exit /b 1
)

:: 如果当前在要删除的目录中，先切换出去
cd /d "%USERPROFILE%" 2>nul

:: 删除目录
rd /s /q "%INSTALL_DIR%" 2>nul
if exist "%INSTALL_DIR%" (
    call :WriteLog "首次删除失败，等待后重试..." "Warning"
    timeout /t 2 /nobreak >nul
    rd /s /q "%INSTALL_DIR%" 2>nul
    if exist "%INSTALL_DIR%" (
        call :WriteLog "无法删除目录，可能有文件被占用" "Error"
        exit /b 1
    )
)

call :WriteLog "安装目录已删除" "Success"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 错误退出
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:ErrorExit
echo.
call :WriteLog "卸载过程中出现错误" "Error"
echo.
exit /b 1
