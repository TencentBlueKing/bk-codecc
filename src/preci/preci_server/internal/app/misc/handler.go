package misc

import (
	"codecc/preci_server/internal/domain/misc"
	"codecc/preci_server/internal/domain/version"
	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/web"
	"github.com/go-chi/chi/v5"
	"net/http"
	"time"
)

func DownloadLatestPreCIHandler(_ *http.Request) web.Encoder {
	err := version.DownloadLatestPreCI()
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{}
}

func LatestVersionHandler(_ *http.Request) web.Encoder {
	latestVersion, err := version.GetLatestVersion()
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{
		Body: &LatestVersionResp{
			LatestVersion: latestVersion,
		},
	}
}

func ReportLogHandler(_ *http.Request) web.Encoder {
	err := misc.LogReporter()
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{}
}

func DebugHandler(r *http.Request) web.Encoder {
	// 获取路径参数
	debugMode := chi.URLParam(r, "mode")
	if debugMode == "on" {
		logger.GetLogger().SetDebugLevel()
	} else {
		logger.GetLogger().SetInfoLevel()
	}

	return web.SimpleEncoder{}
}

func HealthHandler(_ *http.Request) web.Encoder {
	resp := &HealthResp{Healthy: true}

	userInfo, err := cache.GetUserInfo(false)
	if err != nil {
		resp.TokenValid = false
		resp.TokenExpiresIn = 0
	} else {
		expiredTime, exists := cache.GetTokenExpiredTime()
		if exists {
			remaining := expiredTime - time.Now().Unix()
			resp.TokenExpiresIn = remaining
			resp.TokenValid = remaining >= 60
		}
		resp.UserId = userInfo.UserId
		resp.ProjectId = userInfo.ProjectId
	}

	return web.SimpleEncoder{Body: resp}
}
