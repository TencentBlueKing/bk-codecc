//go:build !windows

package cmd

import (
	"os/exec"
	"syscall"
)

// setProcAttributes 设置进程属性,使子进程独立于父进程
func setProcAttributes(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{
		Setpgid: true, // 创建新的进程组,使子进程不受父进程终端信号影响
	}
}
