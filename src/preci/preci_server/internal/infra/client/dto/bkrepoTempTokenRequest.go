package dto

type BkrepoTempTokenResp struct {
	CodeCCBaseResponse
	Data BkrepoTempTokenDto `json:"data"`
}

type BkrepoTempTokenDto struct {
	Token string `json:"token"`
}
