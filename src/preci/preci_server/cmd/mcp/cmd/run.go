package cmd

import (
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strconv"
	"syscall"

	"codecc/preci_server/cmd/mcp/config"
	"codecc/preci_server/cmd/mcp/tool"
	"github.com/modelcontextprotocol/go-sdk/mcp"
	"github.com/spf13/cobra"
)

var runPort int

var runCmd = &cobra.Command{
	Use:   "run",
	Short: "前台运行 MCP 服务（调试用）",
	Long: `以前台模式运行 MCP 服务器。

此命令通常用于调试目的，服务会在前台运行并输出日志。
按 Ctrl+C 可停止服务。

示例：
  preci-mcp run          # 使用自动分配端口运行
  preci-mcp run -p 8080  # 使用指定端口运行`,
	RunE: runServer,
}

func init() {
	rootCmd.AddCommand(runCmd)
	runCmd.Flags().IntVarP(&runPort, "port", "p", 0, "MCP 服务端口号（0 表示自动分配）")
}

// runServer 运行 MCP 服务器
func runServer(cmd *cobra.Command, args []string) error {
	// 同步配置
	config.Port = Port

	// 读取 PreCI 服务端口号
	portFilePath := filepath.Join(InstallDir, "config", "server.port")
	portData, err := os.ReadFile(portFilePath)
	if err != nil {
		log.Printf("读取 PreCI 服务端口文件失败: %v\n", err)
		return fmt.Errorf("读取 PreCI 服务端口文件失败: %w", err)
	}

	tempPort, err := strconv.Atoi(string(portData))
	if err != nil {
		log.Printf("解析 PreCI 服务端口失败: %v\n", err)
		return fmt.Errorf("解析 PreCI 服务端口失败: %w", err)
	}
	config.Port = tempPort

	// 创建 MCP 服务器
	server := mcp.NewServer(&mcp.Implementation{
		Name:    "preci",
		Title:   "PreCI 本地代码扫描服务 - 提供代码质量检查、扫描任务管理和结果分析功能",
		Version: config.Version,
	}, nil)
	mcp.AddTool(server, &mcp.Tool{Name: "scan", Description: "开始扫描"}, tool.Scan)
	mcp.AddTool(server, &mcp.Tool{Name: "progress", Description: "获取当前正在进行的代码扫描进度"}, tool.Progress)
	mcp.AddTool(server, &mcp.Tool{Name: "result", Description: "获取代码扫描结果"}, tool.Result)
	mcp.AddTool(server, &mcp.Tool{Name: "progressAndResult", Description: "先获取当前扫描任务进度，如果发现当前任务已完成，" +
		"会将结果一并返回；否则只返回进度"}, tool.ProgressAndResult)
	mcp.AddTool(server, &mcp.Tool{Name: "scanAndResult", Description: "开始扫描并等待扫描结束，获取结果"}, tool.ScanAndResult)

	// 创建 StreamableHTTP 处理器
	handler := mcp.NewStreamableHTTPHandler(func(r *http.Request) *mcp.Server {
		return server
	}, nil)

	// 创建监听器，支持自动端口分配
	var listener net.Listener
	if runPort == 0 {
		// 端口为 0 时，系统自动分配可用端口
		listener, err = net.Listen("tcp", ":0")
		if err != nil {
			log.Printf("创建监听器失败: %v\n", err)
			return fmt.Errorf("创建监听器失败: %w", err)
		}
	} else {
		// 使用用户指定的端口
		listener, err = net.Listen("tcp", fmt.Sprintf(":%d", runPort))
		if err != nil {
			log.Printf("监听端口 %d 失败: %v\n", runPort, err)
			return fmt.Errorf("监听端口 %d 失败: %w", runPort, err)
		}
	}

	// 获取实际监听的端口号
	actualPort := listener.Addr().(*net.TCPAddr).Port

	// 写入端口文件（供其他工具读取）
	configDir := filepath.Join(InstallDir, "config")
	if err := os.MkdirAll(configDir, 0755); err != nil {
		log.Printf("警告: 创建配置目录失败: %v", err)
	}

	portFile := filepath.Join(configDir, "mcp-server.port")
	if err := os.WriteFile(portFile, []byte(strconv.Itoa(actualPort)), 0644); err != nil {
		log.Printf("警告: 写入端口文件失败: %v", err)
	}

	// 写入 PID 文件
	pidFile := filepath.Join(configDir, "mcp-server.pid")
	if err := os.WriteFile(pidFile, []byte(strconv.Itoa(os.Getpid())), 0644); err != nil {
		log.Printf("警告: 写入 PID 文件失败: %v", err)
	}

	// 设置信号处理，优雅退出时清理文件
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		<-sigChan
		log.Println("收到退出信号，正在清理...")
		os.Remove(pidFile)
		os.Remove(portFile)
		os.Exit(0)
	}()

	// 启动 HTTP 服务器
	log.Printf("MCP 服务已启动，监听端口: %d", actualPort)
	if err := http.Serve(listener, handler); err != nil {
		return fmt.Errorf("HTTP 服务器错误: %w", err)
	}

	return nil
}
