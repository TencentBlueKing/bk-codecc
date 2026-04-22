package perror

import "fmt"

var (
	ErrServerInvalid  = fmt.Errorf("preci-server invalid")        // preci-server 不可用
	ErrRequestFailed  = fmt.Errorf("preci-server request failed") // 请求 preci-server 失败
	ErrNotInitialized = fmt.Errorf("not initialized")             // 未初始化
)
