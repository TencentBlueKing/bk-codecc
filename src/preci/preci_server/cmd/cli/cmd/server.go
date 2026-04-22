package cmd

import (
	clilog "codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	utilos "codecc/preci_server/internal/util/os"
	"codecc/preci_server/internal/util/perror"
	"github.com/spf13/cobra"
)

const usage = `管理 PreCI 本地服务。

Usage:
  preci server start      # 启动服务
  preci server stop       # 停止服务
  preci server restart    # 重启服务`

var serverCmd = &cobra.Command{
	Use:   "server",
	Short: "管理本地服务（启动/停止/重启）",
	Long:  usage,
	RunE:  runServer,
}

func init() {
	rootCmd.AddCommand(serverCmd)
}

// runServer 处理 server 子命令
func runServer(cmd *cobra.Command, args []string) error {
	if len(args) == 0 {
		fmt.Println(usage)
		return perror.ErrInvalidParam
	}

	action := args[0]
	switch action {
	case "start":
		return startServer()
	case "stop":
		return stopServer()
	case "restart":
		return startServer()
	default:
		fmt.Println(usage)
		return perror.ErrInvalidParam
	}
}

// startServer 启动服务
func startServer() error {
	clilog.Start("starting preci server...")

	preciServerPath := filepath.Join(InstallDir, "preci-server")
	portFilePath := filepath.Join(InstallDir, "config", "server.port")

	// 启动前先删除旧的端口文件，避免读到过时的端口号
	_ = os.Remove(portFilePath)

	// 执行 preci-server
	cmd := exec.Command(preciServerPath)

	// 将 stdin/stdout/stderr 设为 null，使 server 进程完全脱离终端
	cmd.Stdin = nil
	cmd.Stdout = nil
	cmd.Stderr = nil

	// 设置进程属性，使子进程独立于父进程
	setProcAttributes(cmd)

	// 启动进程
	if err := cmd.Start(); err != nil {
		clilog.Fail("failed to start preci server: %v", err)
		return fmt.Errorf("启动服务失败: %w", err)
	}

	pid := cmd.Process.Pid
	log.Printf("preci server PID: %d\n", pid)

	// 等待一小段时间，检查进程是否仍在运行（确保没有立即崩溃）
	time.Sleep(500 * time.Millisecond)

	// 检查进程是否还存活
	proc := utilos.GetProcess(pid)
	if proc == nil {
		return fmt.Errorf("启动服务失败: 无法找到进程")
	}

	if !utilos.IsProcessAlive(proc) {
		return fmt.Errorf("启动服务失败: 进程已退出")
	}

	// 释放进程资源，让子进程完全独立运行
	_ = cmd.Process.Release()

	// 轮询等待新的端口文件写入（最多等待 10 秒）
	for i := 0; i < 20; i++ {
		portData, err := os.ReadFile(portFilePath)
		if err == nil {
			if tempPort, parseErr := strconv.Atoi(strings.TrimSpace(string(portData))); parseErr == nil {
				Port = tempPort
				break
			}
		}
		time.Sleep(500 * time.Millisecond)
	}

	clilog.Success("preci server started successfully, PID: %d, Port: %d", pid, Port)

	// 检查 Token 是否有效，无效则提示重新登录
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		log.Printf("创建客户端失败，跳过 token 检查: %v\n", err)
		return nil
	}

	healthResp, err := cli.Health()
	if err != nil {
		clilog.Fail("获取健康状态失败，跳过 token 检查: %v", err)
		return nil
	}

	if !healthResp.TokenValid {
		fmt.Println("\ntoken 已失效，请执行以下命令重新登录：")
		fmt.Println("  preci login")
		return nil
	}

	userId := healthResp.UserId
	projectId := healthResp.ProjectId
	clilog.Success("token 有效。 userId: %s, projectId: %s", userId, projectId)

	if projectId == "" {
		_ = selectProject(cli)
	}

	return nil
}

// stopServer 停止服务
func stopServer() error {
	clilog.Start("stopping preci server...")
	c, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return err
	}

	err = c.ShutdownServer()
	if err != nil {
		clilog.Fail("shutdown server failed: %v", err)
	} else {
		clilog.Success("shutdown server successfully")
	}

	return nil
}
