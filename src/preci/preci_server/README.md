# PreCI - 代码检查本地服务（社区版）

PreCI 是一个轻量级的代码静态分析本地服务工具，旨在为开发者提供快速、便捷的代码质量检查能力。通过本地运行的服务架构，PreCI 可以在代码开发过程中辅助检测代码问题，帮助团队提升代码质量。

## ✨ 特性

- 🚀 **本地服务架构** - 服务运行在本地
- 🔧 **多语言支持** - 支持多种编程语言的代码扫描
- 🔍 **多种扫描模式** - 全量扫描、指定扫描、Pre-Commit 扫描、Pre-Push 扫描
- 📋 **规则集管理** - 灵活配置检查规则集
- 🖥️ **跨平台** - 支持 Windows、Linux、macOS (AMD64/ARM64)
- ⚡ **轻量高效** - 使用嵌入式数据库，无需额外依赖

## 🛠️ 支持的代码检查工具

| 工具 | 语言 | 说明 |
|------|------|------|
| DETEKT | Kotlin | Kotlin 静态代码分析工具 |
| GOML (GoMetaLinter) | Go | Go 语言多工具聚合检查器 |
| CPPLINT | C++ | C++ 代码规范检查工具 |
| RESHARPER | C# | JetBrains ReSharper C# 代码分析工具 |
| CHECKSTYLE | Java | Java 代码规范检查工具 |
| ESLINT | JavaScript | JavaScript/TypeScript 代码检查工具 |
| PYLINT | Python | Python 代码规范检查工具 |
| ACTION_CODE_CHECK | AI 检查 | 星图智检 AI 代码检查工具 |

## 📦 安装
详见 [PreCI 构建部署文档](/PreCI%20构建部署文档.md)

## 🚀 快速开始

> 所有命令均可通过 `-h` 参数查看帮助信息，例如：`preci scan -h`

### 1. 启动服务

```bash
# 启动本地服务
preci server start

# 停止服务
preci server stop
```

### 2. 登录认证

```bash
# OAuth 设备码登录（通过浏览器完成授权）
preci login

# 登录并绑定蓝盾项目
preci login -p <蓝盾项目id>
```

### 3. 初始化项目

```bash
# 在项目目录下执行，由 PreCI 自动推断项目根目录
preci init

# 或指定项目根目录
preci init --root_path /path/to/project/root
```

### 4. 配置规则集

```bash
# 查看可用的规则集列表
preci checkerset list

# 选择要使用的规则集
preci checkerset select <checker_set_id>

# 选择多个规则集（逗号分隔）
preci checkerset select <checker_set_id_1>,<checker_set_id_2>
```

### 5. 执行代码扫描

```bash
# 全量扫描当前项目
preci scan

# 扫描指定路径
preci scan --path /path/to/scan

# 扫描多个路径（逗号分隔）
preci scan --path /path/1,/path/2

# Pre-commit 模式扫描
preci scan --pre-commit

# Pre-push 模式扫描
preci scan --pre-push

# 静默模式（不显示进度）
preci scan --silence
```

### 6. 查看扫描结果

```bash
# 查看当前扫描进度
preci scan status

# 查看扫描结果
preci scan result

# 查看所有结果（默认只显示前 10 条）
preci scan result -a
```

## 🗑️ 卸载

提供卸载脚本：
- `/script/unix/uninstall.sh`
- `/script/win/uninstall.ps1`

也可自行手动删除安装目录，并从 shell 配置文件中移除 PreCI 相关的环境变量。
