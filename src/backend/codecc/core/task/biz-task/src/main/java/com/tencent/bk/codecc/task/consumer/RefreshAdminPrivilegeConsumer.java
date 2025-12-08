package com.tencent.bk.codecc.task.consumer;

import com.tencent.bk.codecc.task.service.AdminPrivilegeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class RefreshAdminPrivilegeConsumer {

    @Autowired
    private AdminPrivilegeService adminPrivilegeService;

    /**
     * 刷新管理员权限状态
     * @param reservedParam 预留参数（没有会导致接收不到MQ的消息）
     */
    public void consumer(String reservedParam) {
        log.info("RefreshAdminPrivilegeStatus start");
        try {
            adminPrivilegeService.batchUpdateAdminPrivilegeStatus();
        } catch (Throwable e) {
            log.error("RefreshAdminPrivilegeStatus fail!", e);
        }
    }
}
