package cmd

import (
	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"fmt"
	"github.com/spf13/cobra"
)

const setProjectHint = "请手动执行 'preci project set <projectId>'"

var projectCmd = &cobra.Command{
	Use:   "project",
	Short: "项目管理",
	Long: `管理蓝盾项目绑定。

Usage:
  preci project                    # 展示项目列表，交互式选择绑定
  preci project set <projectId>    # 直接设置当前项目
  preci project get                # 查看当前绑定的项目`,
	Args: cobra.NoArgs,
	RunE: runProjectSelect,
}

var projectSetCmd = &cobra.Command{
	Use:   "set <projectId>",
	Short: "设置当前项目",
	Args:  cobra.ExactArgs(1),
	RunE:  runProjectSet,
}

var projectGetCmd = &cobra.Command{
	Use:   "get",
	Short: "获取当前绑定的项目",
	Args:  cobra.NoArgs,
	RunE:  runProjectGet,
}

func init() {
	rootCmd.AddCommand(projectCmd)
	projectCmd.AddCommand(projectSetCmd)
	projectCmd.AddCommand(projectGetCmd)
}

func runProjectSelect(_ *cobra.Command, _ []string) error {
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}
	return selectProject(cli)
}

func runProjectSet(_ *cobra.Command, args []string) error {
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}
	projectId := args[0]
	if err := cli.SetProject(projectId); err != nil {
		log.Fail("设置项目失败: %v", err)
	} else {
		log.Success("设置项目成功, 项目 ID: %s", projectId)
	}
	return nil
}

func runProjectGet(_ *cobra.Command, _ []string) error {
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}
	projectId, err := cli.GetProject()
	if err != nil {
		log.Fail("获取项目失败: %v", err)
	} else {
		log.Success("当前绑定的项目 ID: %s", projectId)
	}
	return nil
}

func selectProject(cli *client.PreCIServerClient) error {
	projects, err := cli.ListProjects()
	if err != nil || len(projects) == 0 {
		log.Fail(fmt.Sprintf("获取项目列表失败：%v，%s", err, setProjectHint))
	} else {
		fmt.Println("请选择要绑定的项目：")
		for i, p := range projects {
			fmt.Printf("  %d. %s (%s)\n", i+1, p.ProjectName, p.ProjectId)
		}
		fmt.Print("输入编号: ")
		var choice int
		if _, err := fmt.Scan(&choice); err != nil || choice < 1 || choice > len(projects) {
			log.Fail("输入无效，%s", setProjectHint)
		} else {
			selected := projects[choice-1]
			if err := cli.SetProject(selected.ProjectId); err != nil {
				log.Fail("绑定项目失败: %v", err)
			} else {
				log.Success("已绑定项目: %s (%s)", selected.ProjectName, selected.ProjectId)
			}
		}
	}
	return nil
}
