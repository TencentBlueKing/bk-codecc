// Package auth implements the authentication/authorization domain for
// PreCI, including OAuth login flows and token caching.
package auth

import (
	"codecc/preci_server/internal/domain/auth/model"
	"codecc/preci_server/internal/infra/cache"
	"codecc/preci_server/internal/infra/client"
	"codecc/preci_server/internal/infra/client/dto"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util/perror"
	"fmt"
)

func ListProjects() ([]dto.Project, error) {
	log := logger.GetLogger()
	codeClient := client.NewCodeCCClient()

	projects, err := codeClient.ListProjects()
	if err != nil {
		log.Error(fmt.Sprintf("调用接口获取用户项目失败: %s", err.Error()))
	} else {
		log.Info(fmt.Sprintf("获取用户项目成功: %d", len(projects)))
	}

	return projects, err
}

func SetProjectId(projectId string) error {
	log := logger.GetLogger()
	log.Info(fmt.Sprintf("set project id: %s", projectId))
	if err := cache.SetProjectId(projectId); err != nil {
		log.Error(fmt.Sprintf("保存 project id 到 db 失败: %v", err))
		return perror.ErrInvalidProjectId
	}

	return nil
}

func GetProjectId() (string, error) {
	log := logger.GetLogger()
	projectId, err := cache.GetProjectId()
	if err != nil {
		log.Error(fmt.Sprintf("获取 project id 失败: %v", err))
		return "", perror.ErrInvalidProjectId
	}

	return projectId, nil
}

func saveAuthInfo(token *model.OauthToken) error {
	log := logger.GetLogger()
	cache.SetAccessTokenCache(token.UserId, token.AccessToken, token.RefreshToken, token.ExpiredTime)

	if err := token.Save(storage.DB); err != nil {
		log.Warn(fmt.Sprintf("保存 token 失败: %v", err))
	}

	return nil
}

func DeviceLogin(accessToken, refreshToken, projectId string, expiresIn int64) (string, string, error) {
	log := logger.GetLogger()

	log.Info(fmt.Sprintf("device code flow login, project id: %s", projectId))

	if projectId != "" {
		if err := cache.SetProjectId(projectId); err != nil {
			log.Warn(fmt.Sprintf("保存 project id 到 db 失败: %v", err))
		}
	}

	codeccClient := client.NewCodeCCClient()
	userId, err := codeccClient.GetUserInfoByToken(accessToken)
	if err != nil {
		log.Warn(fmt.Sprintf("获取 userId 失败: %v, 将使用空 userId", err))
	}

	token := model.NewOauthToken2(userId, accessToken, refreshToken, expiresIn)
	_ = saveAuthInfo(&token)

	projectId, _ = cache.GetProjectId()

	log.Info(fmt.Sprintf("device 登录成功: userId=%s, projectId=%s, access token expired after=%d seconds",
		userId, projectId, expiresIn))

	return userId, projectId, nil
}

func RefreshAccessToken(refreshToken string) (*model.OauthToken, error) {
	log := logger.GetLogger()

	log.Info("refreshing access token...")

	bkauthClient := client.NewBKAuthClient()
	resp, err := bkauthClient.RefreshToken(refreshToken)
	if err != nil {
		log.Error(fmt.Sprintf("refresh token failed: %v", err))
		return nil, err
	}

	codeccClient := client.NewCodeCCClient()
	userId, err := codeccClient.GetUserInfoByToken(resp.AccessToken)
	if err != nil {
		log.Warn(fmt.Sprintf("获取 userId 失败: %v", err))
	}

	token := model.NewOauthToken2(userId, resp.AccessToken, resp.RefreshToken, resp.ExpiresIn)
	_ = saveAuthInfo(&token)

	log.Info(fmt.Sprintf("token 刷新成功: userId=%s, expires_in=%d", userId, resp.ExpiresIn))

	return &token, nil
}
