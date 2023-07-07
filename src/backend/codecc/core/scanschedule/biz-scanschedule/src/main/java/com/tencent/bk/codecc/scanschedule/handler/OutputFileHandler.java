/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.scanschedule.handler;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.scanschedule.pojo.output.OutputVO;
import com.tencent.bk.codecc.scanschedule.pojo.record.ToolRecord;
import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import com.tencent.bk.sdk.iam.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理代码检查输出文件
 *
 * @version V1.0
 * @date 2021/11/9
 */
@Component
@Slf4j
public class OutputFileHandler {

    private static final List<SimpleDefectVO> NO_PROBLEMS_FOUND = Collections.emptyList();

    /**
     * 解析Output.json获取告警信息
     * @param userId
     * @param toolRecord
     * @param outputPath
     * @return
     */
    public List<SimpleDefectVO> getDefect(String userId, ToolRecord toolRecord,
                                          String outputPath) {
        try {
            List<SimpleDefectVO> defectList = Lists.newArrayList();
            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                log.info("{} 解析output.json {}", toolRecord.getToolName(), outputPath);
                String outData = new String(Files.readAllBytes(Paths.get(outputPath)), StandardCharsets.UTF_8);
                OutputVO outputVo = JsonUtil.fromJson(outData, OutputVO.class);
                List<SimpleDefectVO> singleToolDefectList = outputVo.getDefects().stream()
                        .map(defect -> {
                            defect.setScanId(toolRecord.getScanId());
                            defect.setAuthor(userId);
                            defect.setToolName(toolRecord.getToolName());
                            defect.setCreateTime(new Date().getTime());
                            return defect;
                        })
                        .collect(Collectors.toList());

                defectList.addAll(singleToolDefectList);
            } else {
                return defectList;
            }
            return defectList;
        } catch (AssertionError e) {
            return NO_PROBLEMS_FOUND;
        }  catch (Throwable e) {
            log.error("{} 解析output.json异常: {}", toolRecord.getToolName(), e);
            return NO_PROBLEMS_FOUND;
        }
    }

    public void execCommand(String workspace, List<String> cmdList, String[] env) throws Exception {
        if (!Strings.isNullOrEmpty(workspace) && !cmdList.isEmpty()) {
            Process process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]),
                    env, Paths.get(workspace).toFile());
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.toLowerCase();
                    log.info(line);
                }
            } catch (Exception e) {
                throw e;
            }
            process.destroy();
        }
    }
}
