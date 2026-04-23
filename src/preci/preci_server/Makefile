# 版本信息
VERSION ?= dev
CLI_LDFLAGS := -ldflags "-X codecc/preci_server/cmd/cli/cmd.Version=$(VERSION)"
SERV_LDFLAGS := -ldflags "-X codecc/preci_server/internal/domain/version.Version=$(VERSION)"
MCP_LDFLAGS := -ldflags "-X codecc/preci_server/mcp/config.Version=$(VERSION)"

.PHONY: build build-server build-cli run run-server test clean help

# 构建 win
build-win: build-server-win build-cli-win build-updater-win build-mcp-win

build-server-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build $(SERV_LDFLAGS) -o bin/preci-server.exe ./cmd/server
build-cli-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build $(CLI_LDFLAGS) -o bin/preci.exe ./cmd/cli
build-updater-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o bin/preci-updater.exe ./cmd/updater
build-mcp-win:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build $(MCP_LDFLAGS) -o bin/preci-mcp.exe ./cmd/mcp

# 构建 linux
build-linux: build-server-linux build-cli-linux build-updater-linux build-mcp-linux

build-server-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(SERV_LDFLAGS) -o bin/preci-server ./cmd/server
build-cli-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(CLI_LDFLAGS) -o bin/preci ./cmd/cli
build-updater-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o bin/preci-updater ./cmd/updater
build-mcp-linux:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(MCP_LDFLAGS) -o bin/preci-mcp ./cmd/mcp

# 构建 mac-amd64
build-mac-amd64: build-server-mac-amd64 build-cli-mac-amd64 build-updater-mac-amd64 build-mcp-mac-amd64

build-server-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build $(SERV_LDFLAGS) -o bin/preci-server ./cmd/server
build-cli-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build $(CLI_LDFLAGS) -o bin/preci ./cmd/cli
build-updater-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -o bin/preci-updater ./cmd/updater
build-mcp-mac-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build $(MCP_LDFLAGS) -o bin/preci-mcp ./cmd/mcp

# 构建 mac-arm64
build-mac-arm64: build-server-mac-arm64 build-cli-mac-arm64 build-updater-mac-arm64 build-mcp-mac-arm64

build-server-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build $(SERV_LDFLAGS) -o bin/preci-server ./cmd/server
build-cli-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build $(CLI_LDFLAGS) -o bin/preci ./cmd/cli
build-updater-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -o bin/preci-updater ./cmd/updater
build-mcp-mac-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build $(MCP_LDFLAGS) -o bin/preci-mcp ./cmd/mcp

# 复制配置文件
copy-config:
	@cp -r config bin/
	@cp -r checkerset bin/

init:
	rm -rf bin/
	mkdir bin
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