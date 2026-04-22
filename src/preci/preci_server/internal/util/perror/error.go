package perror

import (
	"errors"
	"fmt"
)

// PreCIErr 自定义错误类型，包含错误码和消息
type PreCIErr struct {
	Code int
	Msg  string
}

// Error 实现 error 接口
func (e *PreCIErr) Error() string {
	return fmt.Sprintf("[%d] %s", e.Code, e.Msg)
}

// Is 实现 errors.Is 支持，用于错误比较
func (e *PreCIErr) Is(target error) bool {
	var t *PreCIErr
	ok := errors.As(target, &t)
	if !ok {
		return false
	}
	return e.Code == t.Code
}

// PreCIError 创建一个 PreCIErr 错误
func PreCIError(code int, msg string) error {
	return &PreCIErr{Code: code, Msg: msg}
}

// GetErrorCode 从错误中提取错误码
// 如果错误不是 PreCIErr 类型，返回 0
func GetErrorCode(err error) int {
	var pErr *PreCIErr
	if errors.As(err, &pErr) {
		return pErr.Code
	}
	return 0
}

// IsPreCIError 判断是否为 PreCIErr 类型错误
func IsPreCIError(err error) bool {
	var pErr *PreCIErr
	return errors.As(err, &pErr)
}

var (
	ErrExternalServiceError  = PreCIError(502, "外部服务调用失败")
	ErrUserCancel            = PreCIError(400, "用户取消")
	ErrDirectoryCreateFailed = PreCIError(500, "目录创建失败")
	ErrDirectoryReadFailed   = PreCIError(500, "目录读取失败")
	ErrFileRemoveFailed      = PreCIError(500, "文件删除失败")
	ErrFileCreateFailed      = PreCIError(500, "文件创建失败")
	ErrFileNotFound          = PreCIError(404, "文件不存在")
	ErrFileReadFailed        = PreCIError(500, "文件读取失败")
	ErrFileWriteFailed       = PreCIError(500, "文件写入失败")
	ErrFilePermissionDenied  = PreCIError(403, "文件权限不足")
	ErrUnknownError          = PreCIError(500, "未知错误")
	ErrInvalidParam          = PreCIError(400, "参数无效")
	ErrJsonDecodeError       = PreCIError(500, "json 解码失败")
	// 运行环境错误

	ErrCodeCCReqError = PreCIError(CodeCodeCCReqErr, "调用 CodeCC 请求失败")
)
