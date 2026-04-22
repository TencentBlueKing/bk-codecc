//go:build windows

package os

import (
	"os"
	"syscall"
)

var (
	kernel32        = syscall.NewLazyDLL("kernel32.dll")
	procOpenProcess = kernel32.NewProc("OpenProcess")
	procCloseHandle = kernel32.NewProc("CloseHandle")
)

const (
	ProcessQueryLimitedInformation = 0x1000
)

// GetProcess 获取进程
func GetProcess(pid int) *os.Process {
	proc, err := os.FindProcess(pid)
	if err != nil {
		return nil
	}

	if IsProcessAlive(proc) {
		return proc
	}

	return nil
}

// IsProcessAlive 判断进程是否存活（Windows 实现）
func IsProcessAlive(proc *os.Process) bool {
	if proc == nil {
		return false
	}

	// 尝试打开进程句柄
	handle, _, _ := procOpenProcess.Call(
		uintptr(ProcessQueryLimitedInformation),
		uintptr(0),
		uintptr(proc.Pid),
	)

	if handle == 0 {
		// 无法打开进程句柄，说明进程不存在
		return false
	}

	// 关闭句柄
	defer procCloseHandle.Call(handle)

	// 如果能成功打开句柄，说明进程存在
	return true
}
