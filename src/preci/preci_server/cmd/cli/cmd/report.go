package cmd

import (
	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"fmt"
	"github.com/spf13/cobra"
)

var reportCmd = &cobra.Command{
	Use:   "report",
	Short: "上传日志",
	Long: `上传本地服务日志到云端，用于问题排查和诊断。

Usage:
  preci report    # 上传日志`,
	RunE: runReport,
}

func init() {
	rootCmd.AddCommand(reportCmd)
}

func runReport(cmd *cobra.Command, args []string) error {
	// 创建客户端
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}

	err = cli.ReportLog()
	if err != nil {
		log.Fail("上传日志失败: %w", err)
	} else {
		log.Success("上传日志成功")
	}

	return nil
}
