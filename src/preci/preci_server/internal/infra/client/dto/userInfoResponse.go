package dto

type UserInfoResponse struct {
	CodeCCBaseResponse
	Data struct {
		Username      string `json:"username"`
		Authenticated bool   `json:"authenticated"`
	} `json:"data"`
}
