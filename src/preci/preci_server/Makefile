# 版本信息
VERSION ?= dev
CLI_LDFLAGS := -ldflags "-X codecc/preci_server/cmd/cli/cmd.Version=$(VERSION)"
SERV_LDFLAGS := -ldflags "-X codecc/preci_server/internal/domain/version.Version=$(VERSION)"
MCP_LDFLAGS := -ldflags "-X codecc/preci_server/mcp/config.Version=$(VERSION)"

.PHONY: build build-server build-cli run run-server test clean help

# 构建 win
build-win: build-server-win build-cli-win build-updater-win build-mcp-win copy-config-win copy-script-win package-win

build-server-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build $(SERV_LDFLAGS) -o bin/win/preci-server.exe ./cmd/server
build-cli-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build $(CLI_LDFLAGS) -o bin/win/preci.exe ./cmd/cli
build-updater-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o bin/win/preci-updater.exe ./cmd/updater
build-mcp-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build $(MCP_LDFLAGS) -o bin/win/preci-mcp.exe ./cmd/mcp

# 构建 linux
build-linux: build-server-linux build-cli-linux build-updater-linux build-mcp-linux copy-config-linux copy-script-linux package-linux

build-server-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(SERV_LDFLAGS) -o bin/linux/preci-server ./cmd/server
build-cli-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(CLI_LDFLAGS) -o bin/linux/preci ./cmd/cli
build-updater-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o bin/linux/preci-updater ./cmd/updater
build-mcp-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(MCP_LDFLAGS) -o bin/linux/preci-mcp ./cmd/mcp

# 构建 mac-amd64
build-mac-amd64: build-server-mac-amd64 build-cli-mac-amd64 build-updater-mac-amd64 build-mcp-mac-amd64 copy-config-mac-amd64 copy-script-mac-amd64 package-mac-amd64

build-server-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build $(SERV_LDFLAGS) -o bin/mac-amd64/preci-server ./cmd/server
build-cli-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build $(CLI_LDFLAGS) -o bin/mac-amd64/preci ./cmd/cli
build-updater-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -o bin/mac-amd64/preci-updater ./cmd/updater
build-mcp-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build $(MCP_LDFLAGS) -o bin/mac-amd64/preci-mcp ./cmd/mcp

# 构建 mac-arm64
build-mac-arm64: build-server-mac-arm64 build-cli-mac-arm64 build-updater-mac-arm64 build-mcp-mac-arm64 copy-config-mac-arm64 copy-script-mac-arm64 package-mac-arm64

build-server-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build $(SERV_LDFLAGS) -o bin/mac-arm64/preci-server ./cmd/server
build-cli-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build $(CLI_LDFLAGS) -o bin/mac-arm64/preci ./cmd/cli
build-updater-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -o bin/mac-arm64/preci-updater ./cmd/updater
build-mcp-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build $(MCP_LDFLAGS) -o bin/mac-arm64/preci-mcp ./cmd/mcp

# 复制配置文件（按平台拆分，便于单独构建）
copy-config: copy-config-win copy-config-linux copy-config-mac-amd64 copy-config-mac-arm64

copy-config-win:
	@cp -r config bin/win/
	@cp -r checkerset bin/win/

copy-config-linux:
	@cp -r config bin/linux/
	@cp -r checkerset bin/linux/

copy-config-mac-amd64:
	@cp -r config bin/mac-amd64/
	@cp -r checkerset bin/mac-amd64/

copy-config-mac-arm64:
	@cp -r config bin/mac-arm64/
	@cp -r checkerset bin/mac-arm64/

# 复制各平台对应的安装/卸载脚本到 bin/<platform>/
copy-script: copy-script-win copy-script-linux copy-script-mac-amd64 copy-script-mac-arm64

copy-script-win:
	@cp -r script/win/. bin/win/

copy-script-linux:
	@cp -r script/unix/. bin/linux/

copy-script-mac-amd64:
	@cp -r script/unix/. bin/mac-amd64/

copy-script-mac-arm64:
	@cp -r script/unix/. bin/mac-arm64/

# 压缩打包（按平台产出对应 zip 压缩包，输出到 bin/dist/）
package: package-win package-linux package-mac-amd64 package-mac-arm64

package-win:
	@cd bin && zip -qr dist/preci-$(VERSION)-win-amd64.zip win
	@echo "Packaged: bin/dist/preci-$(VERSION)-win-amd64.zip"

package-linux:
	@cd bin && zip -qr dist/preci-$(VERSION)-linux-amd64.zip linux
	@echo "Packaged: bin/dist/preci-$(VERSION)-linux-amd64.zip"

package-mac-amd64:
	@cd bin && zip -qr dist/preci-$(VERSION)-mac-amd64.zip mac-amd64
	@echo "Packaged: bin/dist/preci-$(VERSION)-mac-amd64.zip"

package-mac-arm64:
	@cd bin && zip -qr dist/preci-$(VERSION)-mac-arm64.zip mac-arm64
	@echo "Packaged: bin/dist/preci-$(VERSION)-mac-arm64.zip"


# 初始化：清理并统一创建所有输出目录
init:
	rm -rf bin/
	mkdir -p bin/win bin/linux bin/mac-amd64 bin/mac-arm64 bin/dist
	go mod tidy

# 默认构建所有
build: build-server build-cli

# 构建服务端
build-server:
	go build -o bin/preci-server ./cmd/server

# 构建 CLI
build-cli:
	go build -o bin/preci ./cmd/cli

# 构建 MCP
build-mcp:
	go build -o bin/preci-mcp ./cmd/mcp