//go:build !windows

package cmd

import (
	"os"
	"os/exec"
	"syscall"
)

// setProcAttributes 设置进程属性，使子进程独立于父进程（Unix/Linux 实现）
func setProcAttributes(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{
		Setpgid: true, // 创建新的进程组，使子进程不受父进程终端信号影响
	}
}

// killProcess 终止进程（Unix/Linux 实现）
func killProcess(proc *os.Process) error {
	// 先尝试发送 SIGTERM 让进程优雅退出
	if err := proc.Signal(syscall.SIGTERM); err != nil {
		// 如果 SIGTERM 失败，尝试 SIGKILL
		return proc.Kill()
	}
	return nil
}
