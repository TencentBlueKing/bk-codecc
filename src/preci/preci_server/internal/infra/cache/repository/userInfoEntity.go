package repository

import (
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util"
	"fmt"
)

const (
	bucketName      = "b_task_info" // 跟 taskInfoEntity 共用一个桶
	accessTokenKey  = "userAccessToken"
	refreshTokenKey = "userRefreshToken"
	expiredTimeKey  = "userExpiredTime"
	projectIdKey    = "userProjectId"
	userIdKey       = "userUserId"
)

type UserInfoEntity struct {
	AccessToken  string `json:"AccessToken"`
	RefreshToken string `json:"RefreshToken"`
	ExpiredTime  int64  `json:"ExpiredTime"` // 过期时间戳. 单位: 秒
	UserId       string `json:"UserId"`
	ProjectId    string `json:"ProjectId"` // 蓝盾项目 ID
}

func SaveAccessToken(sto storage.Storage, userId, accessToken, refreshToken string, expiredTime int64) error {
	log := logger.GetLogger()

	if err := sto.UpdateByStrKey(bucketName, userIdKey, []byte(userId)); err != nil {
		log.Error(fmt.Sprintf("Save user id error: %v", err))
		return err
	}

	if err := sto.UpdateByStrKey(bucketName, accessTokenKey, []byte(accessToken)); err != nil {
		log.Error(fmt.Sprintf("Save AccessToken error: %v", err))
		return err
	}

	if err := sto.UpdateByStrKey(bucketName, refreshTokenKey, []byte(refreshToken)); err != nil {
		log.Error(fmt.Sprintf("Save RefreshToken error: %v", err))
		return err
	}

	if err := sto.UpdateByStrKey2Int(bucketName, expiredTimeKey, expiredTime); err != nil {
		log.Error(fmt.Sprintf("Save AccessToken expired time error: %v", err))
		return err
	}

	return nil
}

func SaveProjectId(sto storage.Storage, projectId string) error {
	log := logger.GetLogger()

	if err := sto.UpdateByStrKey(bucketName, projectIdKey, []byte(projectId)); err != nil {
		log.Error(fmt.Sprintf("Save project id error: %v", err))
		return err
	}

	return nil
}

func GetProjectId(sto storage.Storage) (string, error) {
	log := logger.GetLogger()
	result := ""

	if data, err := sto.GetByStrKey(bucketName, projectIdKey); err != nil {
		log.Error(fmt.Sprintf("Get project id error: %v", err))
		return result, err
	} else {
		result = string(data)
	}

	return result, nil
}

func GetAccessToken(sto storage.Storage) (*UserInfoEntity, error) {
	log := logger.GetLogger()
	result := new(UserInfoEntity)

	if data, err := sto.GetByStrKey(bucketName, userIdKey); err != nil {
		log.Error(fmt.Sprintf("Get user id error: %v", err))
		return nil, err
	} else {
		// []byte 转 string
		result.UserId = string(data)
	}

	if data, err := sto.GetByStrKey(bucketName, accessTokenKey); err != nil {
		log.Error(fmt.Sprintf("Get AccessToken error: %v", err))
		return nil, err
	} else {
		// []byte 转 string
		result.AccessToken = string(data)
	}

	if data, err := sto.GetByStrKey(bucketName, expiredTimeKey); err != nil {
		log.Error(fmt.Sprintf("Get AccessToken expired time error: %v", err))
		return nil, err
	} else {
		// []byte 转 int64
		expiredTime, err := util.BytesToInt64(data)
		if err != nil {
			log.Error(fmt.Sprintf("Parse expired time error: %v", err))
			return nil, err
		}

		result.ExpiredTime = expiredTime
	}

	if data, err := sto.GetByStrKey(bucketName, refreshTokenKey); err != nil {
		// refresh token 可能不存在（旧版 pin+token 登录），不视为错误
		log.Info(fmt.Sprintf("No refresh token found in DB. %v", err))
	} else {
		result.RefreshToken = string(data)
	}

	return result, nil
}
