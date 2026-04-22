@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: PreCI 安装脚本 (Windows BAT)
::
:: 功能说明:
::   - 作为 install.ps1 的兜底方案，适用于没有 PowerShell 的环境
::   - 检测系统环境
::   - 拷贝 PreCI 相关文件到目标目录
::   - 设置环境变量
::   - 支持 Windows 10/11 (AMD64/ARM64) 环境
::
:: 使用方法:
::   install.bat [安装目录]
::
:: 作者: PreCI Team
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:: 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

:: 默认安装目录
set "DEFAULT_INSTALL_DIR=%USERPROFILE%\PreCI"

:: 实际安装目录
set "INSTALL_DIR="

:: 需要拷贝的文件列表
set "FILES_TO_COPY=preci.exe preci-server.exe preci-mcp.exe config checkerset install.ps1 uninstall.ps1 uninstall_old_preci.ps1 install.bat uninstall.bat uninstall_old_preci.bat"

:: 需要创建的空目录
set "DIRS_TO_CREATE=db log tool"

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 主入口
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

echo.
echo ============================================
echo   PreCI 安装程序
echo ============================================
echo.

:: 系统检测
call :GetSystemInfo

:: 验证源文件
call :TestSourceFiles
if errorlevel 1 goto :ErrorExit

:: 获取安装目录
call :GetInstallDirectory %1
if errorlevel 1 goto :ErrorExit

:: 检查环境变量中的其他 PreCI 目录
call :TestOldPreciEnvironment
if errorlevel 1 goto :ErrorExit

:: 准备安装目录
call :InitializeInstallDirectory
if errorlevel 1 goto :ErrorExit

:: 拷贝文件
call :CopyFiles
if errorlevel 1 goto :ErrorExit

:: 创建必要的目录
call :CreateDirectories

:: 设置环境变量
call :SetEnvironmentVariables

:: 验证安装
call :TestInstallation

:: 打印安装信息
call :ShowInstallationInfo

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
:: 系统检测
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:GetSystemInfo
call :WriteInfo "检测系统环境..."
echo [INFO] 系统架构: %PROCESSOR_ARCHITECTURE%
echo [INFO] 操作系统: %OS%
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 文件验证
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:TestSourceFiles
call :WriteInfo "验证源文件..."

set "MISSING_COUNT=0"
for %%F in (preci.exe preci-server.exe preci-mcp.exe) do (
    if not exist "%SCRIPT_DIR%\%%F" (
        echo [ERROR] 缺失文件: %%F
        set /a MISSING_COUNT+=1
    )
)

if %MISSING_COUNT% GTR 0 (
    call :WriteErrorLog "必需文件缺失，请检查安装包完整性"
    exit /b 1
)

call :WriteInfo "源文件验证通过"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 获取安装目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:GetInstallDirectory
if not "%~1"=="" (
    set "INSTALL_DIR=%~1"
) else (
    echo.
    set /p "INSTALL_DIR=请输入安装目录 [默认: %DEFAULT_INSTALL_DIR%]: "
    if "!INSTALL_DIR!"=="" set "INSTALL_DIR=%DEFAULT_INSTALL_DIR%"
)

call :WriteInfo "安装目录: %INSTALL_DIR%"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 检查环境变量中的其他 PreCI 目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:TestOldPreciEnvironment
call :WriteInfo "检查环境变量中的其他 PreCI 安装..."

set "FOUND_OLD=0"

:: 检查 PRECI_HOME
set "CURRENT_PRECI_HOME="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v PRECI_HOME 2^>nul') do set "CURRENT_PRECI_HOME=%%B"

if defined CURRENT_PRECI_HOME (
    if not "%CURRENT_PRECI_HOME%"=="%INSTALL_DIR%" (
        echo [WARN] 发现旧的 PRECI_HOME: %CURRENT_PRECI_HOME%
        set "FOUND_OLD=1"
    )
)

:: 检查 PATH 中是否有其他 PreCI 路径
set "USER_PATH="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v Path 2^>nul') do set "USER_PATH=%%B"

if defined USER_PATH (
    for %%P in ("!USER_PATH:;=" "!") do (
        set "ENTRY=%%~P"
        if not "!ENTRY!"=="" (
            echo !ENTRY! | findstr /i "PreCI" >nul 2>&1
            if !errorlevel!==0 (
                if not "!ENTRY!"=="%INSTALL_DIR%" (
                    echo [WARN] PATH 中发现旧的 PreCI 路径: !ENTRY!
                    set "FOUND_OLD=1"
                )
            )
        )
    )
)

if %FOUND_OLD%==1 (
    echo.
    echo [WARN] 建议手动清理旧的 PreCI 环境变量:
    echo   1. 按 Win+R，输入: sysdm.cpl
    echo   2. 点击 '高级' -^> '环境变量'
    echo   3. 删除旧的 PRECI_HOME 变量和 PATH 中的旧 PreCI 路径
    echo.
    set /p "CONFIRM=是否继续安装? [y/N]: "
    if /i not "!CONFIRM!"=="y" (
        call :WriteInfo "安装已取消"
        exit /b 1
    )
) else (
    call :WriteInfo "未发现其他 PreCI 安装路径"
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 准备安装目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:InitializeInstallDirectory
:: 检查目录是否存在且不为空
if exist "%INSTALL_DIR%\*" (
    dir /b "%INSTALL_DIR%" 2>nul | findstr "." >nul 2>&1
    if !errorlevel!==0 (
        echo [WARN] 目录 %INSTALL_DIR% 已存在且不为空
        set /p "CONFIRM=是否覆盖安装? [y/N]: "
        if /i not "!CONFIRM!"=="y" (
            call :WriteInfo "安装已取消"
            exit /b 1
        )

        :: 停止 PreCI 服务（避免可执行文件被占用）
        call :WriteInfo "尝试停止 PreCI 服务..."
        if exist "%INSTALL_DIR%\preci.exe" (
            "%INSTALL_DIR%\preci.exe" server stop 2>nul
        )
        taskkill /f /im preci-server.exe 2>nul
        taskkill /f /im preci.exe 2>nul
        taskkill /f /im preci-mcp.exe 2>nul
        timeout /t 2 /nobreak >nul

        :: 只清理需要安装的文件，保留 log 目录（避免被 IDE 插件占用的日志文件导致安装失败）
        call :WriteInfo "清理旧的安装文件..."
        for %%F in (%FILES_TO_COPY%) do (
            set "TARGET_CLEAN=%INSTALL_DIR%\%%F"
            if exist "!TARGET_CLEAN!\*" (
                call :WriteInfo "清理目录: %%F"
                rd /s /q "!TARGET_CLEAN!" 2>nul
            ) else if exist "!TARGET_CLEAN!" (
                call :WriteInfo "清理文件: %%F"
                del /f /q "!TARGET_CLEAN!" 2>nul
            )
        )
        :: 清理 DIRS_TO_CREATE 中除 log 以外的目录
        for %%D in (%DIRS_TO_CREATE%) do (
            if /i not "%%D"=="log" (
                set "TARGET_CLEAN=%INSTALL_DIR%\%%D"
                if exist "!TARGET_CLEAN!\*" (
                    call :WriteInfo "清理目录: %%D"
                    rd /s /q "!TARGET_CLEAN!" 2>nul
                )
            )
        )
        call :WriteSuccess "旧文件清理完成"
    )
)

:: 创建安装目录
if not exist "%INSTALL_DIR%" (
    call :WriteInfo "创建安装目录: %INSTALL_DIR%"
    mkdir "%INSTALL_DIR%" 2>nul
    if errorlevel 1 (
        call :WriteErrorLog "创建安装目录失败"
        exit /b 1
    )
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 拷贝文件
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:CopyFiles
call :WriteInfo "开始拷贝文件..."

for %%F in (%FILES_TO_COPY%) do (
    set "SOURCE=%SCRIPT_DIR%\%%F"
    set "TARGET=%INSTALL_DIR%\%%F"

    if exist "!SOURCE!\*" (
        :: 拷贝目录
        call :WriteInfo "拷贝目录: %%F"
        xcopy "!SOURCE!" "!TARGET!" /E /I /Y /Q >nul 2>&1
    ) else if exist "!SOURCE!" (
        :: 拷贝文件
        call :WriteInfo "拷贝文件: %%F"
        copy /y "!SOURCE!" "!TARGET!" >nul 2>&1
    ) else (
        echo [WARN] 跳过不存在的文件: %%F
    )
)

call :WriteSuccess "文件拷贝完成"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 创建必要的目录
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:CreateDirectories
call :WriteInfo "创建必要的目录..."

for %%D in (%DIRS_TO_CREATE%) do (
    if not exist "%INSTALL_DIR%\%%D" (
        mkdir "%INSTALL_DIR%\%%D" 2>nul
        call :WriteInfo "创建目录: %%D"
    )
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 设置环境变量
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:SetEnvironmentVariables
call :WriteInfo "配置环境变量..."

:: 设置 PRECI_HOME（用户级别）
set "CURRENT_PRECI_HOME="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v PRECI_HOME 2^>nul') do set "CURRENT_PRECI_HOME=%%B"

if "%CURRENT_PRECI_HOME%"=="%INSTALL_DIR%" (
    call :WriteInfo "PRECI_HOME 已配置，跳过"
) else (
    reg add "HKCU\Environment" /v PRECI_HOME /t REG_SZ /d "%INSTALL_DIR%" /f >nul 2>&1
    call :WriteSuccess "已设置 PRECI_HOME = %INSTALL_DIR%"
)

:: 检查 PATH 中是否已包含安装目录
set "USER_PATH="
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v Path 2^>nul') do set "USER_PATH=%%B"

set "PATH_CONTAINS=0"
if defined USER_PATH (
    echo !USER_PATH! | findstr /i /c:"%INSTALL_DIR%" >nul 2>&1
    if !errorlevel!==0 set "PATH_CONTAINS=1"
)

if %PATH_CONTAINS%==0 (
    if defined USER_PATH (
        set "NEW_PATH=%INSTALL_DIR%;!USER_PATH!"
    ) else (
        set "NEW_PATH=%INSTALL_DIR%"
    )
    reg add "HKCU\Environment" /v Path /t REG_EXPAND_SZ /d "!NEW_PATH!" /f >nul 2>&1
    call :WriteSuccess "已将安装目录添加到 PATH"
) else (
    call :WriteInfo "PATH 已包含安装目录，跳过"
)

:: 更新当前会话
set "PRECI_HOME=%INSTALL_DIR%"
set "PATH=%INSTALL_DIR%;%PATH%"

:: 通知系统环境变量已更改
call :WriteWarn "环境变量已配置，重新打开终端窗口后生效"
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 验证安装
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:TestInstallation
call :WriteInfo "验证安装..."

set "ALL_OK=1"
for %%E in (preci.exe preci-server.exe preci-mcp.exe) do (
    if exist "%INSTALL_DIR%\%%E" (
        echo [INFO] √ %%E 已安装
    ) else (
        echo [ERROR] × %%E 未找到
        set "ALL_OK=0"
    )
)

if %ALL_OK%==1 (
    call :WriteSuccess "安装验证通过"
) else (
    call :WriteErrorLog "安装验证失败"
)
goto :EOF

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 打印安装信息
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:ShowInstallationInfo
echo.
echo ============================================
echo   PreCI 安装完成
echo ============================================
echo.
echo 安装目录: %INSTALL_DIR%
echo.
echo 快速开始:
echo   1. 重新打开终端窗口使环境变量生效
echo.
echo   2. 验证安装:
echo      preci version
echo.
echo   3. 查看帮助:
echo      preci --help
echo.
echo 卸载方法:
echo   运行卸载脚本:
echo      %INSTALL_DIR%\uninstall.bat
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
call :WriteErrorLog "安装过程中发生错误"
if exist "%INSTALL_DIR%" (
    set /p "CONFIRM=是否删除已创建的安装目录? [y/N]: "
    if /i "!CONFIRM!"=="y" (
        rd /s /q "%INSTALL_DIR%" 2>nul
        call :WriteInfo "已删除: %INSTALL_DIR%"
    )
)
exit /b 1
