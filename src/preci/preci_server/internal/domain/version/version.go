// Package version implements the version domain, which resolves the
// installed/latest PreCI versions and coordinates self-upgrade.
package version

import (
	"codecc/preci_server/internal/infra/client"
	"codecc/preci_server/internal/infra/config/model"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/util/perror"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"sync"
	"time"
)

var Version string

var (
	downloadMu    sync.Mutex
	isDownloading bool
)

func GetVersion() string {
	return Version
}

func GetLatestVersion() (string, error) {
	log := logger.GetLogger()
	latestVersion, err := client.GetLatestVersion()
	if err != nil {
		log.Error(fmt.Sprintf("获取最新版本失败: %v", err))
		return "", perror.ErrNoLatestVersion
	}

	return latestVersion, nil
}

func DownloadLatestPreCI() error {
	log := logger.GetLogger()

	downloadMu.Lock()
	if isDownloading {
		downloadMu.Unlock()
		log.Info("已有下载任务在进行中，跳过本次请求")
		return perror.ErrDupInstall
	}
	isDownloading = true
	downloadMu.Unlock()

	defer func() {
		downloadMu.Lock()
		isDownloading = false
		downloadMu.Unlock()
	}()

	latestVersion, err := client.GetLatestVersion()
	if err != nil {
		log.Error(fmt.Sprintf("获取最新版本失败: %v", err))
		return perror.ErrNoLatestVersion
	}

	log.Info(fmt.Sprintf("最新版本: %s", latestVersion))
	if Version == latestVersion {
		return perror.ErrIsLatestVersion
	}

	// 默认是 windows
	osType := "win"
	if runtime.GOOS == "linux" {
		osType = "linux"
	} else if runtime.GOOS == "darwin" {
		arch := runtime.GOARCH

		if arch != "amd64" && arch != "arm64" {
			log.Error(fmt.Sprintf("不太支持的系统架构: mac-%s", arch))
			arch = "amd64"
		}

		osType = "mac_" + arch
	}

	fileName := fmt.Sprintf("preci_%s_%s.zip", osType, latestVersion)
	filePath := filepath.Join(model.GlobalConf.InstallDir, "preci_tmp.zip")

	if info, err := os.Stat(filePath); err == nil && info.Size() > 0 {
		// 残留 zip 超过 10 分钟则判定为上次更新的遗留文件，清理后继续下载
		staleThreshold := 10 * time.Minute
		if time.Since(info.ModTime()) > staleThreshold {
			log.Info(fmt.Sprintf("检测到残留的 %s（修改于 %s），已超过 %v，清理后继续更新",
				filePath, info.ModTime().Format("15:04:05"), staleThreshold))
			if err := os.Remove(filePath); err != nil {
				log.Error(fmt.Sprintf("清理残留 zip 失败: %v", err))
				return perror.ErrDupInstall
			}
		} else {
			log.Info(fmt.Sprintf("检测到 %s 已存在（修改于 %s），可能有更新正在进行中，跳过本次自动更新",
				filePath, info.ModTime().Format("15:04:05")))
			return perror.ErrDupInstall
		}
	}

	downloadUrl := fmt.Sprintf("%s/%s/%s", client.BkRepoDownloadFolder, latestVersion, fileName)
	err = client.DownloadFile(downloadUrl, filePath)
	if err != nil {
		log.Error(fmt.Sprintf("下载失败: %v", err))
		return perror.ErrDownloadFailed
	}

	return nil
}
