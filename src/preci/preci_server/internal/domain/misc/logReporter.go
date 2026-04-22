package misc

import (
	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/client"
	"codecc/preci_server/internal/infra/config/model"
	"codecc/preci_server/internal/infra/logger"
	"fmt"
	"path/filepath"
)

func LogReporter() error {
	log := logger.GetLogger()
	userInfo, err := cache.GetUserInfo(true)
	if err != nil {
		return err
	}

	log.Info(fmt.Sprintf("user: %s, 蓝盾项目: %s", userInfo.UserId, userInfo.ProjectId))
	cli := client.NewCodeCCClient()
	token, err := cli.BkRepoTempToken()
	if err != nil || token == "" {
		log.Error(fmt.Sprintf("获取临时token失败: %v", err))
		return err
	}

	logPath := filepath.Join(model.GlobalConf.InstallDir, "log", "preci-server.log")
	cli2 := client.NewBkRepoClient()
	for i := 0; i < 3; i++ {
		err = cli2.UploadLog(userInfo.UserId, token, logPath)
		if err != nil {
			log.Error(fmt.Sprintf("上传日志失败: %v", err))
			continue
		}

		return nil
	}

	return err
}
