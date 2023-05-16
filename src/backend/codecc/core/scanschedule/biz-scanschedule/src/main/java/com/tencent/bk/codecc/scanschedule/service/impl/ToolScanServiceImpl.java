package com.tencent.bk.codecc.scanschedule.service.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.tencent.bk.codecc.scanschedule.constants.ScanScheduleConstants;
import com.tencent.bk.codecc.scanschedule.dao.mdb.SimpleDefectRepository;
import com.tencent.bk.codecc.scanschedule.dao.mdb.ScanRecordRepository;
import com.tencent.bk.codecc.scanschedule.dao.mdb.ToolRecordRepository;
import com.tencent.bk.codecc.scanschedule.handler.OutputFileHandler;
import com.tencent.bk.codecc.scanschedule.model.SimpleDefectEntity;
import com.tencent.bk.codecc.scanschedule.model.ScanRecordEntity;
import com.tencent.bk.codecc.scanschedule.model.ToolRecordEntity;
import com.tencent.bk.codecc.scanschedule.pojo.input.InputVO;
import com.tencent.bk.codecc.scanschedule.pojo.input.OpenCheckers;
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord;
import com.tencent.bk.codecc.scanschedule.pojo.record.ToolRecord;
import com.tencent.bk.codecc.scanschedule.service.ToolScanService;
import com.tencent.bk.codecc.scanschedule.vo.ContentVO;
import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import com.tencent.bk.sdk.iam.util.JsonUtil;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.FileUtils;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ToolScanServiceImpl implements ToolScanService {

    @Autowired
    private ScanRecordRepository scanRecordRepository;
    @Autowired
    private SimpleDefectRepository defectRecordRepository;
    @Autowired
    private ToolRecordRepository toolRecordRepository;
    @Autowired
    private OutputFileHandler outputFileHandler;
    @Value("${codecc.file.data.path:/data/bkee/codecc/nfs}")
    private String codeccFileDataPath = "";

    @Override
    public ScanRecord generateScanRecord(String appCode, ContentVO contentVO) {
        ScanRecord scanRecord = new ScanRecord();
        //生成本次扫描唯一标识
        String scanId = "s-" + UUID.randomUUID().toString().replace("-", "");
        scanRecord.setScanId(scanId);
        scanRecord.setStartTime(new Date().getTime());
        scanRecord.setAppCode(appCode);
        scanRecord.setUserName(contentVO.getUserId());
        scanRecord.setCheckerSets(contentVO.getCheckerSets());
        scanRecord.setContent(contentVO.getContent());
        return scanRecord;
    }

    @Override
    public ScanRecord scan(ScanRecord scanRecord) {
        List<String> toolList = Lists.newArrayList();
        toolList.add(ScanScheduleConstants.TOOL_NAME);
        String projectPath = String.format(codeccFileDataPath + "/workspace/%s", scanRecord.getUserName());
        String scanFile = projectPath + File.separator + ScanScheduleConstants.SCAN_FILE_NAME;
        List<SimpleDefectVO> defectRecordList = Lists.newArrayList();
        List<ToolRecord> toolRecordList = Lists.newArrayList();
        if (FileUtils.saveContentToFile(scanFile, scanRecord.getContent())) {
            for (String tool : toolList) {
                ToolRecord toolRecord = createToolRecord(scanRecord.getScanId(), tool,
                        ScanScheduleConstants.TOOL_VERSION);
                Pair<ToolRecord, List<SimpleDefectVO>> toolResult = toolScan(scanRecord.getUserName(), toolRecord, projectPath, scanFile);
                toolRecordList.add(toolResult.component1());
                defectRecordList.addAll(toolResult.component2());
            }
        }

        //获取失败状态工具记录列表
        List<ToolRecord> failToolReCordList = toolRecordList
                .stream().filter(it -> it.getStatus() == 1)
                .collect(Collectors.toList());

        if (failToolReCordList.size() > 0) {
            scanRecord.setStatus(1);
            String failMsg = failToolReCordList.stream()
                    .map(ToolRecord::getFailMsg)
                    .collect(Collectors.joining(";"));
            scanRecord.setFailMsg(failMsg);
        } else {
            scanRecord.setStatus(0);
        }
        scanRecord.setDefectCount(defectRecordList.size());
        scanRecord.setToolRecordList(toolRecordList);
        scanRecord.setDefectList(defectRecordList);
        scanRecord.setEndTime(new Date().getTime());
        scanRecord.setElapseTime(scanRecord.getEndTime() - scanRecord.getStartTime());
        return scanRecord;
    }

    private ToolRecord createToolRecord(String scanId, String tool, String toolVersion) {
        ToolRecord toolRecord = new ToolRecord();
        toolRecord.setScanId(scanId);
        toolRecord.setToolName(tool);
        toolRecord.setToolVersion(toolVersion);
        toolRecord.setStartTime(new Date().getTime());
        return toolRecord;
    }

    private Pair<ToolRecord, List<SimpleDefectVO>> toolScan(String userId, ToolRecord toolRecord,
                                                            String projectPath, String scanFile) {

        String inputPath = String.format("%s/.codecc/%s", projectPath, toolRecord.getToolName() + "_input.json");
        log.info("{}: 生成或更新Input.json文件 {}", toolRecord.getToolName(), inputPath);
        InputVO inputVo = null;
        try (InputStreamReader inputStreamReader = new InputStreamReader(
                this.getClass().getResourceAsStream("/" + toolRecord.getToolName() + "_input.json"),
                "UTF-8")
        ) {
            String data = new String(CharStreams.toString(inputStreamReader).getBytes(),
                    StandardCharsets.UTF_8);
            inputVo = JsonUtil.fromJson(data, InputVO.class);
            inputVo.setScanPath(Paths.get(scanFile).getParent().toString());
            inputVo.setIncrementalFiles(Lists.newArrayList(scanFile));
            FileUtils.saveContentToFile(inputPath, JsonUtil.toJson(inputVo));
        } catch (IOException e) {
            String failMsg = "generate input json file failed:";
            log.info(failMsg, e);
            toolRecord.setStatus(1);
            toolRecord.setFailMsg(failMsg);
            return new Pair<>(toolRecord, Lists.newArrayList());
        }

        String outputPath = String.format("%s/.codecc/%s", projectPath, toolRecord.getToolName() + "_output.json");
        if (Paths.get(outputPath).toFile().exists()) {
            Paths.get(outputPath).toFile().delete();
        }

        List<String> cmdList = Lists.newArrayList();
        cmdList.add("/bin/bash");
        cmdList.add("-c");
        cmdList.add(String.format("./CodeSecurityScan --input=%s --output=%s", inputPath, outputPath));
        String[] env = {"PATH=" + System.getenv("PATH")};
        String toolPath = String.format(codeccFileDataPath + "/tools/%s/%s/tool", toolRecord.getToolName(),
                toolRecord.getToolVersion());
        log.info("{}: 扫描生成Output.json文件 {}", toolRecord.getToolName(), outputPath);
        log.info("运行路径：{}", toolPath);
        log.info("运行命令：{}", String.join(" ", cmdList));
        log.info("运行环境变量: {}", String.join(" ", env));
        try {
            log.info("运行开始: {}", toolRecord.getToolName());
            outputFileHandler.execCommand(toolPath, cmdList, env);
            log.info("运行结束: {}", toolRecord.getToolName());
        } catch (Exception e) {
            String failMsg = String.format("Run %s %s binary failed, workspace is %s, command is %s",
                    toolRecord.getToolName(), toolRecord.getToolVersion(),
                    toolPath, String.join(" ", cmdList));
            log.info(failMsg, e);
            toolRecord.setStatus(1);
            toolRecord.setFailMsg(failMsg);
            return new Pair<>(toolRecord, Lists.newArrayList());
        }

        //获取告警列表
        List<SimpleDefectVO> defectList = Lists.newArrayList();
        List<SimpleDefectVO> singleToolDefectList = outputFileHandler
                .getDefect(userId, toolRecord, outputPath, inputVo.getOpenCheckers());
        defectList.addAll(singleToolDefectList);

        toolRecord.setDefectCount(defectList.size());
        toolRecord.setStatus(0);
        toolRecord.setEndTime(new Date().getTime());
        toolRecord.setElapseTime(toolRecord.getEndTime() - toolRecord.getStartTime());
        return new Pair<>(toolRecord, defectList);
    }

    @Override
    public void saveScanRecord(ScanRecord scanRecord) {

        //保存扫描记录
        ScanRecordEntity scanRecordEntity = new ScanRecordEntity();
        BeanUtils.copyProperties(scanRecord, scanRecordEntity);
        //转化规则集类型
        scanRecordEntity.setCheckerSets(
                scanRecord.getCheckerSets().stream()
                        .map(it -> {
                            ScanRecordEntity.SimpleCheckerSet simpleCheckerSet = new ScanRecordEntity.SimpleCheckerSet();
                            BeanUtils.copyProperties(it, simpleCheckerSet);
                            return simpleCheckerSet;
                        }).collect(Collectors.toList())
        );
        //编码保存代码片段
        scanRecordEntity.setContent(CompressionUtils.compressAndEncodeBase64(scanRecord.getContent()));
        scanRecordRepository.save(scanRecordEntity);
        //保存工具记录
        List<ToolRecordEntity> toolRecordEntityList = scanRecord.getToolRecordList().stream()
                .map(it -> {
                    ToolRecordEntity toolRecordEntity = new ToolRecordEntity();
                    BeanUtils.copyProperties(it, toolRecordEntity);
                    return toolRecordEntity;
                })
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toolRecordEntityList)) {
            toolRecordRepository.saveAll(toolRecordEntityList);
        }

        //保存扫描告警记录
        List<SimpleDefectEntity> simpleDefectEntityList = scanRecord.getDefectList().stream()
                .map(it -> {
                    SimpleDefectEntity simpleDefectEntity = new SimpleDefectEntity();
                    BeanUtils.copyProperties(it, simpleDefectEntity);
                    return simpleDefectEntity;
                })
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(simpleDefectEntityList)) {
            defectRecordRepository.saveAll(simpleDefectEntityList);
        }
    }
}
