package com.tencent.bk.codecc.scanschedule.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.scanschedule.dao.mdb.SimpleDefectRepository;
import com.tencent.bk.codecc.scanschedule.dao.mdb.ScanRecordRepository;
import com.tencent.bk.codecc.scanschedule.dao.mdb.ToolRecordRepository;
import com.tencent.bk.codecc.scanschedule.handle.BinaryHandler;
import com.tencent.bk.codecc.scanschedule.handle.CheckersHandler;
import com.tencent.bk.codecc.scanschedule.handle.ContentHandler;
import com.tencent.bk.codecc.scanschedule.handle.InputHandler;
import com.tencent.bk.codecc.scanschedule.handle.OutputHandler;
import com.tencent.bk.codecc.scanschedule.handle.ToolMetaHandler;
import com.tencent.bk.codecc.scanschedule.model.SimpleDefectEntity;
import com.tencent.bk.codecc.scanschedule.model.ScanRecordEntity;
import com.tencent.bk.codecc.scanschedule.model.ToolRecordEntity;
import com.tencent.bk.codecc.scanschedule.pojo.input.InputVO;
import com.tencent.bk.codecc.scanschedule.pojo.input.OpenCheckers;
import com.tencent.bk.codecc.scanschedule.pojo.output.OutputVO;
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord;
import com.tencent.bk.codecc.scanschedule.pojo.record.ToolRecord;
import com.tencent.bk.codecc.scanschedule.service.ToolScanService;
import com.tencent.bk.codecc.scanschedule.vo.ContentVO;
import com.tencent.bk.codecc.scanschedule.vo.SimpleCheckerSetVO;
import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.CompressionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 扫描服务实现类
 * @author jimxzcai
 */
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
    private BinaryHandler binaryHandler;
    @Autowired
    private ContentHandler contentHandler;
    @Autowired
    private InputHandler inputHandler;
    @Autowired
    private ToolMetaHandler toolMetaHandler;
    @Autowired
    private CheckersHandler checkersHandler;
    @Autowired
    private OutputHandler outputHandler;

    /**
     * 生成扫描记录
     */
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
        log.info("start scanRecord = [{}]", scanRecord);
        return scanRecord;
    }

    /**
     * 初始化扫描
     */
    @Override
    public ScanRecord initScan(ScanRecord scanRecord) {
        // 0.保存扫描文本内容
        contentHandler.saveContentToFile(scanRecord);
        // 1.获取规则集列表和工具列表
        List<CheckerSetVO> checkerSetVOList = checkersHandler.queryCheckerDetailForSet(scanRecord);
        if (CollectionUtils.isEmpty(checkerSetVOList)) {
            scanRecord.setStatus(1);
            scanRecord.setFailMsg(String.format("scanId %s checkers set is empty", scanRecord.getScanId()));
            return scanRecord;
        }
        List<ToolMetaDetailVO> toolMetaDetailVOList = toolMetaHandler.queryToolDetailForCheckerSet(checkerSetVOList);
        if (CollectionUtils.isEmpty(toolMetaDetailVOList)) {
            scanRecord.setStatus(1);
            scanRecord.setFailMsg(String.format("scanId %s tool info is empty", scanRecord.getScanId()));
            return scanRecord;
        }
        // 2.下载工具和生成输入文件
        try {
            List<ToolRecord> toolRecordList = new ArrayList<>();
            for (ToolMetaDetailVO toolInfo: toolMetaHandler.filterToolMetaByEnableBinary(toolMetaDetailVOList)) {
                // 2.1.生成二进制命令与工具记录对象
                String command = binaryHandler.generateCommand(
                        toolInfo.getBinary(),
                        scanRecord.getScanId(),
                        toolInfo.getName());
                ToolRecord toolRecord = createToolRecord(
                        scanRecord.getScanId(),
                        toolInfo.getName(),
                        toolInfo.getBinary().getBinaryVersion(),
                        command);

                // 2.2.下载工具
                binaryHandler.binaryDownload(toolInfo.getBinary(), toolInfo.getName());

                // 2.3.生成工具对应规则列表
                List<OpenCheckers>  openCheckers =
                        checkersHandler.getCheckersForToolName(
                                toolInfo.getName(),
                                checkerSetVOList);

                // 2.4.生成工具输入对象并保存到文件
                InputVO inputVO =
                        inputHandler.generateInputVo(
                                scanRecord,
                                toolInfo.getName(),
                                openCheckers);
                String inputPath = inputHandler.saveInputVoToFile(scanRecord, inputVO);
                if (StringUtils.isEmpty(inputPath)) {
                    String failMsg = String.format("scanId %s tool %s save inputFile failed:",
                            scanRecord.getScanId(), toolInfo.getName());
                    scanRecord.setStatus(1);
                    toolRecord.setStatus(1);
                    scanRecord.setFailMsg(failMsg);
                    toolRecord.setFailMsg(failMsg);
                }
                toolRecordList.add(toolRecord);
            }
            scanRecord.setToolRecordList(toolRecordList);
        } catch (Exception e) {
            String failMsg = String.format("scanId %s init scan failed:", scanRecord.getScanId());
            log.error(failMsg, e);
            scanRecord.setStatus(1);
            scanRecord.setFailMsg(failMsg);
            return scanRecord;
        }
        return scanRecord;
    }

    /**
     * 二进制扫描
     */
    @Override
    public ScanRecord scan(ScanRecord scanRecord) {
        List<SimpleDefectVO> defectRecordList = Lists.newArrayList();
        if (scanRecord.getStatus() != 1) {
            for (ToolRecord toolRecord: scanRecord.getToolRecordList()) {
                if (toolRecord.getStatus() != 1) {
                    // 1.运行二进制命令
                    binaryHandler.execCommand(
                            toolRecord.getToolCommand(),
                            toolRecord.getToolName(),
                            toolRecord.getToolVersion());

                    // 2.解析输出结果
                    OutputVO outPutVO = outputHandler.readOutputVoFromFile(
                            scanRecord,
                            toolRecord.getToolName());
                    defectRecordList.addAll(
                            outputHandler.explainOutputVoToDefectList(
                                    scanRecord,
                                    toolRecord.getToolName(),
                                    outPutVO)
                    );
                }
            }
        }

        //删除本地内容文本文件
        contentHandler.removeContentFile(scanRecord);

        //保存记录信息
        scanRecord.setDefectCount(defectRecordList.size());
        scanRecord.setDefectList(defectRecordList);
        scanRecord.setEndTime(new Date().getTime());
        scanRecord.setElapseTime(scanRecord.getEndTime() - scanRecord.getStartTime());
        return scanRecord;

    }

    /**
     * 保存扫描记录
     */
    @Override
    public void saveScanRecord(ScanRecord scanRecord) {
        // 1.保存扫描记录
        ScanRecordEntity scanRecordEntity = new ScanRecordEntity();
        BeanUtils.copyProperties(scanRecord, scanRecordEntity);
        scanRecordEntity.setCheckerSets(
                scanRecord.getCheckerSets().stream()
                        .map(this::convertToSimpleCheckerSet)
                        .collect(Collectors.toList())
        );
        scanRecordEntity.setContent(CompressionUtils.compressAndEncodeBase64(scanRecord.getContent()));
        scanRecordRepository.save(scanRecordEntity);

        // 2.保存工具记录
        List<ToolRecordEntity> toolRecordEntityList = scanRecord.getToolRecordList().stream()
                .map(it -> {
                    ToolRecordEntity toolRecordEntity = new ToolRecordEntity();
                    BeanUtils.copyProperties(it, toolRecordEntity);
                    return toolRecordEntity;
                }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toolRecordEntityList)) {
            toolRecordRepository.saveAll(toolRecordEntityList);
        }

        // 3.保存扫描告警记录
        List<SimpleDefectEntity> simpleDefectEntityList = scanRecord.getDefectList().stream()
                .map(it -> {
                    SimpleDefectEntity simpleDefectEntity = new SimpleDefectEntity();
                    BeanUtils.copyProperties(it, simpleDefectEntity);
                    return simpleDefectEntity;
                }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(simpleDefectEntityList)) {
            defectRecordRepository.saveAll(simpleDefectEntityList);
        }
    }

    /**
     * 创建工具扫描记录
     */
    private ToolRecord createToolRecord(
            String scanId, String tool,
            String toolVersion, String command) {

        ToolRecord toolRecord = new ToolRecord();
        toolRecord.setScanId(scanId);
        toolRecord.setToolName(tool);
        toolRecord.setToolVersion(toolVersion);
        toolRecord.setToolCommand(command);
        toolRecord.setStartTime(new Date().getTime());
        return toolRecord;
    }

    /**
     * 转换规则对象
     */
    private ScanRecordEntity.SimpleCheckerSet convertToSimpleCheckerSet(SimpleCheckerSetVO checkerSet) {
        ScanRecordEntity.SimpleCheckerSet simpleCheckerSet = new ScanRecordEntity.SimpleCheckerSet();
        BeanUtils.copyProperties(checkerSet, simpleCheckerSet);
        return simpleCheckerSet;
    }
}
