package dto

type BatchGetToolMetaResp struct {
	CodeCCBaseResponse
	Data BatchGetToolMetaDto `json:"data"`
}

type BatchGetToolMetaDto struct {
	ToolMetas []ToolMetaDetail `json:"toolMetas"`
}

type ToolMetaDetail struct {
	Name         string        `json:"name"`
	DisplayName  string        `json:"displayName"`
	Lang         int64         `json:"lang"`
	ToolVersions []ToolVersion `json:"toolVersions"`
}

type ToolVersion struct {
	// 工具版本类型，T-测试版本，G-灰度版本，P-正式发布版本，O-开源扫描
	VersionType string `json:"versionType"`

	// docker启动运行的命令
	DockerTriggerShell string `json:"dockerTriggerShell"`

	// docker镜像存放URL
	DockerImageURL string `json:"dockerImageURL"`

	// 工具docker镜像版本号
	DockerImageVersion string `json:"dockerImageVersion"`

	// 工具外部docker镜像版本号，用于关联第三方直接提供的docker镜像版本
	ForeignDockerImageVersion string `json:"foreignDockerImageVersion"`

	// docker镜像hash值
	DockerImageHash string `json:"dockerImageHash"`

	// 工具二进制相关信息
	Binary Binary `json:"binary"`

	// 更新时间
	UpdatedDate int64 `json:"updatedDate"`

	// 更新人
	UpdatedBy string `json:"updatedBy"`
}

type Binary struct {
	WinUrl        string `json:"winUrl"`        // win二进制的下载路径
	LinuxUrl      string `json:"linuxUrl"`      // linux二进制的下载路径
	MacUrl        string `json:"macUrl"`        // mac二进制的下载路径
	WinCommand    string `json:"winCommand"`    // win环境下命令行
	LinuxCommand  string `json:"linuxCommand"`  // linux环境下命令行
	MacCommand    string `json:"macCommand"`    // mac环境下命令行
	BinaryVersion string `json:"binaryVersion"` // 二进制工具版本
}
