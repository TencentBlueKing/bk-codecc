package dto

// BatchGetToolMetaResp 批量查询工具元信息接口的响应
type BatchGetToolMetaResp struct {
	CodeCCBaseResponse
	Data BatchGetToolMetaDto `json:"data"`
}

// BatchGetToolMetaDto 批量查询工具元信息接口响应的数据体，包含工具元信息列表
type BatchGetToolMetaDto struct {
	ToolMetas []ToolMetaDetail `json:"toolMetas"`
}

// ToolMetaDetail 单个工具的元信息详情，包含工具名、展示名、语言及各版本信息
type ToolMetaDetail struct {
	Name         string        `json:"name"`
	DisplayName  string        `json:"displayName"`
	Lang         int64         `json:"lang"`
	ToolVersions []ToolVersion `json:"toolVersions"`
}

// ToolVersion 工具的具体版本信息，包含 docker 镜像、二进制及维护元数据
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

// Binary 工具二进制分发信息，按平台区分下载地址和启动命令
type Binary struct {
	WinUrl        string `json:"winUrl"`        // win二进制的下载路径
	LinuxUrl      string `json:"linuxUrl"`      // linux二进制的下载路径
	MacUrl        string `json:"macUrl"`        // mac二进制的下载路径
	WinCommand    string `json:"winCommand"`    // win环境下命令行
	LinuxCommand  string `json:"linuxCommand"`  // linux环境下命令行
	MacCommand    string `json:"macCommand"`    // mac环境下命令行
	BinaryVersion string `json:"binaryVersion"` // 二进制工具版本
}
