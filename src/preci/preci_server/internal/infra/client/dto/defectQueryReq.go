package dto

// DefectQueryReq 对应 Java DefectQueryReqVO（/issue/list 接口）
type DefectQueryReq struct {
	TaskIdList    []int64  `json:"taskIdList,omitempty"`
	ToolNameList  []string `json:"toolNameList,omitempty"`
	DimensionList []string `json:"dimensionList,omitempty"`
	Checker       string   `json:"checker,omitempty"`
	Author        string   `json:"author,omitempty"`
	Severity      []string `json:"severity,omitempty"`
	Status        []string `json:"status,omitempty"`
	FileList      []string `json:"fileList,omitempty"`
	DefectType    []string `json:"defectType,omitempty"`
	ClusterType   string   `json:"clusterType,omitempty"`
	BuildId       string   `json:"buildId,omitempty"`
	CheckerSets   []string `json:"checkerSets"`
}
