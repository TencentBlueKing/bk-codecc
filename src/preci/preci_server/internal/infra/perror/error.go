package perror

import (
	comerror "codecc/preci_server/internal/util/perror"
)

var (
	// 认证相关错误
	ErrUnauthorized = comerror.PreCIError(401, "没有 Token")
	ErrExpiredToken = comerror.PreCIError(401, "Token 已过期")

	ErrGitOperation = comerror.PreCIError(500, "Git 操作异常")
)
