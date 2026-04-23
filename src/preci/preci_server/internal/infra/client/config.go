package client

import "time"

// 以下默认值留空，具体地址由外部配置文件（如 initConfig.json / authConfig.json）注入，
// 避免在源码中硬编码任何内部域名。
var (
	BKAuthBaseURL  = ""
	BKAuthClientID = "bk-preci"
)

var CodeCCBaseUrl = ""
var BkRepoBaseUrl = ""
var BkRepoProject = "bkdevops"
var BkRepoRepo = "static"
var BkRepoDownloadFolder = ""
var BkRepoUploadSubPath = "gw/resource/preci/v2/log"

// 默认 web 配置
const (
	DefaultTimeout = 30 * time.Second
	UploadTimeout  = 10 * time.Minute // 文件上传超时时间
)
