package cmd

import (
	"github.com/spf13/cobra"
)

var (
	Port       = 0  // preci-server 端口号
	InstallDir = "" // CLI 安装目录

	all = false
)

// rootCmd 是 CLI 的根命令
var rootCmd = &cobra.Command{
	Use:   "preci",
	Short: "PreCI CLI - 代码检查本地服务客户端",
	Long: `PreCI CLI v2 是一个本地化代码质量检查命令行工具。

通过与本地 PreCI Server 协作，提供高效的静态代码扫描服务，帮助开发者在编码阶段
快速识别代码潜在缺陷和安全问题。

主要功能：
  • 服务管理 - 启动、停止、重启本地 PreCI Server
  • 代码扫描 - 支持全量扫描、预提交(pre-commit)、预推送(pre-push)、指定路径等多种模式
  • 规则集管理 - 查看、选择和取消选择代码检查规则集
  • 项目管理 - 绑定和切换蓝盾项目
  • 版本管理 - 查看版本信息和自动更新

快速开始：
  1. preci server start  启动本地服务
  2. preci login          完成用户认证
  3. preci init           初始化项目配置
  4. preci scan           执行代码扫描

使用 "preci [command] --help" 获取更多命令帮助信息。`,
	// 如果没有子命令，显示帮助信息
	Run: func(cmd *cobra.Command, args []string) {
		cmd.Help()
	},
}

// Execute 执行根命令
func Execute() error {
	return rootCmd.Execute()
}

func init() {
	// 设置帮助命令的模板
	rootCmd.SetHelpTemplate(helpTemplate())

	scanCmd.Flags().BoolVarP(&all, "all", "a", false, "all")
}

func helpTemplate() string {
	return `{{with (or .Long .Short)}}{{. | trimTrailingWhitespaces}}

{{end}}{{if or .Runnable .HasSubCommands}}{{.UsageString}}{{end}}`
}
