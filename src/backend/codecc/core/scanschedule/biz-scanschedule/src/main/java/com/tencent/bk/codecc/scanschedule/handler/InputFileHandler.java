package com.tencent.bk.codecc.scanschedule.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.scanschedule.constants.ScanScheduleConstants;
import com.tencent.bk.codecc.scanschedule.pojo.input.CheckerOptions;
import com.tencent.bk.codecc.scanschedule.pojo.input.InputVO;
import com.tencent.bk.codecc.scanschedule.pojo.input.OpenCheckers;
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord;
import com.tencent.bk.codecc.scanschedule.pojo.record.ToolRecord;
import com.tencent.bk.codecc.scanschedule.utils.EnvUtils;
import com.tencent.bk.sdk.iam.util.JsonUtil;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.enums.OSType;
import com.tencent.devops.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InputFileHandler {

    @Value("${codecc.file.data.path:/data/bkee/codecc/nfs}")
    private String codeccFileDataPath = "";

    private InputVO generateInputVo(ToolMetaDetailVO.Binary binary, InputVO inputVo, String toolName) {
        if (Objects.isNull(inputVo)) {
            inputVo = new InputVO();
        }
        //初始化二进制信息
        if (Objects.nonNull(binary)) {
            if (OSType.WINDOWS.equals(EnvUtils.getOS())) {
                inputVo.setWinBinPath(binaryDownload(binary.getWinUrl(),
                        toolName, binary.getBinaryVersion()));
                inputVo.setWinCommand(binary.getWinCommand());
            } else {
                if (OSType.LINUX.equals(EnvUtils.getOS())) {
                    inputVo.setLinuxBinPath(binaryDownload(binary.getLinuxUrl(),
                            toolName, binary.getBinaryVersion()));
                    inputVo.setLinuxCommand(binary.getLinuxCommand());
                } else {
                    if (OSType.MAC_OS.equals(EnvUtils.getOS())) {
                        inputVo.setMacBinPath(binaryDownload(binary.getMacUrl(),
                                toolName, binary.getBinaryVersion()));
                        inputVo.setMacCommand(binary.getMacCommand());
                    }
                }
            }
        }

        return inputVo;
    }

    /**
     * 创建 input File 文件
     * @param scanRecord
     * @param toolMetaDetailVOList
     * @throws Exception
     */
    public ScanRecord createContentAndInputFile(ScanRecord scanRecord, List<ToolMetaDetailVO> toolMetaDetailVOList,
                                List<CheckerSetVO> checkerSetVOList) {
        String projectPath = String.format("%s/workspace/%s", codeccFileDataPath, scanRecord.getUserName());
        //保存文本内容
        String scanFile = projectPath + File.separator + ScanScheduleConstants.SCAN_SCHEDULE_FILE_NAME;
        if (!FileUtils.saveContentToFile(scanFile, scanRecord.getContent())) {
            String failMsg = String.format("scanId %s save content into file failed:", scanRecord.getScanId());
            log.info(failMsg);
            scanRecord.setStatus(1);
            scanRecord.setFailMsg(failMsg);
            return scanRecord;
        }
        List<ToolRecord> toolRecordList = new ArrayList<>();
        for (ToolMetaDetailVO toolInfo : toolMetaDetailVOList) {
            ToolMetaDetailVO.Binary binary = toolInfo.getBinary();
            if (Objects.isNull(binary)) {
                continue;
            }
            ToolRecord toolRecord = createToolRecord(scanRecord.getScanId(), toolInfo.getName(),
                    binary.getBinaryVersion());
            //定义input路径
            String inputPath = String.format("%s/.codecc/%s", projectPath, toolInfo.getName() + "_input.json");
            log.info("scanId {}, generate Input.json file: {}", toolRecord.getScanId(), inputPath);
            InputVO inputVo = new InputVO();
            File inputFile = new File(inputPath);
            if (inputFile.isFile()) {
                String data = null;
                try {
                    data = new String(Files.readAllBytes(Paths.get(inputFile.getAbsolutePath())),
                            StandardCharsets.UTF_8);

                    inputVo = JsonUtil.fromJson(data, InputVO.class);
                } catch (IOException e) {
                    String failMsg = String.format("scanId %s read inputFile %s failed:",
                            scanRecord.getScanId(), inputFile.getAbsolutePath());
                    log.error(failMsg, e);
                    toolRecord.setStatus(1);
                    toolRecord.setFailMsg(failMsg);
                    toolRecordList.add(toolRecord);
                    scanRecord.setToolRecordList(toolRecordList);
                    continue;
                }
            }
            inputVo = generateInputVo(binary, inputVo, toolInfo.getName());
            Set<OpenCheckers> toolOpenCheckers = new HashSet<>();
            for (CheckerSetVO checkerSetVO : checkerSetVOList) {
                if (checkerSetVO.getToolList().contains(toolInfo.getName())) {
                    //从规则集中获取规则列表并保存到input.json文件中
                    Set<OpenCheckers> openCheckersSet = checkerSetVO.getCheckerProps().stream()
                            .filter(it -> it.getToolName().equals(toolInfo.getName()))
                            .map(it -> {
                                OpenCheckers openCheckers = new OpenCheckers();
                                openCheckers.setCheckerName(it.getCheckerKey());
                                //如果规则集的规则存在属性值,获取规则属性值,否则通过默认规则详情中获取
                                String props = "";
                                if (Objects.nonNull(it.getProps()) && it.getProps() != "") {
                                    props = it.getProps();
                                }
                                //解析规则属性值成Option对象
                                if (!Strings.isNullOrEmpty(props)) {
                                    List<CheckerOptions> checkerOptionsList = new ArrayList<>();
                                    List<HashMap> propsList = null;
                                    try {
                                        propsList = JsonUtil.fromJson(props, ArrayList.class);
                                    } catch (IOException e) {
                                        log.error("checker {} parse checker params failed: ", props, e);
                                    }
                                    propsList.forEach(propMap -> {
                                        CheckerOptions checkerOptions = new CheckerOptions();
                                        checkerOptions.setCheckerOptionName((String) propMap.get("propName"));
                                        checkerOptions.setCheckerOptionValue((String) propMap.get("propValue"));
                                        checkerOptionsList.add(checkerOptions);
                                    });
                                    if (!checkerOptionsList.isEmpty()) {
                                        openCheckers.setCheckerOptions(checkerOptionsList);
                                    }
                                }
                                return openCheckers;
                            }).collect(Collectors.toSet());
                    toolOpenCheckers.addAll(openCheckersSet);
                }
            }
            inputVo.setOpenCheckers(toolOpenCheckers.stream().collect(Collectors.toList()));
            inputVo.setScanPath(projectPath);
            inputVo.setIncrementalFiles(Lists.newArrayList(scanFile));

            try {
                FileUtils.saveContentToFile(inputPath, JsonUtil.toJson(inputVo));
            } catch (JsonProcessingException e) {
                String failMsg = String.format("scanId %s create %s input file failed:",
                        scanRecord.getScanId(), toolInfo.getName());
                log.error(failMsg, e);
                toolRecord.setStatus(1);
                toolRecord.setFailMsg(failMsg);
                toolRecordList.add(toolRecord);
                scanRecord.setToolRecordList(toolRecordList);
                continue;
            }

            toolRecordList.add(toolRecord);
        }
        scanRecord.setToolRecordList(toolRecordList);
        return scanRecord;
    }

    /**
     * 下载工具
     *
     * @param toolUrl
     * @param toolName
     * @param urlVersion
     * @return
     */
    private String binaryDownload(String toolUrl, String toolName, String urlVersion) {
        String toolPath = "";
        String toolInstallPath = String.format("%s/tools", codeccFileDataPath);
        Path path = Paths.get(String.format("%s/%s/%s", toolInstallPath, toolName, urlVersion));
        if (path.toFile().exists()) {
            toolPath = path.toAbsolutePath().toString();
            log.info("tool binary path {} is exist already, don't download it", toolPath);
        } else {
            if (!Strings.isNullOrEmpty(toolUrl)) {
                //创建二进制目录
                log.info("create tool binary path: {}", path.toAbsolutePath().toString());
                path.toFile().mkdirs();
                //下载二进制
                log.info("download tool binary path: {}", toolUrl);
                String fileName = StringUtils.substringAfterLast(toolUrl, "/");
                String localToolFile = path.toAbsolutePath().toString() + File.separator + fileName;
                try (InputStream in = new URL(toolUrl).openStream()) {
                    Files.copy(in, Paths.get(localToolFile), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    if (path.toFile().exists()) {
                        path.toFile().delete();
                    }
                    log.error("download tool binary path {} failed!",  path.toAbsolutePath().toString(), e);
                    return "";
                }
                log.info("download tool binary path success: {}", path.toAbsolutePath().toString());

                //解压二进制
                log.info("unzip tool binary path: {}", localToolFile);
                if (!FileUtils.unzipFile(localToolFile, path.toAbsolutePath().toString())) {
                    log.error("unzip tool binary path {} failed", localToolFile);
                    return "";
                }
                toolPath = path.toAbsolutePath().toString();
                log.info("unzip tool binary path success: {}", localToolFile);

                //修改文件权限
                log.info("chmod tool binary path: {}", path.toAbsolutePath().toString());
                if (!FileUtils.chmodPath(path.toAbsolutePath().toString(), true, true, true)) {
                    log.error("chmod tool binary path {} failed", path.toAbsolutePath().toString());
                    return "";
                }
                log.info("chmod tool binary path success: {}", path.toAbsolutePath().toString());

                if (Paths.get(localToolFile).toFile().isFile()) {
                    Paths.get(localToolFile).toFile().delete();
                }
            } else {
                log.error("tool binary {} url is empty, please check it.", toolName);
                return "";
            }
        }
        return toolPath;
    }

    private ToolRecord createToolRecord(String scanId, String tool, String toolVersion) {
        ToolRecord toolRecord = new ToolRecord();
        toolRecord.setScanId(scanId);
        toolRecord.setToolName(tool);
        toolRecord.setToolVersion(toolVersion);
        toolRecord.setStartTime(new Date().getTime());
        return toolRecord;
    }
}
