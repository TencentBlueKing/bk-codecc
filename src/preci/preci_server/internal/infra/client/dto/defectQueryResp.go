package dto

// DefectQueryResp 告警查询响应（对应 Java Result<LintDefectQueryRspVO>）
type DefectQueryResp struct {
	CodeCCBaseResponse
	Data LintDefectQueryData `json:"data"`
}

type LintDefectQueryData struct {
	SeriousCount int            `json:"seriousCount"`
	NormalCount  int            `json:"normalCount"`
	PromptCount  int            `json:"promptCount"`
	TotalCount   int            `json:"totalCount"`
	ExistCount   int            `json:"existCount"`
	FixCount     int            `json:"fixCount"`
	IgnoreCount  int            `json:"ignoreCount"`
	NewCount     int            `json:"newCount"`
	HistoryCount int            `json:"historyCount"`
	DefectList   LintDefectPage `json:"defectList"`
}

type LintDefectPage struct {
	Count      int          `json:"count"`
	Page       int          `json:"page"`
	PageSize   int          `json:"pageSize"`
	TotalPages int          `json:"totalPages"`
	Records    []LintDefect `json:"records"`
}

type LintDefect struct {
	EntityId   string   `json:"entityId"`
	FileName   string   `json:"fileName"`
	LineNum    int      `json:"lineNum"`
	Author     []string `json:"author"`
	Checker    string   `json:"checker"`
	Severity   int      `json:"severity"`
	Message    string   `json:"message"`
	DefectType int      `json:"defectType"`
	Status     int      `json:"status"`
	RelPath    string   `json:"relPath"`
	ToolName   string   `json:"toolName"`
}
