package perror

var (
	ErrMCPServerRunning = PreCIError(CodeMCPServerRunning, "MCP 服务已在运行")
)
