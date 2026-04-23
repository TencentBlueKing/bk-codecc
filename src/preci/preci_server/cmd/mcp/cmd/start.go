package cmd

import (
	"codecc/preci_server/internal/util/perror"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"time"

	utilos "codecc/preci_server/internal/util/os"
	"github.com/spf13/cobra"
)

var mcpPort int

var startCmd = &cobra.Command{
	Use:   "start",
	Short: "启动 MCP 服务（守护进程模式）",
	Long: `以守护进程模式启动 MCP 服务器。

服务启动后会将进程 ID 保存到 config/mcp-server.pid 文件中，
端口号保存到 config/mcp-server.port 文件中。

示例：
  preci-mcp start          # 使用自动分配端口启动
  preci-mcp start -p 8080  # 使用指定端口启动`,
	RunE: runStart,
}

func init() {
	rootCmd.AddCommand(startCmd)
	startCmd.Flags().IntVarP(&mcpPort, "port", "p", 0, "MCP 服务端口号（0 表示自动分配）")
}

// runStart 启动 MCP 服务
func runStart(cmd *cobra.Command, args []string) error {
	// 检查是否已有运行中的实例
	pidFile := filepath.Join(InstallDir, "config", "mcp-server.pid")
	if pidData, err := os.ReadFile(pidFile); err == nil {
		if pid, err := strconv.Atoi(string(pidData)); err == nil {
			if proc := utilos.GetProcess(pid); proc != nil {
				return perror.ErrMCPServerRunning
			}
		}
		// PID 文件存在但进程不存在，删除旧的 PID 文件
		os.Remove(pidFile)
	}

	log.Println("正在启动 MCP 服务...")

	// 获取当前可执行文件路径
	exePath, err := os.Executable()
	if err != nil {
		return err
	}

	// 构建启动命令：以 run 模式启动自己
	cmdArgs := []string{"run"}
	if mcpPort != 0 {
		cmdArgs = append(cmdArgs, "-p", strconv.Itoa(mcpPort))
	}

	command := exec.Command(exePath, cmdArgs...)

	// 将 stdin/stdout/stderr 设为 null，使进程完全脱离终端
	command.Stdin = nil
	command.Stdout = nil
	command.Stderr = nil

	// 设置进程属性，使子进程独立于父进程（跨平台）
	setProcAttributes(command)

	// 启动进程
	if err := command.Start(); err != nil {
		return fmt.Errorf("启动 MCP 服务失败: %w", err)
	}

	pid := command.Process.Pid
	log.Printf("MCP 服务 PID: %d\n", pid)

	// 等待一小段时间，检查进程是否仍在运行（确保没有立即崩溃）
	time.Sleep(500 * time.Millisecond)

	// 检查进程是否还存活
	proc := utilos.GetProcess(pid)
	if proc == nil {
		return fmt.Errorf("启动 MCP 服务失败: 无法找到进程")
	}

	if !utilos.IsProcessAlive(proc) {
		return fmt.Errorf("启动 MCP 服务失败: 进程已退出")
	}

	// 释放进程资源，让子进程完全独立运行
	// 注意：PID 文件由 run 命令写入，避免重复
	_ = command.Process.Release()

	// 等待并读取端口文件
	portFile := filepath.Join(InstallDir, "config", "mcp-server.port")
	var port string
	for i := 0; i < 10; i++ {
		if portData, err := os.ReadFile(portFile); err == nil {
			port = string(portData)
			break
		}
		time.Sleep(100 * time.Millisecond)
	}

	if port != "" {
		log.Printf("MCP 服务启动成功，PID: %d，端口: %s", pid, port)
	} else {
		log.Printf("MCP 服务启动成功，PID: %d（端口号获取失败）", pid)
	}
	return nil
}
