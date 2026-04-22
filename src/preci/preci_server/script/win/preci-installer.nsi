;==============================================================================
; PreCI Windows Installer - NSIS Script
;
; Build:
;   makensis /DVERSION=1.0.0 preci-installer.nsi
;
; Expects all distributable files alongside this script:
;
;     preci.exe
;     preci-server.exe
;     preci-mcp.exe
;     preci-updater.exe
;     config\            (directory, copied recursively)
;     checkerset\        (directory, copied recursively)
;     install.ps1
;     uninstall.ps1
;     uninstall_old_preci.ps1
;     install.bat
;     uninstall.bat
;     uninstall_old_preci.bat
;
;==============================================================================

!ifndef VERSION
  !define VERSION "dev"
!endif

!define PRODUCT_NAME      "PreCI"
!define PRODUCT_PUBLISHER  "PreCI Team"
!define PRODUCT_REG_KEY    "Software\PreCI"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\PreCI"

;------------------------------------------------------------------------------
; General
;------------------------------------------------------------------------------
Name "${PRODUCT_NAME} ${VERSION}"
OutFile "PreCI-Setup.exe"
InstallDir "$PROFILE\PreCI"
InstallDirRegKey HKCU "${PRODUCT_REG_KEY}" "InstallDir"
RequestExecutionLevel user
Unicode True
SetCompressor /SOLID lzma

;------------------------------------------------------------------------------
; Includes
;------------------------------------------------------------------------------
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "WinMessages.nsh"
!include "StrFunc.nsh"

; Declare StrFunc functions (installer + uninstaller variants)
${StrStr}
${StrRep}
${UnStrStr}
${UnStrRep}

;------------------------------------------------------------------------------
; MUI Settings
;------------------------------------------------------------------------------
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"

!define MUI_WELCOMEPAGE_TITLE "PreCI ${VERSION} 安装向导"
!define MUI_WELCOMEPAGE_TEXT "此向导将引导您完成 PreCI ${VERSION} 的安装。$\r$\n$\r$\n建议在安装前关闭所有正在使用 PreCI 的程序。$\r$\n$\r$\n点击「下一步」继续。"

!define MUI_FINISHPAGE_TITLE "安装完成"
!define MUI_FINISHPAGE_TEXT "PreCI ${VERSION} 安装完成。$\r$\n$\r$\n请重新打开终端窗口使环境变量生效。$\r$\n$\r$\n验证安装：$\r$\n    preci version"

;------------------------------------------------------------------------------
; Pages
;------------------------------------------------------------------------------
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

;------------------------------------------------------------------------------
; Languages
;------------------------------------------------------------------------------
!insertmacro MUI_LANGUAGE "SimpChinese"
!insertmacro MUI_LANGUAGE "English"

;==============================================================================
; Installer Callbacks
;==============================================================================
Function .onInit
  ; Restore previous install directory from registry when present
  ReadRegStr $0 HKCU "${PRODUCT_REG_KEY}" "InstallDir"
  ${If} $0 != ""
    StrCpy $INSTDIR $0
  ${EndIf}
FunctionEnd

;==============================================================================
; Install Section
;==============================================================================
Section "Install" SecInstall
  SetOutPath $INSTDIR

  ;---------------------------------------------------------------------------
  ; 1. Stop existing PreCI service / processes
  ;---------------------------------------------------------------------------
  DetailPrint "停止 PreCI 服务..."
  IfFileExists "$INSTDIR\preci.exe" 0 _skip_stop
    nsExec::ExecToLog '"$INSTDIR\preci.exe" server stop'
    Pop $0
  _skip_stop:

  nsExec::ExecToLog 'taskkill /f /im preci-server.exe'
  Pop $0
  nsExec::ExecToLog 'taskkill /f /im preci.exe'
  Pop $0
  nsExec::ExecToLog 'taskkill /f /im preci-mcp.exe'
  Pop $0
  nsExec::ExecToLog 'taskkill /f /im preci-updater.exe'
  Pop $0
  Sleep 2000

  ;---------------------------------------------------------------------------
  ; 2. Clean old installed files (preserve log directory)
  ;---------------------------------------------------------------------------
  DetailPrint "清理旧文件..."
  Delete "$INSTDIR\preci.exe"
  Delete "$INSTDIR\preci-server.exe"
  Delete "$INSTDIR\preci-mcp.exe"
  Delete "$INSTDIR\preci-updater.exe"
  Delete "$INSTDIR\install.ps1"
  Delete "$INSTDIR\uninstall.ps1"
  Delete "$INSTDIR\uninstall_old_preci.ps1"
  Delete "$INSTDIR\install.bat"
  Delete "$INSTDIR\uninstall.bat"
  Delete "$INSTDIR\uninstall_old_preci.bat"
  RMDir /r "$INSTDIR\config"
  RMDir /r "$INSTDIR\checkerset"
  RMDir /r "$INSTDIR\db"
  RMDir /r "$INSTDIR\tool"
  ; NOTE: log directory is intentionally preserved

  ;---------------------------------------------------------------------------
  ; 3. Install files
  ;---------------------------------------------------------------------------
  DetailPrint "安装文件..."

  ; Force overwrite all existing files
  SetOverwrite on

  SetOutPath $INSTDIR
  File "preci.exe"
  File "preci-server.exe"
  File "preci-mcp.exe"
  File "preci-updater.exe"
  File "install.ps1"
  File "uninstall.ps1"
  File "uninstall_old_preci.ps1"
  File "install.bat"
  File "uninstall.bat"
  File "uninstall_old_preci.bat"

  SetOutPath "$INSTDIR\config"
  File /r "config\*"

  SetOutPath "$INSTDIR\checkerset"
  File /r "checkerset\*"

  ;---------------------------------------------------------------------------
  ; 4. Create required empty directories
  ;---------------------------------------------------------------------------
  CreateDirectory "$INSTDIR\db"
  CreateDirectory "$INSTDIR\log"
  CreateDirectory "$INSTDIR\tool"

  ;---------------------------------------------------------------------------
  ; 5. Write uninstaller
  ;---------------------------------------------------------------------------
  SetOutPath $INSTDIR
  WriteUninstaller "$INSTDIR\uninstall.exe"

  ;---------------------------------------------------------------------------
  ; 6. Environment variables (user level)
  ;---------------------------------------------------------------------------
  DetailPrint "配置环境变量..."

  ; PRECI_HOME
  WriteRegStr HKCU "Environment" "PRECI_HOME" "$INSTDIR"
  DetailPrint "已设置 PRECI_HOME = $INSTDIR"

  ; Add $INSTDIR to user PATH (only if not already present)
  Call AddToUserPath

  ; Notify the system that environment variables have changed
  SendMessage ${HWND_BROADCAST} ${WM_SETTINGCHANGE} 0 "STR:Environment" /TIMEOUT=5000

  ;---------------------------------------------------------------------------
  ; 7. Register in Add/Remove Programs
  ;---------------------------------------------------------------------------
  WriteRegStr   HKCU "${PRODUCT_UNINST_KEY}" "DisplayName"     "${PRODUCT_NAME} ${VERSION}"
  WriteRegStr   HKCU "${PRODUCT_UNINST_KEY}" "UninstallString"  '"$INSTDIR\uninstall.exe"'
  WriteRegStr   HKCU "${PRODUCT_UNINST_KEY}" "QuietUninstallString" '"$INSTDIR\uninstall.exe" /S'
  WriteRegStr   HKCU "${PRODUCT_UNINST_KEY}" "InstallLocation"  "$INSTDIR"
  WriteRegStr   HKCU "${PRODUCT_UNINST_KEY}" "DisplayVersion"   "${VERSION}"
  WriteRegStr   HKCU "${PRODUCT_UNINST_KEY}" "Publisher"         "${PRODUCT_PUBLISHER}"
  WriteRegDWORD HKCU "${PRODUCT_UNINST_KEY}" "NoModify" 1
  WriteRegDWORD HKCU "${PRODUCT_UNINST_KEY}" "NoRepair" 1

  ; Store install metadata
  WriteRegStr HKCU "${PRODUCT_REG_KEY}" "InstallDir" "$INSTDIR"
  WriteRegStr HKCU "${PRODUCT_REG_KEY}" "Version"    "${VERSION}"

  DetailPrint "安装完成"
SectionEnd

;==============================================================================
; Uninstaller Section
;==============================================================================
Section "Uninstall"
  ;---------------------------------------------------------------------------
  ; 1. Stop PreCI service / processes
  ;---------------------------------------------------------------------------
  DetailPrint "停止 PreCI 服务..."
  IfFileExists "$INSTDIR\preci.exe" 0 _un_skip_stop
    nsExec::ExecToLog '"$INSTDIR\preci.exe" server stop'
    Pop $0
  _un_skip_stop:

  nsExec::ExecToLog 'taskkill /f /im preci-server.exe'
  Pop $0
  nsExec::ExecToLog 'taskkill /f /im preci.exe'
  Pop $0
  nsExec::ExecToLog 'taskkill /f /im preci-mcp.exe'
  Pop $0
  nsExec::ExecToLog 'taskkill /f /im preci-updater.exe'
  Pop $0
  Sleep 2000

  ;---------------------------------------------------------------------------
  ; 2. Clean environment variables (user level)
  ;---------------------------------------------------------------------------
  DetailPrint "清理环境变量..."

  DeleteRegValue HKCU "Environment" "PRECI_HOME"
  Call un.RemoveFromUserPath

  SendMessage ${HWND_BROADCAST} ${WM_SETTINGCHANGE} 0 "STR:Environment" /TIMEOUT=5000

  ;---------------------------------------------------------------------------
  ; 3. Delete installed files
  ;---------------------------------------------------------------------------
  DetailPrint "删除文件..."

  Delete "$INSTDIR\preci.exe"
  Delete "$INSTDIR\preci-server.exe"
  Delete "$INSTDIR\preci-mcp.exe"
  Delete "$INSTDIR\preci-updater.exe"
  Delete "$INSTDIR\install.ps1"
  Delete "$INSTDIR\uninstall.ps1"
  Delete "$INSTDIR\uninstall_old_preci.ps1"
  Delete "$INSTDIR\install.bat"
  Delete "$INSTDIR\uninstall.bat"
  Delete "$INSTDIR\uninstall_old_preci.bat"
  Delete "$INSTDIR\uninstall.exe"

  RMDir /r "$INSTDIR\config"
  RMDir /r "$INSTDIR\checkerset"
  RMDir /r "$INSTDIR\db"
  RMDir /r "$INSTDIR\log"
  RMDir /r "$INSTDIR\tool"

  ; Remove the install directory itself (succeeds only when empty)
  RMDir "$INSTDIR"

  ;---------------------------------------------------------------------------
  ; 4. Remove registry entries
  ;---------------------------------------------------------------------------
  DeleteRegKey HKCU "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKCU "${PRODUCT_REG_KEY}"

  DetailPrint "卸载完成"
SectionEnd

;==============================================================================
; Helper: add $INSTDIR to the user-level PATH
;
; Uses semicolon-sentinel technique for exact-match detection:
;   wraps the existing PATH as ";PATH;" and searches for ";$INSTDIR;".
;==============================================================================
Function AddToUserPath
  ReadRegStr $0 HKCU "Environment" "Path"

  ${If} $0 == ""
    ; PATH is empty -- set it to $INSTDIR
    WriteRegExpandStr HKCU "Environment" "Path" "$INSTDIR"
    DetailPrint "已将安装目录添加到 PATH"
  ${Else}
    ; Wrap with sentinels for exact matching
    StrCpy $1 ";$0;"
    ${StrStr} $2 $1 ";$INSTDIR;"
    ${If} $2 == ""
      ; Not found -- prepend
      WriteRegExpandStr HKCU "Environment" "Path" "$INSTDIR;$0"
      DetailPrint "已将安装目录添加到 PATH"
    ${Else}
      DetailPrint "PATH 已包含安装目录，跳过"
    ${EndIf}
  ${EndIf}
FunctionEnd

;==============================================================================
; Helper (uninstaller): remove $INSTDIR from the user-level PATH
;
; Strategy:
;   1. Wrap existing PATH: ";PATH;"
;   2. Replace ";$INSTDIR;" with ";"
;   3. Strip leading/trailing sentinel semicolons
;   4. Write back (or delete if empty)
;==============================================================================
Function un.RemoveFromUserPath
  ReadRegStr $0 HKCU "Environment" "Path"

  ${If} $0 == ""
    Return
  ${EndIf}

  ; Exact match: PATH is just $INSTDIR
  ${If} $0 == "$INSTDIR"
    DeleteRegValue HKCU "Environment" "Path"
    DetailPrint "已从 PATH 中移除安装目录"
    Return
  ${EndIf}

  ; Wrap with sentinel semicolons
  StrCpy $1 ";$0;"

  ; Replace ";$INSTDIR;" -> ";"
  ${UnStrRep} $2 $1 ";$INSTDIR;" ";"

  ; If nothing changed, $INSTDIR was not in PATH
  ${If} $2 == $1
    Return
  ${EndIf}

  ; Strip leading semicolon
  StrCpy $3 $2 1          ; first character
  ${If} $3 == ";"
    StrLen $4 $2
    IntOp $4 $4 - 1
    StrCpy $2 $2 $4 1     ; skip first char
  ${EndIf}

  ; Strip trailing semicolon
  StrLen $4 $2
  ${If} $4 > 0
    IntOp $5 $4 - 1
    StrCpy $3 $2 1 $5     ; last character
    ${If} $3 == ";"
      StrCpy $2 $2 $5     ; drop last char
    ${EndIf}
  ${EndIf}

  ${If} $2 == ""
    DeleteRegValue HKCU "Environment" "Path"
  ${Else}
    WriteRegExpandStr HKCU "Environment" "Path" "$2"
  ${EndIf}

  DetailPrint "已从 PATH 中移除安装目录"
FunctionEnd
