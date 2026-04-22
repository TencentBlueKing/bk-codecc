package model

// LandunProject 蓝盾项目
type LandunProject struct {
	UID         string `json:"uid" necessary:""`
	AccessToken string `json:"accessToken" necessary:""`
	BkTicket    string `json:"bk_ticket" necessary:""`
}
