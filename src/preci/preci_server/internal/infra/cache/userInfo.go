package cache

import (
	"fmt"
	"sync"
	"time"

	"codecc/preci_server/internal/infra/cache/repository"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util/perror"
)

const proactiveRefreshThresholdSec = 720 // 10% of typical 7200s token lifetime

var userInfoCache = new(UserInfo)

type UserInfo struct {
	AccessToken  string
	RefreshToken string
	ExpiredTime  int64
	UserId       string
	ProjectId    string
}

type RefreshedToken struct {
	UserId       string
	AccessToken  string
	RefreshToken string
	ExpiredTime  int64
}

var (
	tokenRefresher func(refreshToken string) (*RefreshedToken, error)
	refreshMu      sync.Mutex
)

func RegisterTokenRefresher(fn func(string) (*RefreshedToken, error)) {
	tokenRefresher = fn
}

func GetUserInfo(needProjectId bool) (*UserInfo, error) {
	log := logger.GetLogger()
	sto := storage.DB
	if userInfoCache.AccessToken == "" {
		entity, err := repository.GetAccessToken(sto)
		if err != nil {
			log.Error(fmt.Sprintf("get access token failed: %v", err))
			return nil, perror.ErrInvalidAccessToken
		}

		userInfoCache.AccessToken = entity.AccessToken
		userInfoCache.RefreshToken = entity.RefreshToken
		userInfoCache.ExpiredTime = entity.ExpiredTime
		userInfoCache.UserId = entity.UserId
	}

	if _, err := refreshTokenIfNeed(); err != nil {
		return nil, perror.ErrInvalidAccessToken
	}

	projectId, err := GetProjectId()
	if err != nil || projectId == "" {
		log.Error("get project id failed")
		if needProjectId {
			return nil, perror.ErrInvalidProjectId
		}
	}

	return &UserInfo{
		AccessToken:  userInfoCache.AccessToken,
		RefreshToken: userInfoCache.RefreshToken,
		UserId:       userInfoCache.UserId,
		ProjectId:    projectId,
	}, nil
}

func GetAccessToken() (string, error) {
	log := logger.GetLogger()
	sto := storage.DB
	if userInfoCache.AccessToken == "" {
		entity, err := repository.GetAccessToken(sto)
		if err != nil {
			log.Error(fmt.Sprintf("get access token failed: %v", err))
			return "", perror.ErrInvalidAccessToken
		}

		userInfoCache.AccessToken = entity.AccessToken
		userInfoCache.RefreshToken = entity.RefreshToken
		userInfoCache.ExpiredTime = entity.ExpiredTime
		userInfoCache.UserId = entity.UserId
	}

	return refreshTokenIfNeed()
}

func refreshTokenIfNeed() (string, error) {
	log := logger.GetLogger()

	now := time.Now().Unix()
	remaining := userInfoCache.ExpiredTime - now

	if remaining < 0 {
		if err := tryRefreshToken(); err != nil {
			log.Info("first refresh attempt failed, retrying in 3s...")
			time.Sleep(3 * time.Second)
			if err = tryRefreshToken(); err != nil {
				log.Error(fmt.Sprintf("refresh token failed after retry: %v", err))
				return "", perror.ErrInvalidAccessToken
			}
		}

		return userInfoCache.AccessToken, nil
	}

	if remaining < proactiveRefreshThresholdSec {
		log.Info("access token nearing expiry, attempting proactive refresh")
		if err := tryRefreshToken(); err != nil {
			log.Info(fmt.Sprintf("proactive refresh failed, using current token: %v", err))
		}
	}

	return userInfoCache.AccessToken, nil
}

func SetProjectId(projectId string) error {
	userInfoCache.ProjectId = projectId
	return repository.SaveProjectId(storage.DB, projectId)
}

func GetProjectId() (string, error) {
	if userInfoCache.ProjectId == "" {
		projectId, err := repository.GetProjectId(storage.DB)
		if err != nil {
			return "", err
		}
		userInfoCache.ProjectId = projectId
	}

	return userInfoCache.ProjectId, nil
}

func SetAccessTokenCache(userId, accessToken, refreshToken string, expiredTime int64) {
	userInfoCache.AccessToken = accessToken
	userInfoCache.RefreshToken = refreshToken
	userInfoCache.ExpiredTime = expiredTime
	userInfoCache.UserId = userId
}

func tryRefreshToken() error {
	refreshMu.Lock()
	defer refreshMu.Unlock()

	now := time.Now().Unix()
	if userInfoCache.ExpiredTime-now > 60 {
		return nil
	}

	if tokenRefresher == nil || userInfoCache.RefreshToken == "" {
		return perror.ErrInvalidAccessToken
	}

	refreshed, err := tokenRefresher(userInfoCache.RefreshToken)
	if err != nil {
		return err
	}

	userInfoCache.AccessToken = refreshed.AccessToken
	userInfoCache.RefreshToken = refreshed.RefreshToken
	userInfoCache.ExpiredTime = refreshed.ExpiredTime
	userInfoCache.UserId = refreshed.UserId

	return nil
}

func InvalidateToken() {
	userInfoCache.ExpiredTime = 0
}

func ForceRefresh() error {
	InvalidateToken()
	_, err := GetAccessToken()
	return err
}

// GetTokenExpiredTime returns the stored token expiry Unix timestamp (seconds) and
// whether a token exists (false means no token found in cache or DB).
func GetTokenExpiredTime() (expiredTime int64, exists bool) {
	if userInfoCache.ExpiredTime == 0 {
		entity, err := repository.GetAccessToken(storage.DB)
		if err != nil {
			return 0, false
		}
		userInfoCache.AccessToken = entity.AccessToken
		userInfoCache.ExpiredTime = entity.ExpiredTime
		userInfoCache.UserId = entity.UserId
		userInfoCache.RefreshToken = entity.RefreshToken
	}
	return userInfoCache.ExpiredTime, true
}
