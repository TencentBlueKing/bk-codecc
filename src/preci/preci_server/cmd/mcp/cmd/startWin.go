//go:build windows

package cmd

import (
	"os"
	"os/exec"
	"syscall"
)

// setProcAttributes 设置进程属性，使子进程独立于父进程（Windows 实现）
func setProcAttributes(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{
		CreationFlags: syscall.CREATE_NEW_PROCESS_GROUP,
	}
}

// killProcess 终止进程（Windows 实现）
func killProcess(proc *os.Process) error {
	// Windows 上直接使用 Kill
	return proc.Kill()
}
