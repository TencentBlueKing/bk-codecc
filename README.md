![LOGO](docs/resource/img/codecc.png)
---
[![license](https://img.shields.io/badge/license-mit-brightgreen.svg?style=flat)](https://github.com/TencentBlueKing/bk-codecc/blob/master/LICENSE.txt)

[English](README_EN.md) | 简体中文

> **重要提示**: `master` 分支在开发过程中可能处于 *不稳定或者不可用状态* 。
请通过[releases](https://github.com/TencentBlueKing/bk-codecc/releases) 而非 `master` 去获取稳定的二进制文件。

代码检查，一般又称为静态代码分析，是指无需运行被测代码，仅通过分析或检查源程序的语法、结构、过程、接口等来检查程序的正确性，找出代码隐藏的错误和缺陷，如内存泄漏，空指针引用，死代码，变量未初始化，复制粘贴错误，重复代码，函数复杂度过高等等。

CodeCC（蓝鲸代码检查中心）构建开放的代码检查平台，提供专业的代码检查解决方案及服务，为产品质量保驾护航。

![image](https://user-images.githubusercontent.com/46527215/227879236-b0dc6b63-cd54-466b-a3bf-e65bd2d51589.png)

## Overview
- [架构设计(待补充)](docs/overview/architecture.md)
- [代码目录（待补充）](docs/overview/code_framework.md)
- [设计理念（待补充）](docs/overview/design.md)

## Features
- 多种检查维度：目前已集成十余款含开源、自研的代码检查工具，覆盖代码缺陷、安全漏洞、编码规范、圈复杂度、代码重复率多种维度；
- 丰富的平台功能：通过快速准确地分析源代码，找出质量问题和安全漏洞，并提供自助接入、实时扫描、告警展示、告警屏蔽、定时日报、修复激励等功能；
- 工具自助上架：支持工具开发框架和自助上架。

## Experience
- [bk-codecc in docker（待补充）](https://hub.docker.com/r/blueking/bk-codecc)

## Getting started
- [下载与编译（待补充）](docs/overview/source_compile.md)
- [一分钟安装部署（待补充）](docs/overview/installation.md)

## Support
1. [GitHub讨论区](https://github.com/TencentBlueKing/bk-codecc/discussions)
2. QQ群：495299374

## BlueKing Community
- [BK-CI](https://github.com/Tencent/bk-ci)：蓝盾是一个免费并开源的CI服务，可助你自动化构建-测试-发布工作流，持续、快速、高质量地交付你的产品。
- [BK-BCS](https://github.com/Tencent/bk-bcs)：蓝鲸容器管理平台是以容器技术为基础，为微服务业务提供编排管理的基础服务平台。
- [BK-CMDB](https://github.com/Tencent/bk-cmdb)：蓝鲸配置平台（蓝鲸CMDB）是一个面向资产及应用的企业级配置管理平台。
- [BK-JOB](https://github.com/Tencent/bk-job)：蓝鲸作业平台(Job)是一套运维脚本管理系统，具备海量任务并发处理能力。
- [BK-PaaS](https://github.com/Tencent/bk-PaaS)：蓝鲸PaaS平台是一个开放式的开发平台，让开发者可以方便快捷地创建、开发、部署和管理SaaS应用。
- [BK-SOPS](https://github.com/Tencent/bk-sops)：蓝鲸标准运维（SOPS）是通过可视化的图形界面进行任务流程编排和执行的系统，是蓝鲸体系中一款轻量级的调度编排类SaaS产品。

## Contributing
- 关于 bk-codecc 分支管理、issue 以及 pr 规范，请阅读 [Contributing](CONTRIBUTING.md)
- [腾讯开源激励计划](https://opensource.tencent.com/contribution) 鼓励开发者的参与和贡献，期待你的加入


## License
BK-CODECC是基于 MIT 协议， 详细请参考 [LICENSE](LICENSE.txt)

我们承诺未来不会更改适用于交付给任何人的当前项目版本的开源许可证（MIT 协议）。