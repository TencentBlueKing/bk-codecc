package cmd

import "github.com/spf13/cobra"

var portCmd = &cobra.Command{
	Use:   "port",
	Short: "查看端口信息",
	Long: `查看端口信息。

Usage:
  preci port	# 查看当前端口`,
	RunE: runPort,
}

func init() {
	rootCmd.AddCommand(portCmd)
}

// runPort 处理 port 子命令
func runPort(cmd *cobra.Command, args []string) error {
	cmd.Println(Port)
	return nil
}
