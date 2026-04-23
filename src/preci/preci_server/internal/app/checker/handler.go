package checker

import (
	"codecc/preci_server/internal/domain/checker"
	"codecc/preci_server/internal/domain/checker/model"
	"codecc/preci_server/internal/infra/web"
	"net/http"
)

func SelectHandler(req *http.Request) web.Encoder {
	var request SelectReq
	err := web.Decode(req, &request)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	succCheckerSets, err := checker.SelectCheckerSet(request.ProjectRootDir, request.CheckerSets)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{
		Body: &SelectResp{
			CheckerSets: succCheckerSets,
		},
	}
}

func UnselectHandler(req *http.Request) web.Encoder {
	var request SelectReq
	err := web.Decode(req, &request)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	succCheckerSets, err := checker.UnselectCheckerSet(request.ProjectRootDir, request.CheckerSets)
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	return web.SimpleEncoder{
		Body: &SelectResp{
			CheckerSets: succCheckerSets,
		},
	}
}

func ListHandler(_ *http.Request) web.Encoder {
	err, checkerSets := checker.ListOfficialCheckerSet()
	if err != nil {
		return web.SimpleEncoder{
			Err: err,
		}
	}

	var simpleCheckerSets []model.CheckerSet
	for _, checkerSet := range checkerSets {
		simpleCheckerSet := model.CheckerSet{
			CheckerSetId:   checkerSet.CheckerSetId,
			CheckerSetName: checkerSet.CheckerSetName,
			ToolName:       checkerSet.ToolName,
		}
		simpleCheckerSets = append(simpleCheckerSets, simpleCheckerSet)
	}

	return web.SimpleEncoder{
		Body: &CheckerSetListResp{
			CheckerSets: simpleCheckerSets,
		},
	}
}
