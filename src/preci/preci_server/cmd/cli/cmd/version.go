package cmd

import (
	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"fmt"
	"github.com/spf13/cobra"
)

var Version string

var versionCmd = &cobra.Command{
	Use:   "version",
	Short: "查看版本信息",
	Long: `查看版本信息。

Usage:
  preci version                              # 查看当前版本
  preci version latest                       # 查看线上最新版本`,
	RunE: runVersion,
}

func init() {
	rootCmd.AddCommand(versionCmd)
}

// runVersion 处理 version 子命令
func runVersion(cmd *cobra.Command, args []string) error {
	if len(args) == 1 && args[0] == "latest" {
		// 创建客户端
		cli, err := client.NewPreCIServerClient(Port)
		if err != nil {
			return fmt.Errorf("创建客户端失败: %w", err)
		}

		resp, err := cli.GetLatestVersion()
		if err != nil {
			log.Fail("获取最新版本失败: %s", err.Error())
			return err
		}

		fmt.Println(resp.LatestVersion)
		return nil
	}

	fmt.Println(Version)
	return nil
}
