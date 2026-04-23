package os

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestMkCodeccDir(t *testing.T) {
	// 测试用例1：创建普通目录
	testDir1 := filepath.Join(os.TempDir(), "test_dir_1")
	err := MkDir(testDir1)
	if err != nil {
		t.Errorf("创建目录失败: %v", err)
	}
	defer os.RemoveAll(testDir1)

	// 验证目录是否创建成功
	if _, err := os.Stat(testDir1); os.IsNotExist(err) {
		t.Errorf("目录未创建成功: %s", testDir1)
	}

	// 测试用例2：创建嵌套目录（跨平台路径）
	nestedPath := filepath.Join(os.TempDir(), "parent", "child", "grandchild")
	err = MkDir(nestedPath)
	if err != nil {
		t.Errorf("创建嵌套目录失败: %v", err)
	}
	defer os.RemoveAll(filepath.Join(os.TempDir(), "parent"))

	// 测试用例3：创建已存在的目录（应该返回成功）
	err = MkDir(testDir1)
	if err != nil {
		t.Errorf("创建已存在目录应该返回成功: %v", err)
	}
}

func TestIsParentPath(t *testing.T) {
	// 测试用例1：直接父子关系
	parent := "/home/user"
	child := "/home/user/documents"
	if !IsParentPath(parent, child) {
		t.Errorf("应该识别为父子关系: %s -> %s", parent, child)
	}

	// 测试用例2：多级父子关系
	parent2 := "/home"
	child2 := "/home/user/documents/files"
	if !IsParentPath(parent2, child2) {
		t.Errorf("应该识别为多级父子关系: %s -> %s", parent2, child2)
	}

	// 测试用例3：相同路径（返回true）
	samePath := "/home/user"
	if !IsParentPath(samePath, samePath) {
		t.Errorf("相同路径应该返回 true: %s", samePath)
	}

	// 测试用例4：非父子关系
	path1 := "/home/user1"
	path2 := "/home/user2/documents"
	if IsParentPath(path1, path2) {
		t.Errorf("不应该识别为父子关系: %s -> %s", path1, path2)
	}

	// 测试用例5：相对路径
	relParent := "parent"
	relChild := "parent/child"
	if !IsParentPath(relParent, relChild) {
		t.Errorf("相对路径应该识别为父子关系: %s -> %s", relParent, relChild)
	}

	// 测试用例6：包含点号的路径
	dotParent := "/home/user/.."
	dotChild := "/home/user/../documents"
	if !IsParentPath(dotParent, dotChild) {
		t.Errorf("包含点号的路径应该识别为父子关系: %s -> %s", dotParent, dotChild)
	}

	// 测试用例7：Windows风格路径（跨平台测试）
	winParent := "C:\\Users\\test"
	winChild := "C:\\Users\\test\\Documents"
	if !IsParentPath(winParent, winChild) {
		t.Errorf("Windows路径应该识别为父子关系: %s -> %s", winParent, winChild)
	}

	// 测试用例8：避免误匹配（/a/b 不应该匹配 /a/bc）
	pathA := "/a/b"
	pathB := "/a/bc"
	if IsParentPath(pathA, pathB) {
		t.Errorf("不应该误匹配: %s -> %s", pathA, pathB)
	}
}

// ========== ReplaceFile 测试 ==========

func TestReplaceFile_BasicReplace(t *testing.T) {
	dir := t.TempDir()
	src := filepath.Join(dir, "src.txt")
	dest := filepath.Join(dir, "dest.txt")

	os.WriteFile(src, []byte("new content"), 0644)
	os.WriteFile(dest, []byte("old content"), 0644)

	if err := ReplaceFile(src, dest); err != nil {
		t.Fatalf("ReplaceFile 失败: %v", err)
	}
	defer CleanupBackups()

	data, _ := os.ReadFile(dest)
	if string(data) != "new content" {
		t.Errorf("期望 'new content'，得到 '%s'", string(data))
	}
}

func TestReplaceFile_DestNotExist(t *testing.T) {
	dir := t.TempDir()
	src := filepath.Join(dir, "src.txt")
	dest := filepath.Join(dir, "dest_new.txt")

	os.WriteFile(src, []byte("hello"), 0644)

	if err := ReplaceFile(src, dest); err != nil {
		t.Fatalf("ReplaceFile（目标不存在）失败: %v", err)
	}
	defer CleanupBackups()

	data, _ := os.ReadFile(dest)
	if string(data) != "hello" {
		t.Errorf("期望 'hello'，得到 '%s'", string(data))
	}
}

func TestReplaceFile_PreservesPermission(t *testing.T) {
	dir := t.TempDir()
	src := filepath.Join(dir, "src.bin")
	dest := filepath.Join(dir, "dest.bin")

	os.WriteFile(src, []byte("exec"), 0755)
	os.WriteFile(dest, []byte("old"), 0644)

	if err := ReplaceFile(src, dest); err != nil {
		t.Fatalf("ReplaceFile 失败: %v", err)
	}
	defer CleanupBackups()

	info, _ := os.Stat(dest)
	if info.Mode().Perm()&0100 == 0 {
		t.Error("目标文件应该保留可执行权限")
	}
}

func TestReplaceFile_CreatesNewInode(t *testing.T) {
	dir := t.TempDir()
	src := filepath.Join(dir, "src.txt")
	dest := filepath.Join(dir, "dest.txt")

	os.WriteFile(src, []byte("new"), 0644)
	os.WriteFile(dest, []byte("old"), 0644)

	oldIno := fileIno(t, dest)

	if err := ReplaceFile(src, dest); err != nil {
		t.Fatalf("ReplaceFile 失败: %v", err)
	}
	defer CleanupBackups()

	newIno := fileIno(t, dest)
	// oldIno == 0 表示平台不支持 inode 比较（Windows），跳过
	if oldIno != 0 && oldIno == newIno {
		t.Error("替换后应产生新的 inode（文件被 remove-then-create）")
	}
}

// ========== removeOrRenameForReplace 测试 ==========

func TestRemoveOrRenameForReplace_RemoveSucceeds(t *testing.T) {
	dir := t.TempDir()
	f := filepath.Join(dir, "target.txt")
	os.WriteFile(f, []byte("data"), 0644)

	if err := removeOrRenameForReplace(f); err != nil {
		t.Fatalf("removeOrRenameForReplace 失败: %v", err)
	}
	defer CleanupBackups()

	if _, err := os.Stat(f); !os.IsNotExist(err) {
		t.Error("文件应该已被删除")
	}
}

func TestRemoveOrRenameForReplace_FileNotExist(t *testing.T) {
	if err := removeOrRenameForReplace("/tmp/nonexistent_file_xyz_12345"); err != nil {
		t.Fatalf("不存在的文件不应报错: %v", err)
	}
}

// ========== CleanupBackups 测试 ==========

func TestCleanupBackups(t *testing.T) {
	// 通过 ReplaceFile 间接触发备份目录创建（正常路径下 Remove 会成功，不会产生备份）
	// 这里直接测试 CleanupBackups 不会 panic
	CleanupBackups()
}

// ========== ReplaceFile 边界场景 ==========

func TestReplaceFile_SrcNotExist(t *testing.T) {
	dir := t.TempDir()
	dest := filepath.Join(dir, "dest.txt")
	os.WriteFile(dest, []byte("old"), 0644)

	err := ReplaceFile(filepath.Join(dir, "nonexistent"), dest)
	if err == nil {
		t.Fatal("源文件不存在时应该返回错误")
	}
	if !strings.Contains(err.Error(), "打开源文件失败") {
		t.Errorf("错误信息应包含 '打开源文件失败'，得到: %s", err.Error())
	}
}
