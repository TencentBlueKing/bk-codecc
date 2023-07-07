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
import com.tencent.bk.codecc.scanschedule.pojo.input.InputVO;
import com.tencent.bk.codecc.scanschedule.utils.EnvUtils;
import com.tencent.bk.sdk.iam.util.JsonUtil;
import com.tencent.devops.common.api.enums.OSType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 二进制工具执行类
 *
 * @version V1.0
 * @date 2022/8/8
 */
@Component
@Slf4j
public class BinaryToolHandler {

    /**
     * 二进制工具执行方法
     *
     * @param inputFile
     * @param outputFile
     */
    public void execCommand(String inputFile, String outputFile) {
        String data = null;
        InputVO inputVo = null;
        try {
            data = new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
            inputVo = JsonUtil.fromJson(data, InputVO.class);
        } catch (IOException e) {
            log.error("{} file ready failed, can't run command to scan", inputFile, e);
            return;
        }

        List<String> cmdList = Lists.newArrayList();
        String toolPath = "";
        if (OSType.WINDOWS.equals(EnvUtils.getOS())
                && Paths.get(inputVo.getWinBinPath()).toFile().exists()) {
            cmdList.add("cmd");
            cmdList.add("/c");
            cmdList.addAll(Arrays.asList(inputVo.getWinCommand().split("##")));
            toolPath = inputVo.getWinBinPath();
        } else if (OSType.LINUX.equals(EnvUtils.getOS())
                && Paths.get(inputVo.getLinuxBinPath()).toFile().exists()) {
            cmdList.add("/bin/bash");
            cmdList.add("-c");
            cmdList.addAll(Arrays.asList(inputVo.getLinuxCommand().split("##")));
            toolPath = inputVo.getLinuxBinPath();
        } else if (OSType.MAC_OS.equals(EnvUtils.getOS())
                && Paths.get(inputVo.getMacBinPath()).toFile().exists()) {
            cmdList.add("/bin/bash");
            cmdList.add("-c");
            cmdList.addAll(Arrays.asList(inputVo.getMacCommand().split("##")));
            toolPath = inputVo.getMacBinPath();
        }
        for (int idx = 0; idx < cmdList.size(); idx++) {
            String cmd = cmdList.get(idx).replace("\"", "");
            if (cmd.contains("{input.json}")) {
                cmd = cmd.replace("{input.json}", "\""
                        + inputFile.replace("\\", "/") + "\"");
                cmdList.set(idx, cmd);
            }
            if (cmd.contains("{output.json}")) {
                cmd = cmd.replace("{output.json}", "\""
                        + outputFile.replace("\\", "/") + "\"");
                cmdList.set(idx, cmd);
            }
        }
        log.info("command Path：{}", toolPath);
        String[] env = {"PATH=" + System.getenv("PATH")};
        log.info("command envirament: {}", String.join(" ", env));
        log.info("start run: {}",String.join(" ", cmdList));
        if (!Strings.isNullOrEmpty(toolPath) && !cmdList.isEmpty()) {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]),
                        env, Paths.get(toolPath).toFile());
            } catch (IOException e) {
                log.error("command run failed: {} ", String.join(" ", cmdList), e);
                process.destroy();
                return;
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.toLowerCase();
                    log.info(line);
                }
            } catch (Exception e) {
                log.error("ready command result failed: {}\n", String.join(" ", cmdList), e);
                return;
            }
            process.destroy();
        }
        log.info("finish run: {}",String.join(" ", cmdList));
    }

}
