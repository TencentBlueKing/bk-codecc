package model

type ToolScanInput struct {
	ProjectName      string    `json:"projName"`
	ProjectId        string    `json:"projectId"`
	ToolName         string    `json:"toolName"`
	ScanPath         string    `json:"scanPath"`
	Language         int64     `json:"language"`
	WhitePaths       []string  `json:"whitePathList"`
	BlackPaths       []string  `json:"skipPaths"`
	ScanType         string    `json:"scanType"`
	IncrementalFiles []string  `json:"incrementalFiles"` // 增量文件列表, 需要传入绝对路径
	OpenCheckers     []Checker `json:"openCheckers"`

	// 待选区: toolOptions, buildScript, repos, commitSince, languageTag, codeccWorkspacePath, eslintRule
}

type Checker struct {
	CheckerName   string         `json:"checkerName"`
	NativeChecker bool           `json:"nativeChecker"`
	Severity      int            `json:"severity"`
	CheckerParams []CheckerParam `json:"checkerOptions"`
}

type CheckerParam struct {
	Key   string `json:"checkerOptionName"`
	Value string `json:"checkerOptionValue"`
}
