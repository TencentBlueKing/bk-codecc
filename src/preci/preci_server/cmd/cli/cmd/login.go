package cmd

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"codecc/preci_server/cmd/cli/log"
	"codecc/preci_server/cmd/client"
	"github.com/spf13/cobra"
	"golang.org/x/oauth2"
)

var (
	projectId string
)

const loginUsage = `登录 PreCI：

通过浏览器打开链接并输入设备码完成 OAuth 授权。

登录后，认证信息会保存在本地，后续 PreCI Local Server 调用云端服务将自动使用该认证。

Usage:
  preci login                                    # OAuth 设备码登录
  preci login -p <蓝盾项目id>                     # 设备码登录并绑定项目

Params:
  --project, -p    蓝盾项目 ID [选填]`

var loginCmd = &cobra.Command{
	Use:   "login",
	Short: "登录",
	Long:  loginUsage,
	RunE:  runLogin,
}

const (
	defaultBKAuthBaseURL  = "http://stage.bkauth.woa.com/realms/bk-devops"
	defaultBKAuthClientID = "bk-preci"
	defaultBKAuthResource = "service:codecc"
)

func init() {
	rootCmd.AddCommand(loginCmd)

	loginCmd.Flags().StringVarP(&projectId, "project", "p", "", "蓝盾项目id")
}

func runLogin(cmd *cobra.Command, args []string) error {
	return runDeviceLogin()
}

type bkAuthConfig struct {
	BaseURL  string `json:"BaseURL"`
	ClientID string `json:"ClientID"`
	Resource string `json:"Resource"`
}

func loadBKAuthConfig() *bkAuthConfig {
	cfg := &bkAuthConfig{
		BaseURL:  defaultBKAuthBaseURL,
		ClientID: defaultBKAuthClientID,
		Resource: defaultBKAuthResource,
	}

	if InstallDir == "" {
		return cfg
	}

	configPath := filepath.Join(InstallDir, "config", "authConfig.json")
	data, err := os.ReadFile(configPath)
	if err != nil {
		return cfg
	}

	var fileCfg struct {
		BKAuth bkAuthConfig `json:"BKAuth"`
	}
	if err := json.Unmarshal(data, &fileCfg); err != nil {
		return cfg
	}

	if fileCfg.BKAuth.BaseURL != "" {
		cfg.BaseURL = fileCfg.BKAuth.BaseURL
	}
	if fileCfg.BKAuth.ClientID != "" {
		cfg.ClientID = fileCfg.BKAuth.ClientID
	}
	if fileCfg.BKAuth.Resource != "" {
		cfg.Resource = fileCfg.BKAuth.Resource
	}

	return cfg
}

func runDeviceLogin() error {
	cli, err := client.NewPreCIServerClient(Port)
	if err != nil {
		return fmt.Errorf("创建客户端失败: %w", err)
	}

	bkauthCfg := loadBKAuthConfig()

	cfg := &oauth2.Config{
		ClientID: bkauthCfg.ClientID,
		Endpoint: oauth2.Endpoint{
			DeviceAuthURL: bkauthCfg.BaseURL + "/oauth2/device/authorize",
			TokenURL:      bkauthCfg.BaseURL + "/oauth2/token",
		},
	}

	log.Start("开始设备码登录...")

	ctx := context.Background()

	da, err := cfg.DeviceAuth(ctx,
		oauth2.SetAuthURLParam("resource", bkauthCfg.Resource),
		oauth2.SetAuthURLParam("client_id", bkauthCfg.ClientID),
	)
	if err != nil {
		log.Fail("请求设备授权失败: %v", err)
		return err
	}

	fmt.Println()
	fmt.Println("To authorize this device, using a browser, visit:")
	fmt.Printf("  %s\n\n", da.VerificationURI)
	fmt.Printf("And enter the code: %s\n\n", da.UserCode)
	if da.VerificationURIComplete != "" {
		fmt.Printf("Or visit: %s\n\n", da.VerificationURIComplete)
	}
	fmt.Println("Waiting for authorization...")

	oauthToken, err := cfg.DeviceAccessToken(ctx, da)
	if err != nil {
		log.Fail("获取令牌失败: %v", err)
		return err
	}

	expiresIn := int64(time.Until(oauthToken.Expiry).Seconds())
	if expiresIn <= 0 {
		expiresIn = 7200
	}

	respUserId, respProjectId, err := cli.OAuthDeviceLogin(
		oauthToken.AccessToken, oauthToken.RefreshToken, projectId, expiresIn,
	)
	if err != nil {
		log.Fail("保存登录信息失败: %v", err)
		return err
	}

	log.Success("登录成功! 用户名: %s, 蓝盾项目: %s", respUserId, respProjectId)
	return nil
}
