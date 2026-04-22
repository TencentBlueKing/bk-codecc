package os

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

// MkDir 创建文件夹
func MkDir(dirPath string) error {
	// 规范化路径
	normalPath := filepath.Clean(dirPath)

	if _, err := os.Stat(normalPath); err == nil {
		// 目录已存在，直接返回创建成功
		return nil
	}

	// 获取上级目录
	parentPath := filepath.Dir(normalPath)
	if parentPath == normalPath {
		// 路径是根目录，返回错误
		return fmt.Errorf("不建议在根目录创建 codecc 相关文件夹")
	}

	if parDir, err := os.Stat(parentPath); err != nil || !parDir.IsDir() {
		// 父目录不存在，返回错误
		return fmt.Errorf("父目录不存在: %v", err)
	}

	// 创建目录
	// MkdirAll 会自动处理跨平台路径分隔符和权限
	err := os.MkdirAll(normalPath, 0755)
	if err != nil {
		return fmt.Errorf("创建目录失败: %v", err)
	}

	return nil
}

// IsParentPath 判断 parentPath 是否是 childPath 的父路径.  相同目录返回 true
func IsParentPath(parentPath, childPath string) bool {
	// 清理路径
	cleanParent := filepath.Clean(parentPath)
	cleanChild := filepath.Clean(childPath)

	// 如果路径相同，直接返回 true
	if cleanParent == cleanChild {
		return true
	}

	// 转换为绝对路径
	absParent, err := filepath.Abs(cleanParent)
	if err != nil {
		return false
	}

	absChild, err := filepath.Abs(cleanChild)
	if err != nil {
		return false
	}

	// 添加路径分隔符确保准确比较（避免 /a/b 匹配 /a/bc 的情况）
	absParentWithSep := absParent + string(filepath.Separator)
	absChildWithSep := absChild + string(filepath.Separator)

	// 检查是否是前缀关系
	return strings.HasPrefix(absChildWithSep, absParentWithSep)
}

// IsExist 判断文件是否存在
func IsExist(filename string) bool {
	_, err := os.Stat(filename)
	return err == nil || os.IsExist(err)
}

// ReadFile 读取文件
func ReadFile(path string) ([]byte, error) {
	return os.ReadFile(path)
}

// backupDir 用于存放 ReplaceFile 在 Windows 上因文件锁定而产生的备份文件
var (
	backupDir  string
	backupOnce sync.Once
)

// getBackupDir 返回（必要时创建）用于存放备份文件的临时目录
func getBackupDir() (string, error) {
	var initErr error
	backupOnce.Do(func() {
		var dir string
		dir, initErr = os.MkdirTemp("", "preci_backup_*")
		if initErr != nil {
			return
		}
		backupDir = dir
	})
	if initErr != nil {
		return "", fmt.Errorf("创建备份临时目录失败: %w", initErr)
	}
	return backupDir, nil
}

// ReplaceFile 拿 src 文件替换 dest 文件
//
// 替换前先删除目标文件（Linux 上可对正在运行的可执行文件执行 unlink），
// 若删除失败（Windows 会锁定运行中的可执行文件），则将旧文件移到临时目录，
// 最后可通过 CleanupBackups 统一清理。
func ReplaceFile(src, dest string) error {
	srcFile, err := os.Open(src)
	if err != nil {
		return fmt.Errorf("打开源文件失败: %w", err)
	}
	defer srcFile.Close()

	srcInfo, err := srcFile.Stat()
	if err != nil {
		return fmt.Errorf("获取源文件信息失败: %w", err)
	}

	if err = removeOrRenameForReplace(dest); err != nil {
		return err
	}

	destFile, err := os.OpenFile(dest, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, srcInfo.Mode())
	if err != nil {
		return fmt.Errorf("创建目标文件失败: %w", err)
	}
	defer destFile.Close()

	if _, err := io.Copy(destFile, srcFile); err != nil {
		return fmt.Errorf("复制文件内容失败: %w", err)
	}

	return nil
}

// removeOrRenameForReplace 先尝试 os.Remove，失败则 rename 到临时目录
func removeOrRenameForReplace(dest string) error {
	if err := os.Remove(dest); err == nil || os.IsNotExist(err) {
		return nil
	}
	// Remove 失败（通常是 Windows 文件锁），将旧文件移到临时目录
	bkDir, err := getBackupDir()
	if err != nil {
		return err
	}
	backupPath := filepath.Join(bkDir, filepath.Base(dest))
	if err := os.Rename(dest, backupPath); err != nil {
		return fmt.Errorf("无法移除或重命名目标文件 %s: %w", dest, err)
	}
	return nil
}

// CleanupBackups 清理 ReplaceFile 产生的备份临时目录
func CleanupBackups() {
	if backupDir != "" {
		os.RemoveAll(backupDir)
	}
}
