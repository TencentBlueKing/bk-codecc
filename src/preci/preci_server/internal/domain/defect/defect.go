// Package defect implements the defect domain (loading, storing and querying
// scan defects produced by PreCI tools).
package defect

import (
	"codecc/preci_server/internal/domain/defect/model"
	"codecc/preci_server/internal/infra/logger"
	"codecc/preci_server/internal/infra/storage"
	"codecc/preci_server/internal/util/perror"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

func GetAllDefectsByPathPre(pathPre string) ([]*model.Defect, error) {
	log := logger.GetLogger()

	// 判断 pathPre 所对应的是文件还是目录
	searchPrefix := pathPre

	// 检查路径是否存在
	fileInfo, err := os.Stat(pathPre)
	if err == nil {
		// 路径存在，判断是文件还是目录
		if fileInfo.IsDir() {
			// 是目录，确保以路径分隔符结尾
			if !strings.HasSuffix(searchPrefix, string(filepath.Separator)) {
				searchPrefix = searchPrefix + string(filepath.Separator)
			}
		}
		// 如果是文件，直接使用原路径作为前缀
	} else {
		log.Error(fmt.Sprintf("path %s does not exist: %v", pathPre, err))
		// 路径不存在
		return nil, perror.ErrNoDefects
	}

	log.Info(fmt.Sprintf("start GetAllDefectsByPathPre: origin path: %s, real path: %s",
		pathPre, searchPrefix))

	return model.GetAllDefectsByPathPre(storage.DB, searchPrefix)
}
