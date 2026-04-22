package cmd

import (
	"github.com/spf13/cobra"
)

var (
	Port       = 0  // MCP 服务端口号
	InstallDir = "" // 安装目录
)

// rootCmd 是 MCP CLI 的根命令
var rootCmd = &cobra.Command{
	Use:   "preci-mcp",
	Short: "PreCI MCP Server - PreCI 的 MCP 服务",
	Long: `PreCI MCP Server 是一个通过 Streamable HTTP 通讯的 MCP 服务器，基于 PreCI Server 提供代码扫描业务的 Model Context Protocol 支持。
它可以与 AI 编辑器（如 CodeBuddy）集成，提供代码扫描功能。

主要命令：
  preci-mcp start    启动 MCP 服务（守护进程模式）
  preci-mcp stop     停止 MCP 服务
  preci-mcp run      前台运行 MCP 服务（调试用）

使用 "preci-mcp [command] --help" 获取更多命令帮助信息。`,
	Run: func(cmd *cobra.Command, args []string) {
		cmd.Help()
	},
}

// Execute 执行根命令
func Execute() error {
	return rootCmd.Execute()
}
