package model

import (
	"codecc/preci_server/internal/infra/cache/repository"
	"codecc/preci_server/internal/infra/storage"
	"time"
)

// OauthToken OAuth 鉴权 token 模型，保存 access/refresh token 及过期时间、关联用户等信息
type OauthToken struct {
	AccessToken  string `json:"AccessToken"`
	RefreshToken string `json:"RefreshToken"`
	ExpiresIn    int64  `json:"ExpiresIn"`
	ExpiredTime  int64  `json:"ExpiredTime"`
	UserId       string `json:"UserId"`
	UserType     string `json:"UserType"`
}

// NewOauthToken2 根据登录信息创建 OauthToken 实例并计算过期时间
func NewOauthToken2(userId, accessToken, refreshToken string, expiresIn int64) OauthToken {
	return OauthToken{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
		ExpiresIn:    expiresIn,
		UserId:       userId,
		ExpiredTime:  time.Now().Unix() + expiresIn,
	}
}

// Save 将 OAuth token 信息持久化到存储层
func (t *OauthToken) Save(sto storage.Storage) error {
	return repository.SaveAccessToken(sto, t.UserId, t.AccessToken, t.RefreshToken, t.ExpiredTime)
}
