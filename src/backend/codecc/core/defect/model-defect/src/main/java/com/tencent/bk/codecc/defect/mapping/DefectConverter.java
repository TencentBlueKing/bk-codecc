package com.tencent.bk.codecc.defect.mapping;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.devops.common.util.FileUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 告警实体转换
 */
@Component
public class DefectConverter {

    /**
     * common to lint
     *
     * @param common
     * @return
     */
    public LintDefectV2Entity commonToLint(CommonDefectEntity common) {
        LintDefectV2Entity lint = new LintDefectV2Entity();
        lint.setEntityId(common.getEntityId());
        // author_list == author, lint改类型为List，兼容String
        lint.setAuthor(Lists.newArrayList(Optional.ofNullable(common.getAuthorList()).orElse(Sets.newHashSet())));
        // checker_name == checker
        lint.setChecker(common.getChecker());
        lint.setCreateBuildNumber(common.getCreateBuildNumber());
        lint.setCreateTime(common.getCreateTime());
        lint.setCreatedBy(common.getCreatedBy());
        lint.setCreatedDate(common.getCreatedDate());
        lint.setDefectInstances(common.getDefectInstances());
        // new: display_category
        lint.setDisplayCategory(common.getDisplayCategory());
        // new: display_type
        lint.setDisplayType(common.getDisplayType());
        lint.setExcludeTime(common.getExcludeTime());
        // new: ext_bug_id
        lint.setExtBugId(common.getExtBugId());
        lint.setFileMd5(common.getFileMd5());
        lint.setFileName(common.getFileName());
        // file_path_name == file_path
        lint.setFilePath(common.getFilePath());
        // new: file_version
        lint.setFileVersion(common.getFileVersion());
        lint.setFixedBuildNumber(common.getFixedBuildNumber());
        lint.setFixedTime(common.getFixedTime());
        lint.setId(common.getId());
        lint.setIgnoreAuthor(common.getIgnoreAuthor());
        lint.setIgnoreReason(common.getIgnoreReason());
        lint.setIgnoreReasonType(common.getIgnoreReasonType());
        lint.setIgnoreTime(common.getIgnoreTime());
        // line_number == line_num
        lint.setLineNum(common.getLineNum());
        lint.setMark(common.getMark());
        lint.setMarkTime(common.getMarkTime());
        lint.setMaskPath(common.getMaskPath());
        lint.setMessage(common.getMessage());
        // new: platform_build_id
        lint.setPlatformBuildId(common.getPlatformBuildId());
        // new: platform_project_id
        lint.setPlatformProjectId(common.getPlatformProjectId());
        lint.setRelPath(common.getRelPath());
        lint.setRevision(common.getRevision());
        lint.setSeverity(common.getSeverity());
        lint.setStatus(common.getStatus());
        // new: stream_name, 同task表的name_en
        lint.setStreamName(common.getStreamName());
        lint.setTaskId(common.getTaskId());
        lint.setToolName(common.getToolName());
        lint.setUpdatedBy(common.getUpdatedBy());
        lint.setUpdatedDate(common.getUpdatedDate());

        // 部分老旧数据用displayType存问题描述
        if (StringUtils.isEmpty(lint.getMessage()) && !StringUtils.isEmpty(lint.getDisplayType())) {
            lint.setMessage(lint.getDisplayType());
        }

        // 部分老旧数据是没有fileName字段，用filePath重新构造出fileName
        if (StringUtils.isEmpty(lint.getFileName()) && !StringUtils.isEmpty(lint.getFilePath())) {
            lint.setFileName(FileUtils.getFileNameByPath(lint.getFilePath()));
        }

        lint.setMarkButNoFixed(common.getMarkButNoFixed());
        lint.setLineUpdateTime(Optional.ofNullable(common.getLineUpdateTime()).orElse(0L));

        return lint;
    }

    /**
     * common to lint (list)
     *
     * @param fromList
     * @return
     */
    public List<LintDefectV2Entity> commonToLint(List<CommonDefectEntity> fromList) {
        if (CollectionUtils.isEmpty(fromList)) {
            return Lists.newArrayList();
        }

        return fromList.stream().map(this::commonToLint).collect(Collectors.toList());
    }

    /**
     * lint to common
     *
     * @param lint
     * @return
     */
    public CommonDefectEntity lintToCommon(LintDefectV2Entity lint) {
        CommonDefectEntity common = new CommonDefectEntity();
        common.setEntityId(lint.getEntityId());
        common.setAuthorList(Sets.newHashSet(Optional.ofNullable(lint.getAuthor()).orElse(Lists.newArrayList())));
        common.setChecker(lint.getChecker());
        common.setCreateBuildNumber(lint.getCreateBuildNumber());
        common.setCreateTime(Optional.ofNullable(lint.getCreateTime()).orElse(0L));
        common.setCreatedBy(lint.getCreatedBy());
        common.setCreatedDate(lint.getCreatedDate());
        common.setDefectInstances(lint.getDefectInstances());
        common.setDisplayCategory(lint.getDisplayCategory());
        common.setDisplayType(lint.getDisplayType());
        common.setExcludeTime(Optional.ofNullable(lint.getExcludeTime()).orElse(0L));
        common.setExtBugId(lint.getExtBugId());
        common.setFileMd5(lint.getFileMd5());
        common.setFileName(lint.getFileName());
        common.setFilePath(lint.getFilePath());
        common.setFileVersion(lint.getFileVersion());
        common.setFixedBuildNumber(lint.getFixedBuildNumber());
        common.setFixedTime(Optional.ofNullable(lint.getFixedTime()).orElse(0L));
        common.setId(lint.getId());
        common.setIgnoreAuthor(lint.getIgnoreAuthor());
        common.setIgnoreReason(lint.getIgnoreReason());
        common.setIgnoreReasonType(Optional.ofNullable(lint.getIgnoreReasonType()).orElse(0));
        common.setIgnoreTime(Optional.ofNullable(lint.getIgnoreTime()).orElse(0L));
        common.setLineNum(lint.getLineNum());
        common.setMark(lint.getMark());
        common.setMarkTime(lint.getMarkTime());
        common.setMaskPath(lint.getMaskPath());
        common.setMessage(lint.getMessage());
        common.setPlatformBuildId(lint.getPlatformBuildId());
        common.setPlatformProjectId(lint.getPlatformProjectId());
        common.setRelPath(lint.getRelPath());
        common.setRevision(lint.getRevision());
        common.setSeverity(lint.getSeverity());
        common.setStatus(lint.getStatus());
        common.setStreamName(lint.getStreamName());
        common.setTaskId(lint.getTaskId());
        common.setToolName(lint.getToolName());
        common.setUpdatedBy(lint.getUpdatedBy());
        common.setUpdatedDate(lint.getUpdatedDate());
        common.setMarkButNoFixed(lint.getMarkButNoFixed());
        common.setLineUpdateTime(lint.getLineUpdateTime());

        return common;
    }

    /**
     * lint to common (list)
     *
     * @param fromList
     * @return
     */
    public List<CommonDefectEntity> lintToCommon(List<LintDefectV2Entity> fromList) {
        if (CollectionUtils.isEmpty(fromList)) {
            return Lists.newArrayList();
        }

        return fromList.stream().map(this::lintToCommon).collect(Collectors.toList());
    }
}
