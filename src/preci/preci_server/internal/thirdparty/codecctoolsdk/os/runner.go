package os

import (
	"context"
	"log"
	"os"
	"os/exec"
	"runtime"
	"time"
)

const (
	NoLog     = 0 // 不输出日志
	FileLog   = 1 // 输出到文件
	StringLog = 2 // 以字符串的形式输出日志
)

// CmdRunner 命令执行器
type CmdRunner struct {
	coreCmd string   // 核心命令
	env     []string // 环境变量
}

// NewCmdRunner 创建命令执行器
func NewCmdRunner(coreCmd string, env ...string) *CmdRunner {
	return &CmdRunner{
		coreCmd: coreCmd,
		env:     env,
	}
}

// RunConfig 运行配置
type RunConfig struct {
	LogMode int           // 日志模式
	LogPath string        // 日志文件路径, 若LogMode=1, 必须指定
	Timeout time.Duration // 超时时间, 选填
}

// Run 执行命令
// config: 运行配置
// 返回值: 日志内容（仅在LogMode=2时有效）和可能的错误
func (r *CmdRunner) Run(config *RunConfig) ([]byte, error) {
	ctx := context.Background()

	// 设置超时时间
	if config.Timeout > 0 {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, config.Timeout)
		defer cancel()
	}

	// 根据操作系统创建命令
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "windows":
		cmd = exec.CommandContext(ctx, "cmd", "/C", r.coreCmd)
	default:
		cmd = exec.CommandContext(ctx, "sh", "-c", r.coreCmd)
	}

	// 设置环境变量
	if len(r.env) > 0 {
		cmd.Env = append(os.Environ(), r.env...)
	}

	// 根据日志模式配置输出
	switch config.LogMode {
	case StringLog:
		return cmd.CombinedOutput()
	case FileLog:
		logFileHandle, err := os.Create(config.LogPath)
		if err != nil {
			log.Printf("create log file failed: %v", err)
		} else {
			defer logFileHandle.Close()
			cmd.Stdout = logFileHandle
			cmd.Stderr = logFileHandle
		}
	default:
		// 不输出日志
	}

	return nil, cmd.Run()
}
