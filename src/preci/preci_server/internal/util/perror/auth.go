package perror

var (
	ErrInvalidAccessToken = PreCIError(CodeInvalidAccessToken, "access token 无效, 请重新登录")
)
