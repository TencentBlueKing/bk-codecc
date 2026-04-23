package cmd

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strconv"

	utilos "codecc/preci_server/internal/util/os"
	"github.com/spf13/cobra"
)

var stopCmd = &cobra.Command{
	Use:   "stop",
	Short: "停止 MCP 服务",
	Long: `停止正在运行的 MCP 服务。

该命令会读取 config/mcp-server.pid 文件中保存的进程 ID，
然后向该进程发送终止信号。

示例：
  preci-mcp stop`,
	RunE: runStop,
}

func init() {
	rootCmd.AddCommand(stopCmd)
}

// runStop 停止 MCP 服务
func runStop(cmd *cobra.Command, args []string) error {
	pidFile := filepath.Join(InstallDir, "config", "mcp-server.pid")

	// 读取 PID 文件
	pidData, err := os.ReadFile(pidFile)
	if err != nil {
		if os.IsNotExist(err) {
			return fmt.Errorf("MCP 服务未运行（PID 文件不存在）")
		}
		return fmt.Errorf("读取 PID 文件失败: %w", err)
	}

	pid, err := strconv.Atoi(string(pidData))
	if err != nil {
		return fmt.Errorf("解析 PID 失败: %w", err)
	}

	log.Printf("正在停止 MCP 服务，PID: %d...\n", pid)

	// 获取进程
	proc := utilos.GetProcess(pid)
	if proc == nil {
		// 进程不存在，清理 PID 文件
		os.Remove(pidFile)
		log.Println("MCP 服务进程不存在，已清理 PID 文件")
		return nil
	}

	// 终止进程
	if err := killProcess(proc); err != nil {
		return fmt.Errorf("终止进程失败: %w", err)
	}

	// 删除 PID 文件
	os.Remove(pidFile)

	// 同时清理端口文件
	portFile := filepath.Join(InstallDir, "config", "mcp-server.port")
	os.Remove(portFile)

	log.Println("MCP 服务已停止")
	return nil
}
