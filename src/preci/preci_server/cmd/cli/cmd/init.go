package cmd

import (
	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"codecc/preci_server/internal/util/perror"
	"errors"
	"fmt"
	"github.com/spf13/cobra"
)

var (
	projRootPath = ""
)

const initUsage = `初始化项目扫描配置。

该命令会向 PreCI Local Server 发送初始化请求：准备代码扫描所需的配置和环境，
包括获取项目信息、下载所需的扫描工具，以及检测并安装可用的版本更新。

Usage:
  preci init                                     # 由 PreCI 根据当前目录推断项目根目录
  preci init --root_path /path/to/project/root   # 直接指定项目根目录

Params:
  --root_path, -r    项目根目录 [选填]`

var initCmd = &cobra.Command{
	Use:   "init",
	Short: "初始化项目扫描配置",
	Long:  initUsage,
	RunE:  runInit,
}

func init() {
	rootCmd.AddCommand(initCmd)

	initCmd.Flags().StringVarP(&projRootPath, "root_path", "r", "", "项目根路径")
}

func runInit(cmd *cobra.Command, args []string) error {
	currentPath, err := GetCurrentPath()
	if err != nil || currentPath == "" {
		return fmt.Errorf("无法确定当前路径, error: %w", err)
	}

	// 创建客户端
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}

	initCore(cli, currentPath, projRootPath)

	return nil
}

func initCore(cli *client.PreCIServerClient, currentPath, projectRoot string) {
	log.Start("开始初始化. currentPath=%s, projRootPath=%s", currentPath, projectRoot)

	// 第一步：调用 /task/init 获取项目信息和工具列表
	initResp, err := cli.Init(currentPath, projectRoot)
	if err != nil {
		log.Fail("初始化失败: %v", err)
		return
	}
	log.Success("初始化成功! 项目根路径: %s, 工具列表: %v", initResp.RootPath, initResp.Tools)

	// 第二步：如果有新版本则自动下载并安装
	if initResp.HasUpdate {
		log.Info("检测到新版本可用! 当前版本: %s, 最新版本: %s", initResp.CurrentVersion, initResp.LatestVersion)
		log.Start("正在自动下载最新版本...")
		err := cli.DownloadLatestPreCI()
		if errors.Is(err, perror.ErrDupInstall) {
			log.Info("preci 服务正在后台进行更新，请稍后重新执行 preci init 初始化")
			return
		} else if err != nil {
			log.Fail("自动更新失败: %v，请稍后手动执行 preci update 进行更新", err)
		} else {
			log.Success("新版本下载成功!")
			log.Start("正在安装更新...")
			if err := installUpdate(); err != nil {
				log.Fail("自动安装失败: %v，请稍后手动执行 preci update 进行更新", err)
			} else {
				log.Success("更新器已启动! preci 将在后台完成更新到版本 %s", initResp.LatestVersion)
				return
			}
		}
	}

	// 第三步：逐个调用 /task/reload/tool/{toolName} 下载工具
	for _, toolName := range initResp.Tools {
		log.Start("正在下载工具: %s", toolName)
		if err := cli.ReloadTool(toolName); err != nil {
			log.Fail("下载工具 %s 失败: %v", toolName, err)
		} else {
			log.Success("下载工具 %s 成功", toolName)
		}
	}
}
