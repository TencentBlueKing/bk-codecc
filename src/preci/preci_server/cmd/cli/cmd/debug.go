package cmd

import (
	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"fmt"
	"github.com/spf13/cobra"
)

var debugCmd = &cobra.Command{
	Use:   "debug",
	Short: "调试模式开关",
	Long: `开启或关闭 PreCI Server 的调试模式。

Usage:
  preci debug on    # 开启调试模式
  preci debug off   # 关闭调试模式
  preci debug       # 关闭调试模式（默认）`,
	RunE: runDebug,
}

func init() {
	rootCmd.AddCommand(debugCmd)
}

// runDebug 处理 debug 子命令
func runDebug(cmd *cobra.Command, args []string) error {
	cmd.Println("Debug mode enabled")

	// 创建客户端
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}

	mode := "off"
	if len(args) == 1 && args[0] == "on" {
		mode = "on"
	}

	err = cli.DebugMode(mode)
	if err != nil {
		errMsg := fmt.Sprintf("设置调试模式失败: %s", err.Error())
		log.Fail(errMsg)
		return err
	}

	return nil
}
