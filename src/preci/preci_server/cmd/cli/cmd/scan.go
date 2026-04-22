package cmd

import (
	"codecc/preci_server/cmd/cli/log"
	clierr "codecc/preci_server/cmd/cli/perror"
	"codecc/preci_server/cmd/client"
	"codecc/preci_server/internal/app/task/model"
	"codecc/preci_server/internal/util/constant"
	"codecc/preci_server/internal/util/perror"
	"errors"
	"fmt"
	"github.com/schollz/progressbar/v3"
	"github.com/spf13/cobra"
	"path/filepath"
	"strings"
	"time"
)

var (
	path      string
	silence   bool
	preCommit bool
	prePush   bool
)

var scanCmd = &cobra.Command{
	Use:   "scan",
	Short: "执行代码扫描",
	Long: `执行代码扫描。

Usage:
  preci scan                              # 全量扫描当前所在项目
  preci scan --path /path/to/dir          # 扫描指定路径，多路径用逗号分隔
  preci scan --pre-commit                 # pre-commit 模式扫描
  preci scan --pre-push                   # pre-push 模式扫描
  preci scan -s                           # 静默模式，不跟踪扫描进度
  preci scan status                       # 查看当前扫描进度
  preci scan result                       # 查看扫描结果
  preci scan result -a                    # 查看全部扫描结果（默认只显示前 10 条）
  preci scan cancel                       # 取消当前扫描任务

Params:
  --path,       -p    扫描路径，多路径用逗号分隔 [选填]
  --silence,    -s    静默模式，不跟踪扫描进度 [选填]
  --pre-commit        pre-commit 模式 [选填]
  --pre-push          pre-push 模式 [选填]
  --all,        -a    显示全部扫描结果 [选填]`,
	RunE: runScan,
}

func init() {
	rootCmd.AddCommand(scanCmd)

	scanCmd.Flags().StringVarP(&path, "path", "p", "", "扫描路径, 多路径用逗号分隔")
	scanCmd.Flags().BoolVarP(&silence, "silence", "s", false, "静默模式，不跟踪扫描进度")
	scanCmd.Flags().BoolVar(&preCommit, "pre-commit", false, "pre-commit 模式")
	scanCmd.Flags().BoolVar(&prePush, "pre-push", false, "pre-push 模式")
}

func displayScanProgress(progress *model.ScanProgressResponse, singleLine bool) string {
	if singleLine {
		toolStatuses := "[工具:"

		for tool, status := range progress.ToolStatuses {
			toolStatuses += fmt.Sprintf("  %s:%s", tool, status)
		}

		toolStatuses += "]"
		if progress.Status == "running" {
			return "扫描中  " + toolStatuses
		} else {
			return "已完成  " + toolStatuses
		}
	}

	var toolStatusLines string
	for tool, status := range progress.ToolStatuses {
		toolStatusLines += fmt.Sprintf("\n  - %-20s %s", tool+":", status)
	}
	return fmt.Sprintf("  项目根目录: %s\n  任务状态: %s\n  工具状态:%s\n",
		progress.ProjectRoot, progress.Status, toolStatusLines)
}

func runScanStatus(cli *client.PreCIServerClient) error {
	progress, err := cli.GetScanProgress()
	if err != nil {
		if errors.Is(err, perror.ErrNoScanTask) {
			log.Success("当前没有扫描任务")
			return nil
		}

		return fmt.Errorf("获取当前扫描进度失败: %s", err.Error())
	}

	// 打印扫描进度信息
	var toolStatusLines string
	for tool, status := range progress.ToolStatuses {
		toolStatusLines += fmt.Sprintf("\n  - %-20s %s", tool+":", status)
	}
	log.Success("获取当前扫描进度成功!")
	fmt.Print(displayScanProgress(progress, false))
	return nil
}

func displayScanResult(result *model.ScanResultResponse) error {
	if result == nil || len(result.Defects) == 0 {
		log.Success("未发现任何代码问题!")
		return nil
	}

	// 统计信息
	totalDefects := len(result.Defects)

	// 打印统计信息
	log.Info("共有 %d 个代码问题: ", totalDefects)

	for ind, defect := range result.Defects {
		if !all && ind >= 10 {
			fmt.Println("......")
			break
		}
		fmt.Printf("%s:%d (%s#%s) %s\n",
			defect.FilePath, defect.Line, defect.ToolName, defect.CheckerName, defect.Description)
	}

	return nil
}

func runScanResult(cli *client.PreCIServerClient) error {
	var resultPath string

	log.Start("开始获取扫描结果, path=%s", path)

	currPath, _ := GetCurrentPath()
	if path != "" {
		// 如果路径是相对路径，需要转换为绝对路径
		if !filepath.IsAbs(path) {
			absPath, err := filepath.Abs(path)
			if err != nil {
				log.Fail("获取指定路径失败. path=%s, err: %v", path, err)
				return fmt.Errorf("获取指定路径失败: %v", err)
			} else {
				log.Info("指定路径为: %s", path)
			}

			resultPath = absPath
		} else {
			resultPath = path
		}
	} else if currPath != "" {
		// 如果没有指定路径，使用当前目录
		resultPath = currPath
	} else {
		log.Fail("未指定路径, 请使用 --path 或 -p 指定路径")
		return fmt.Errorf("未指定路径")
	}

	result, err := cli.GetScanResult(resultPath)
	if err != nil {
		return fmt.Errorf("获取扫描结果失败: %s", err.Error())
	}

	log.Success("获取扫描结果成功!")
	return displayScanResult(result)
}

func runScanCore(cli *client.PreCIServerClient) error {
	var scanType int
	var paths []string
	if path != "" {
		scanType = constant.TargetScan
		tmpPaths := strings.Split(path, ",")
		for _, tmpPath := range tmpPaths {
			if !filepath.IsAbs(tmpPath) {
				absPath, err := filepath.Abs(tmpPath)
				if err == nil {
					paths = append(paths, absPath)
				}
			} else {
				paths = append(paths, tmpPath)
			}
		}
	} else if preCommit {
		scanType = constant.PreCommitScan
	} else if prePush {
		scanType = constant.PrePushScan
	} else {
		scanType = constant.FullScan
	}

	projectRoot := GetProjectRoot()
	if projectRoot == "" {
		log.Fail("无法确定项目根目录, 可能还未初始化.")
		return clierr.ErrNotInitialized
	}

	log.Start("开始扫描. scanType=%d, paths=%v, rootDir=%s", scanType, paths, projectRoot)
	// 启动扫描
	successMessage, err := cli.StartScan(scanType, paths, projectRoot)
	if err != nil {
		log.Fail("扫描失败: %v", err)
		return nil
	}

	log.Success(successMessage)

	// 如果是静默模式，直接返回
	if silence {
		return nil
	}

	// 循环获取扫描进度
	if err = trackScanProgress(cli); err != nil {
		return err
	}

	return runScanResult(cli)
}

func runScanCancel(cli *client.PreCIServerClient) error {
	log.Start("开始取消扫描")
	projectRoot, err := cli.CancelScan()
	if err != nil {
		log.Fail("取消扫描失败: %v", err)
	} else {
		log.Success("取消扫描成功! 被取消的项目: %s", projectRoot)
	}

	return nil
}

func runScan(cmd *cobra.Command, args []string) error {
	// 创建客户端
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}

	if args == nil || len(args) == 0 {
		return runScanCore(cli)
	} else if len(args) == 1 {
		if args[0] == "status" {
			return runScanStatus(cli)
		} else if args[0] == "result" {
			return runScanResult(cli)
		} else if args[0] == "cancel" {
			return runScanCancel(cli)
		}
	}

	return fmt.Errorf("无效的参数: %v", args)
}

// trackScanProgress 跟踪扫描进度
func trackScanProgress(cli *client.PreCIServerClient) error {
	ticker := time.NewTicker(1 * time.Second) // 每2秒查询一次进度
	defer ticker.Stop()

	log.Info("代码扫描中...")

	bar := progressbar.NewOptions(-1,
		progressbar.OptionSetDescription("初始化扫描..."),
		progressbar.OptionSpinnerType(14), // 使用 spinner 样式
		progressbar.OptionSetWidth(40),
	)

	errorTime := 0
	for {
		select {
		case <-ticker.C:
			progress, err := cli.GetScanProgress()
			if err != nil {
				if errors.Is(err, perror.ErrNoScanTask) {
					log.Success("扫描完成!")
					return nil
				}

				errorTime++
				if errorTime > 3 {
					log.Fail("获取扫描进度失败: %s", err.Error())
					return fmt.Errorf("获取扫描进度失败: %s", err.Error())
				}

				continue
			}

			// 打印扫描进度信息
			printScanProgress(progress, bar)

			// 检查是否完成
			if progress.Status == "done" {
				_ = bar.Close()
				fmt.Println()
				log.Success("扫描完成!")
				return nil
			}
		}
	}
}

// printScanProgress 打印扫描进度
func printScanProgress(progress *model.ScanProgressResponse, bar *progressbar.ProgressBar) {
	bar.Describe(displayScanProgress(progress, true))
	bar.Add(1)
}
