package cmd

import (
	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/cli/perror"
	"codecc/preci_server/cmd/client"
	"fmt"
	"github.com/spf13/cobra"
	"strings"
)

var checkerCmd = &cobra.Command{
	Use:   "checkerset",
	Short: "管理代码检查规则集",
	Long: `管理代码检查规则集：查看、选择和取消选择规则集。

Usage:
  preci checkerset list                              # 查看可用规则集列表
  preci checkerset select <规则集id>                 # 选择规则集
  preci checkerset select <id1>,<id2>                # 选择多个规则集，用逗号分隔
  preci checkerset unselect <规则集id>               # 取消选择规则集
  preci checkerset unselect <id1>,<id2>              # 取消选择多个规则集，用逗号分隔`,
	RunE: runCheckerSet,
}

func init() {
	rootCmd.AddCommand(checkerCmd)
}

func runCheckerSet(cmd *cobra.Command, args []string) error {
	// 创建客户端
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}

	if len(args) == 1 && args[0] == "list" {
		return runCheckerSetList(cli)
	} else if len(args) == 2 && args[0] == "select" {
		checkerSets := strings.Split(args[1], ",")
		return runCheckerSetSelect(cli, checkerSets)
	} else if len(args) == 2 && args[0] == "unselect" {
		checkerSets := strings.Split(args[1], ",")
		return runCheckerSetUnselect(cli, checkerSets)
	}

	return fmt.Errorf("无效的参数: %v", args)
}

func runCheckerSetSelect(cli *client.PreCIServerClient, checkerSets []string) error {
	log.Start("开始选择规则集: %v", checkerSets)
	projectRoot := GetProjectRoot()
	if projectRoot == "" {
		log.Fail("获取项目根目录失败, 可能还没初始化")
		return perror.ErrNotInitialized
	}

	log.Info("项目根目录: %s", projectRoot)
	resp, err := cli.SelectCheckerSets(projectRoot, checkerSets)
	if err != nil {
		log.Fail("选择规则集失败: %v", err)
		return fmt.Errorf("选择规则集失败: %s", err.Error())
	}
	log.Success("选择规则集成功: %v", resp.CheckerSets)

	initCore(cli, projectRoot, projectRoot)

	return nil
}

func runCheckerSetUnselect(cli *client.PreCIServerClient, checkerSets []string) error {
	log.Start("开始取消选择规则集: %v", checkerSets)
	projectRoot := GetProjectRoot()
	if projectRoot == "" {
		log.Fail("获取项目根目录失败, 可能还没初始化")
		return perror.ErrNotInitialized
	}

	log.Info("项目根目录: %s", projectRoot)
	resp, err := cli.UnselectCheckerSets(projectRoot, checkerSets)
	if err != nil {
		log.Fail("取消选择规则集失败: %v", err)
		return fmt.Errorf("取消选择规则集失败: %s", err.Error())
	}
	log.Success("取消选择规则集成功: %v", resp.CheckerSets)

	initCore(cli, projectRoot, projectRoot)

	return nil
}

func runCheckerSetList(cli *client.PreCIServerClient) error {
	log.Start("开始获取规则集列表")

	resp, err := cli.GetCheckerSetList()
	if err != nil {
		return fmt.Errorf("获取规则集列表失败: %s", err.Error())
	}
	log.Success("获取规则集列表成功:")

	fmt.Printf("	%-30s%-30s%s\n", "CHECKER SET ID (TOOL NAME)", "CHECKER SET NAME", "")
	for _, cs := range resp.CheckerSets {
		fmt.Printf("	%-30s%s\n", cs.CheckerSetId+" ("+cs.ToolName+")", cs.CheckerSetName)
	}

	return nil
}
