package com.tencent.devops.common.auth.api.pojo

enum class UserGroupRole {
    // 管理员(权限大)
    MANAGER,
    DEVELOPER,
    MAINTAINER,
    TESTER,
    PM,
    QC,
    // CI管理员
    CI_MANAGER
}
