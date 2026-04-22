package client

import "time"

var (
	BKAuthBaseURL  = "http://bkauth-test.woa.com/realms/bk-devops"
	BKAuthClientID = "bk-preci"
)

var CodeCCBaseUrl = "https://codecc.woa.com"
var BkRepoBaseUrl = "http://test.devnet.bkrepo.woa.com"
var BkRepoProject = "bkdevops"
var BkRepoRepo = "static"
var BkRepoDownloadFolder = "http://devnet.bkrepo.woa.com/generic/bkdevops/static/gw/resource/preci/v2/prod"
var BkRepoUploadSubPath = "gw/resource/preci/v2/log"

// 默认 web 配置
const (
	DefaultTimeout = 30 * time.Second
	UploadTimeout  = 10 * time.Minute // 文件上传超时时间
)
