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

func NewOauthToken2(userId, accessToken, refreshToken string, expiresIn int64) OauthToken {
	return OauthToken{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
		ExpiresIn:    expiresIn,
		UserId:       userId,
		ExpiredTime:  time.Now().Unix() + expiresIn,
	}
}

func (t *OauthToken) Save(sto storage.Storage) error {
	return repository.SaveAccessToken(sto, t.UserId, t.AccessToken, t.RefreshToken, t.ExpiredTime)
}
