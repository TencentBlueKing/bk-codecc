package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.TaskProjectCountVO;

import java.util.List;
import java.util.Set;

public interface CommonTaskDao {

    List<Long> getTaskIdList(Long lastTaskId, Integer limit, String filterProjectId);

    List<TaskProjectCountVO> getProjectCount(Set<Long> taskIds);

    List<TaskInfoEntity> getStopTask(Set<Long> taskIds, Long startTime, Long endTime);

    List<TaskInfoEntity> getTaskByTaskIds(Set<Long> taskIds);
}
