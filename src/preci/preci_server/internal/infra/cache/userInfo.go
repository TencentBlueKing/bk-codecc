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

// UserInfo 登录用户的内存缓存信息，包含 token、过期时间及所属项目
type UserInfo struct {
	AccessToken  string
	RefreshToken string
	ExpiredTime  int64
	UserId       string
	ProjectId    string
}

// RefreshedToken 刷新 token 后返回的结果，包含新的 access/refresh token 及过期时间
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

// RegisterTokenRefresher 注册用于刷新 access token 的回调；通常由依赖倒置的调用方在初始化时注入
func RegisterTokenRefresher(fn func(string) (*RefreshedToken, error)) {
	tokenRefresher = fn
}

// GetUserInfo 返回当前登录用户信息。needProjectId 为 true 时若未选择项目会返回错误；内部会按需刷新 token
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

// GetAccessToken 返回当前有效的 access token；过期或即将过期时会自动刷新
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

// SetProjectId 更新当前选中的蓝盾项目 ID，同时更新内存缓存与持久化存储
func SetProjectId(projectId string) error {
	userInfoCache.ProjectId = projectId
	return repository.SaveProjectId(storage.DB, projectId)
}

// GetProjectId 返回当前选中的蓝盾项目 ID，优先从内存缓存读取
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

// SetAccessTokenCache 使用最新的 token 信息刷新内存缓存，不会写入存储
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

// InvalidateToken 将当前 access token 的过期时间置零，强制下一次获取时触发刷新
func InvalidateToken() {
	userInfoCache.ExpiredTime = 0
}

// ForceRefresh 强制刷新当前 access token，无论是否已过期
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
