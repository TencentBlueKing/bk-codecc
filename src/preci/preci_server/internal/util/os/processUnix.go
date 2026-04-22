//go:build !windows

package os

import (
	"os"
	"syscall"
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

// IsProcessAlive 判断进程是否存活（Unix/Linux 实现）
func IsProcessAlive(proc *os.Process) bool {
	if proc == nil {
		return false
	}

	// 在 Unix/Linux 系统上，发送信号 0 可以检查进程是否存在
	// 如果进程存在，返回 nil；如果不存在，返回错误
	err := proc.Signal(syscall.Signal(0))
	return err == nil
}
