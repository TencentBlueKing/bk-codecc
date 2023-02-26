package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.dao.mongorepository.ToolConfigRepository;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.ToolConfigService;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ToolConfigServiceImpl implements ToolConfigService {

    @Autowired
    private ToolConfigRepository toolConfigRepository;

    @Override
    public List<ToolConfigInfoVO> getToolConfigByTaskId(long taskId) {
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = toolConfigRepository.findByTaskId(taskId);
        if (toolConfigInfoEntityList.isEmpty()) {
            return new ArrayList<>();
        }

        List<ToolConfigInfoVO> toolConfigInfoVOList = new ArrayList<>();
        toolConfigInfoEntityList.forEach(it -> {
            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
            BeanUtils.copyProperties(it, toolConfigInfoVO);
            toolConfigInfoVOList.add(toolConfigInfoVO);
        });

        return toolConfigInfoVOList;
    }
}
