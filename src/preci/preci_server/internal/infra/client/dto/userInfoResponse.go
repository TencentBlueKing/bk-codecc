package dto

// UserInfoResponse 查询当前登录用户信息接口的响应
type UserInfoResponse struct {
	CodeCCBaseResponse
	Data struct {
		Username      string `json:"username"`
		Authenticated bool   `json:"authenticated"`
	} `json:"data"`
}
