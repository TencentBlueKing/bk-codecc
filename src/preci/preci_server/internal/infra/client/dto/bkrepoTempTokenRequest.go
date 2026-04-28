package dto

// BkrepoTempTokenResp 获取 bk-repo 临时 token 接口的响应
type BkrepoTempTokenResp struct {
	CodeCCBaseResponse
	Data BkrepoTempTokenDto `json:"data"`
}

// BkrepoTempTokenDto bk-repo 临时 token 响应的数据体
type BkrepoTempTokenDto struct {
	Token string `json:"token"`
}
