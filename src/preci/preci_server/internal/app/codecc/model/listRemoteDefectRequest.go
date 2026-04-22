package model

import "encoding/json"

type ListRemoteDefectReq struct {
	ProjectRoot string `json:"projectRoot"`

	TaskIdList    []int64  `json:"taskIdList"`
	ToolNameList  []string `json:"toolNameList"`
	DimensionList []string `json:"dimensionList"`
	Checker       string   `json:"checker"`
	Author        string   `json:"author"`
	Severity      []string `json:"severity"`
	Status        []string `json:"status"`
	FileList      []string `json:"fileList"`
	DefectType    []string `json:"defectType"`
	BuildId       string   `json:"buildId"`

	PageNum   int    `json:"pageNum"`
	PageSize  int    `json:"pageSize"`
	SortField string `json:"sortField"`
	SortType  string `json:"sortType"`
}

func (req *ListRemoteDefectReq) Decode(data []byte) error {
	return json.Unmarshal(data, req)
}

type ListRemoteDefectResp struct {
	SeriousCount int            `json:"seriousCount"`
	NormalCount  int            `json:"normalCount"`
	PromptCount  int            `json:"promptCount"`
	TotalCount   int            `json:"totalCount"`
	ExistCount   int            `json:"existCount"`
	FixCount     int            `json:"fixCount"`
	IgnoreCount  int            `json:"ignoreCount"`
	Defects      []RemoteDefect `json:"defects"`
}

type RemoteDefect struct {
	FileName string   `json:"fileName"`
	FilePath string   `json:"filePath"`
	LineNum  int      `json:"lineNum"`
	Author   []string `json:"author"`
	Checker  string   `json:"checker"`
	Severity int      `json:"severity"`
	Message  string   `json:"message"`
	Status   int      `json:"status"`
	ToolName string   `json:"toolName"`
}

func (resp *ListRemoteDefectResp) Encode() ([]byte, error) {
	return json.Marshal(resp)
}
