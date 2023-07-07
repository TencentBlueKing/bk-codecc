package com.tencent.bk.codecc.codeccjob.service.impl;

import com.tencent.bk.codecc.codeccjob.service.AfterFilterPathDoneOpsService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AfterFilterPathDoneOpsServiceImpl implements AfterFilterPathDoneOpsService {

    @Override
    public void doAfterFilterPathDone(long taskId, String toolName, List defectList) {
        // do noting
    }
}
