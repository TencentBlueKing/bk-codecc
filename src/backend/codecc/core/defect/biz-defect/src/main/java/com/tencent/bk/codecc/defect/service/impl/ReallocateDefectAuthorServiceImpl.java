package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.service.ReallocateDefectAuthorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReallocateDefectAuthorServiceImpl implements ReallocateDefectAuthorService {

    @Override
    public boolean isReallocate(Long taskId, String toolName) {
        return false;
    }

    @Override
    public void updateCurrentStatus(Long taskId, String toolName) {

    }
}
