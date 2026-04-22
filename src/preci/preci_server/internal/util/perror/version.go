package perror

var (
	ErrNoLatestVersion = PreCIError(CodeNoLatestVersion, "获取当前最新版本失败, 请稍后再试或者咨询 O2000")
	ErrIsLatestVersion = PreCIError(CodeIsLatestVersion, "当前已经是最新版本")
	ErrDownloadFailed  = PreCIError(CodeDownloadFailed, "下载失败, 请稍后再试或者咨询 O2000")
	ErrDupInstall      = PreCIError(CodeDupInstall, "安装正在进行")
)
