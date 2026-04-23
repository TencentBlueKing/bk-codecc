package dto

// BkRepoUploadResponse BkRepo 上传文件响应
type BkRepoUploadResponse struct {
	Code    int             `json:"code"`
	Message string          `json:"message"`
	Data    *BkRepoNodeInfo `json:"data"`
	TraceId string          `json:"traceId"`
}

// BkRepoNodeInfo BkRepo 节点信息
type BkRepoNodeInfo struct {
	CreatedBy        string                 `json:"createdBy"`
	CreatedDate      string                 `json:"createdDate"`
	LastModifiedBy   string                 `json:"lastModifiedBy"`
	LastModifiedDate string                 `json:"lastModifiedDate"`
	Folder           bool                   `json:"folder"`
	Path             string                 `json:"path"`
	Name             string                 `json:"name"`
	FullPath         string                 `json:"fullPath"`
	Size             int64                  `json:"size"`
	Sha256           string                 `json:"sha256"`
	Md5              string                 `json:"md5"`
	Metadata         map[string]interface{} `json:"metadata"`
	ProjectId        string                 `json:"projectId"`
	RepoName         string                 `json:"repoName"`
}
