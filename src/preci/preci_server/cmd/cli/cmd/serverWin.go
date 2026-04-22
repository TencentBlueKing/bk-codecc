//go:build windows

package cmd

import (
	"os/exec"
	"syscall"
)

const _DETACHED_PROCESS = 0x00000008

// setProcAttributes 设置进程属性,使子进程独立于父进程
func setProcAttributes(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{
		// CREATE_NEW_PROCESS_GROUP: 阻止 CTRL_C_EVENT 传播到子进程
		// DETACHED_PROCESS: 完全脱离父控制台，避免关闭 CMD 窗口时子进程收到 CTRL_CLOSE_EVENT 被终止
		CreationFlags: syscall.CREATE_NEW_PROCESS_GROUP | _DETACHED_PROCESS,
	}
}
