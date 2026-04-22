package perror

import "codecc/preci_server/internal/util/perror"

var (
	ErrToolNotFound       = perror.PreCIError(404, "工具不存在")
	ErrToolRunError       = perror.PreCIError(500, "工具执行错误")
	ErrRuntimeEnvironment = perror.PreCIError(500, "运行时环境错误")
	ErrStorageError       = perror.PreCIError(500, "存储错误")
)
