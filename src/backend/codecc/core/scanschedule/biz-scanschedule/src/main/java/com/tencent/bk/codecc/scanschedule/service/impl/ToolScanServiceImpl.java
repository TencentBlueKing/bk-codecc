package com.tencent.bk.codecc.scanschedule.service.impl;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.scanschedule.constants.ScanScheduleConstants;
import com.tencent.bk.codecc.scanschedule.dao.mdb.SimpleDefectRepository;
import com.tencent.bk.codecc.scanschedule.dao.mdb.ScanRecordRepository;
import com.tencent.bk.codecc.scanschedule.dao.mdb.ToolRecordRepository;
import com.tencent.bk.codecc.scanschedule.handler.BinaryToolHandler;
import com.tencent.bk.codecc.scanschedule.handler.InputFileHandler;
import com.tencent.bk.codecc.scanschedule.handler.OutputFileHandler;
import com.tencent.bk.codecc.scanschedule.model.SimpleDefectEntity;
import com.tencent.bk.codecc.scanschedule.model.ScanRecordEntity;
import com.tencent.bk.codecc.scanschedule.model.ToolRecordEntity;
import com.tencent.bk.codecc.scanschedule.pojo.input.InputVO;
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord;
import com.tencent.bk.codecc.scanschedule.pojo.record.ToolRecord;
import com.tencent.bk.codecc.scanschedule.service.ToolScanService;
import com.tencent.bk.codecc.scanschedule.vo.ContentVO;
import com.tencent.bk.codecc.scanschedule.vo.SimpleCheckerSetVO;
import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import com.tencent.bk.codecc.task.api.ServiceToolMetaRestResource;
import com.tencent.bk.sdk.iam.util.JsonUtil;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.FileUtils;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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
    private Client client;
    @Autowired
    private ScanRecordRepository scanRecordRepository;
    @Autowired
    private SimpleDefectRepository defectRecordRepository;
    @Autowired
    private ToolRecordRepository toolRecordRepository;
    @Autowired
    private OutputFileHandler outputFileHandler;
    @Autowired
    private InputFileHandler inputFileHandler;
    @Autowired
    private BinaryToolHandler binaryToolHandler;
    @Value("${codecc.file.data.path:/data/bkee/codecc/nfs}")
    private String codeccFileDataPath = "";

    @Override
    public ScanRecord generateScanRecord(String appCode, ContentVO contentVO) {
        ScanRecord scanRecord = new ScanRecord();
        //生成本次扫描唯一标识
        String scanId = "s-" + UUID.randomUUID().toString().replace("-", "");
        log.info("start record: scanId {}, appCode {}, userId {}, checkerSets {}, content {}",
                scanId, appCode, contentVO.getUserId(),
                contentVO.getCheckerSets(), contentVO.getContent());
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
        List<SimpleDefectVO> defectRecordList = Lists.newArrayList();
        List<ToolRecord> toolRecordList = Lists.newArrayList();
        String projectPath = String.format("%s/workspace/%s", codeccFileDataPath, scanRecord.getUserName());
        for (ToolRecord toolRecord: scanRecord.getToolRecordList()) {
            String inputPath = String.format("%s/.codecc/%s", projectPath, toolRecord.getToolName() + "_input.json");
            String outputPath = String.format("%s/.codecc/%s", projectPath, toolRecord.getToolName() + "_output.json");
            if (Paths.get(outputPath).toFile().exists()) {
                Paths.get(outputPath).toFile().delete();
            }
            if (Paths.get(inputPath).toFile().isFile()) {
                Pair<ToolRecord, List<SimpleDefectVO>> toolResult = toolScan(scanRecord.getUserName(),
                        toolRecord, inputPath, outputPath);
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

    private Pair<ToolRecord, List<SimpleDefectVO>> toolScan(String userId, ToolRecord toolRecord,
                                                            String inputFile, String outputFile) {
        try {
            binaryToolHandler.execCommand(inputFile, outputFile);
        } catch (Exception e) {
            String failMsg = String.format("scanId %s Run %s %s binary failed",
                    toolRecord.getScanId(), toolRecord.getToolName(), toolRecord.getToolVersion());
            log.info(failMsg, e);
            toolRecord.setStatus(1);
            toolRecord.setFailMsg(failMsg);
            return new Pair<>(toolRecord, Lists.newArrayList());
        }

        //获取告警列表
        List<SimpleDefectVO> defectList = new ArrayList<>();
        List<SimpleDefectVO> singleToolDefectList = outputFileHandler.getDefect(userId, toolRecord, outputFile);
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

    /**
     * 根据规则集名称获取规则集列表信息
     * @param scanRecord
     * @return
     */
    public List<CheckerSetVO> queryCheckerDetailForSet(ScanRecord scanRecord) {
        List<CheckerSetVO> checkerSetVOList;
        List<SimpleCheckerSetVO> checkerSets = new ArrayList<>();
        if (Objects.nonNull(scanRecord.getCheckerSets())) {
            checkerSets = scanRecord.getCheckerSets();
        }
        String latestTag = "latest";
        if (checkerSets.isEmpty()) {
            SimpleCheckerSetVO defaultCheckerSet = new SimpleCheckerSetVO();
            defaultCheckerSet.setCheckerSet(ScanScheduleConstants.SCAN_SCHEDULE_DEFAULT_CHECKER_SET);
            defaultCheckerSet.setCheckerSetVersion(latestTag);
            checkerSets.add(defaultCheckerSet);
        }

        Result<List<CheckerSetVO>> result = client.get(ServiceCheckerSetRestResource.class)
                .getCheckerSetsForContent(
                        checkerSets.stream().map(SimpleCheckerSetVO::getCheckerSet).collect(Collectors.toList()));

        if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())) {
            String ms = String.format("select checker set %s data is null", checkerSets);
            log.error(ms);
            throw new CodeCCException(ms);
        }

        checkerSetVOList = result.getData();

        return checkerSetVOList;
    }

    public List<ToolMetaDetailVO> queryToolDetailForSet(List<CheckerSetVO> checkerSetVOList) {
        List<ToolMetaDetailVO> toolMetaDetailVOList = new ArrayList<>();
        Set<String> toolNameList = new HashSet<>();
        for (CheckerSetVO checkerSetVO: checkerSetVOList) {
            toolNameList.addAll(checkerSetVO.getToolList());
        }
        Result<List<ToolMetaDetailVO>> result = client.get(ServiceToolMetaRestResource.class)
                .queryToolMetaDataByToolName(toolNameList.stream().collect(Collectors.toList()));
        if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())) {
            String ms = String.format("select tool meta %s data is null", toolNameList);
            log.error(ms);
            throw new CodeCCException(ms);
        }
        toolMetaDetailVOList = result.getData();

        return toolMetaDetailVOList;
    }

    @Override
    public ScanRecord initScan(ScanRecord scanRecord) {
        //获取规则集列表
        List<CheckerSetVO> checkerSetVOList = queryCheckerDetailForSet(scanRecord);
        //获取工具列表
        List<ToolMetaDetailVO> toolMetaDetailVOList = queryToolDetailForSet(checkerSetVOList);

        //保存文本内容，下载工具, 生成Input文件
        try {
            return inputFileHandler.createContentAndInputFile(scanRecord, toolMetaDetailVOList, checkerSetVOList);
        } catch (Exception e) {
            String failMsg = String.format("scanId %s init scan failed:", scanRecord.getScanId());
            log.error(failMsg, e);
            scanRecord.setStatus(1);
            scanRecord.setFailMsg(failMsg);
            return scanRecord;
        }
    }
}
