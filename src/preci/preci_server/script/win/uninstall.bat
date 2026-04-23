@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: PreCI 卸载脚本 (Windows BAT)
::
:: 功能说明:
::   - 作为 uninstall.ps1 的兜底方案，适用于没有 PowerShell 的环境
::   - 停止 PreCI 服务
::   - 清理环境变量配置
::   - 删除安装目录及所有文件
::   - 支持 Windows 10/11 (AMD64/ARM64) 环境
::
:: 使用方法:
::   uninstall.bat [安装目录]
::   支持 /silent 参数跳过确认提示（供 install.bat 内部调用）
::
:: 作者: PreCI Team
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:: 获取脚本所在目录（即安装目录）
set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

:: 默认卸载目录（脚本所在目录）
set "UNINSTALL_DIR="

:: 静默模式标志
set "SILENT_MODE=0"

:: 解析参数
set "INPUT_DIR="
for %%A in (%*) do (
    if /i "%%A"=="/silent" (
        set "SILENT_MODE=1"
    ) else (
        if not defined INPUT_DIR set "INPUT_DIR=%%~A"
    )
)

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 主入口
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

echo.
echo ============================================
echo   PreCI 卸载程序
echo ============================================
echo.

:: 获取卸载目录
call :GetUninstallDirectory
if errorlevel 1 goto :ErrorExit

:: 验证卸载目录
call :TestUninstallDirectory
if errorlevel 1 goto :ErrorExit

:: 确认卸载
call :ConfirmUninstall
if errorlevel 1 goto :EOF

:: 停止 PreCI 服务
call :StopPreciService

:: 清理环境变量
call :RemoveEnvironmentVariables

:: 删除安装目录
call :RemoveInstallation
if errorlevel 1 goto :ErrorExit

:: 打印卸载完成信息
call :ShowUninstallInfo

goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 日志输出函数
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:WriteInfo
echo [INFO] %~1
goto :EOF

:WriteWarn
echo [WARN] %~1
goto :EOF

:WriteErrorLog
echo [ERROR] %~1
goto :EOF

:WriteSuccess
echo [SUCCESS] %~1
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 获取卸载目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:GetUninstallDirectory
if defined INPUT_DIR (
    set "UNINSTALL_DIR=%INPUT_DIR%"
) else (
    set "UNINSTALL_DIR=%SCRIPT_DIR%"
)

call :WriteInfo "卸载目录: %UNINSTALL_DIR%"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 验证卸载目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:TestUninstallDirectory
if not exist "%UNINSTALL_DIR%" (
    call :WriteErrorLog "目录不存在: %UNINSTALL_DIR%"
    exit /b 1
)

:: 检查是否是 PreCI 安装目录
set "IS_PRECI_DIR=0"
if exist "%UNINSTALL_DIR%\preci.exe" set "IS_PRECI_DIR=1"
if exist "%UNINSTALL_DIR%\preci-server.exe" set "IS_PRECI_DIR=1"
if exist "%UNINSTALL_DIR%\preci-mcp.exe" set "IS_PRECI_DIR=1"

if %IS_PRECI_DIR%==0 (
    echo [WARN] 目录 %UNINSTALL_DIR% 似乎不是 PreCI 安装目录
    if %SILENT_MODE%==0 (
        set /p "CONFIRM=是否继续卸载? [y/N]: "
        if /i not "!CONFIRM!"=="y" exit /b 1
    )
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 确认卸载
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:ConfirmUninstall
if %SILENT_MODE%==1 (
    call :WriteInfo "静默模式，跳过确认"
    goto :EOF
)

echo.
echo ============================================
echo   PreCI 卸载确认
echo ============================================
echo.
echo 警告: 此操作将:
echo   1. 停止 PreCI 服务
echo   2. 删除目录: %UNINSTALL_DIR%
echo   3. 清理环境变量配置
echo.
echo 此操作不可恢复!
echo.

set /p "CONFIRM=确认卸载 PreCI? [y/N]: "
if /i "%CONFIRM%"=="y" (
    call :WriteInfo "开始卸载..."
) else (
    call :WriteInfo "卸载已取消"
    exit /b 1
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 停止 PreCI 服务
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:StopPreciService
call :WriteInfo "停止 PreCI 服务..."

if exist "%UNINSTALL_DIR%\preci.exe" (
    "%UNINSTALL_DIR%\preci.exe" server stop 2>nul
    if !errorlevel!==0 (
        call :WriteSuccess "PreCI 服务已停止"
    ) else (
        call :WriteWarn "PreCI 服务可能未运行或停止失败"
    )
) else (
    call :WriteWarn "未找到 preci.exe，跳过服务停止"
)

:: 等待进程退出
timeout /t 1 /nobreak >nul

:: 强制结束可能残留的进程
for %%P in (preci preci-server preci-mcp) do (
    tasklist /fi "imagename eq %%P.exe" 2>nul | findstr /i "%%P.exe" >nul 2>&1
    if !errorlevel!==0 (
        call :WriteInfo "强制结束进程: %%P"
        taskkill /f /im "%%P.exe" >nul 2>&1
    )
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 清理环境变量
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:RemoveEnvironmentVariables
call :WriteInfo "清理环境变量..."

set "CLEANED=0"

:: 清理 PRECI_HOME
set "CURRENT_PRECI_HOME="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v PRECI_HOME 2^>nul') do set "CURRENT_PRECI_HOME=%%B"

if defined CURRENT_PRECI_HOME (
    call :WriteInfo "清理 PRECI_HOME 环境变量"
    reg delete "HKCU\Environment" /v PRECI_HOME /f >nul 2>&1
    set "CLEANED=1"
)

:: 清理 PATH 中的安装目录
set "USER_PATH="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v Path 2^>nul') do set "USER_PATH=%%B"

if defined USER_PATH (
    set "NEW_PATH="
    set "PATH_CHANGED=0"

    for %%P in ("!USER_PATH:;=" "!") do (
        set "ENTRY=%%~P"
        if not "!ENTRY!"=="" (
            if /i "!ENTRY!"=="%UNINSTALL_DIR%" (
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
        call :WriteInfo "从 PATH 中移除安装目录"
        if defined NEW_PATH (
            reg add "HKCU\Environment" /v Path /t REG_EXPAND_SZ /d "!NEW_PATH!" /f >nul 2>&1
        ) else (
            reg delete "HKCU\Environment" /v Path /f >nul 2>&1
        )
        set "CLEANED=1"
    )
)

if %CLEANED%==1 (
    call :WriteSuccess "环境变量已清理"
    echo.
    call :WriteWarn "环境变量已清理，重新打开终端窗口后生效"
) else (
    call :WriteInfo "未找到需要清理的环境变量配置"
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 删除安装目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:RemoveInstallation
call :WriteInfo "删除安装目录..."

if not exist "%UNINSTALL_DIR%" (
    call :WriteWarn "目录不存在: %UNINSTALL_DIR%"
    goto :EOF
)

:: 安全检查：确保不会删除重要目录
if /i "%UNINSTALL_DIR%"=="%SystemRoot%" (
    call :WriteErrorLog "拒绝删除系统目录: %UNINSTALL_DIR%"
    exit /b 1
)
if /i "%UNINSTALL_DIR%"=="%ProgramFiles%" (
    call :WriteErrorLog "拒绝删除系统目录: %UNINSTALL_DIR%"
    exit /b 1
)
if /i "%UNINSTALL_DIR%"=="%USERPROFILE%" (
    call :WriteErrorLog "拒绝删除系统目录: %UNINSTALL_DIR%"
    exit /b 1
)
if /i "%UNINSTALL_DIR%"=="C:\" (
    call :WriteErrorLog "拒绝删除系统目录: %UNINSTALL_DIR%"
    exit /b 1
)

:: 如果当前在要删除的目录中，先切换到用户目录
cd /d "%USERPROFILE%" 2>nul

:: 删除目录
rd /s /q "%UNINSTALL_DIR%" 2>nul
if exist "%UNINSTALL_DIR%" (
    call :WriteErrorLog "删除目录失败: %UNINSTALL_DIR%"
    call :WriteErrorLog "可能有文件被占用，请关闭相关程序后重试"
    exit /b 1
)

call :WriteSuccess "已删除: %UNINSTALL_DIR%"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 打印卸载完成信息
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:ShowUninstallInfo
echo.
echo ============================================
echo   PreCI 卸载完成
echo ============================================
echo.
echo PreCI 已成功卸载
echo.
echo 后续步骤:
echo   1. 重新打开终端窗口使环境变量更改生效
echo.
echo ============================================
echo   感谢使用 PreCI
echo ============================================
echo.
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 错误退出
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:ErrorExit
call :WriteErrorLog "卸载过程中发生错误"
call :WriteInfo "部分文件可能未被清理，请手动检查"
exit /b 1
