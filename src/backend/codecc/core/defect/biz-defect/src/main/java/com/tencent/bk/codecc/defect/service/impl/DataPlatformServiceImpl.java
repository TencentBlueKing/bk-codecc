package com.tencent.bk.codecc.defect.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CommonStatisticDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import com.tencent.bk.codecc.defect.service.DataPlatformService;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.codecc.util.JsonUtil;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class DataPlatformServiceImpl implements DataPlatformService {

    @Autowired
    protected CommonStatisticDao commonStatisticDao;

    @Autowired
    protected LintDefectDao lintDefectDao;

    @Autowired
    protected CCNDefectDao ccnDefectDao;

    @Autowired
    protected DUPCDefectDao dupcDefectDao;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private CLOCDefectRepository clocDefectRepository;

    @Async("asyncTaskExecutor")
    @Override
    public void pushStatistic(List<Long> list) {
        log.info("start push statistic list size: " + list.size());
        list.forEach(s -> {
            Set<String> stringSet = new HashSet<>();
            stringSet.add("COVERITY");
            stringSet.add("KLOCWORK");
            stringSet.add("PINPOINT");
            List<CommonStatisticEntity> commonStatisticEntity = commonStatisticDao.findFirstByTaskIdOrderByStartTime(s, stringSet);
            commonStatisticEntity.forEach(commonStatisticEntity1 -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(commonStatisticEntity1);
//                pushToRabbitMq(map, KafkaTopic.STATISTIC_TOPIC);
            });

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        log.info("end push statistic list size: " + list.size());
    }

    @Async("asyncTaskExecutor")
    @Override
    public void pushLineStatistic(List<Long> list) {
        log.info("start push lint statistic list size: " + list.size());
        list.forEach(s -> {
            Set<String> stringSet = new HashSet<>();
            stringSet.add("SENSITIVE");
            stringSet.add("CPPLINT");
            stringSet.add("CHECKSTYLE");
            stringSet.add("ESLINT");
            stringSet.add("STYLECOP");
            stringSet.add("GOML");
            stringSet.add("DETEKT");
            stringSet.add("PHPCS");
            stringSet.add("PYLINT");
            stringSet.add("OCCHECK");
            List<LintStatisticEntity> lintStatisticEntities = lintDefectDao.findFirstByTaskIdOrderByStartTime(s, stringSet);
            lintStatisticEntities.forEach(lintStatisticEntity -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(lintStatisticEntity);
//                pushToRabbitMq(map, KafkaTopic.LINT_STATISTIC_TOPIC);
            });

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        log.info("end push lint statistic list size: " + list.size());
    }

    @Async("asyncTaskExecutor")
    @Override
    public void pushCcnStatistic(List<Long> list) {
        log.info("start push ccn statistic list size: " + list.size());
        list.forEach(s -> {
            Set<String> stringSet = new HashSet<>();
            stringSet.add("CCN");
            List<CCNStatisticEntity> ccnStatisticEntities = ccnDefectDao.findFirstByTaskIdOrderByTime(s, stringSet);
            ccnStatisticEntities.forEach(ccnStatisticEntity -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(ccnStatisticEntity);
//                pushToRabbitMq(map, KafkaTopic.CNN_STATISTIC_TOPIC);
            });

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        log.info("end push ccn statistic list size: " + list.size());
    }

    @Async("asyncTaskExecutor")
    @Override
    public void pushDupcStatistic(List<Long> list) {
        log.info("start push dupc statistic list size: " + list.size());
        list.forEach(s -> {
            Set<String> stringSet = new HashSet<>();
            stringSet.add("DUPC");
            List<DUPCStatisticEntity> dupcStatisticEntityList = dupcDefectDao.findFirstByTaskIdOrderByTime(s, stringSet);
            dupcStatisticEntityList.forEach(dupcStatisticEntity -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(dupcStatisticEntity);
//                pushToRabbitMq(map, KafkaTopic.DUPC_STATISTIC_TOPIC);
            });

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        log.info("end push dupc statistic list size: " + list.size());
    }

    @Async("asyncTaskExecutor")
    @Override
    public void pushClocDefect(List<Long> list) {
        log.info("start push cloc defect list size: " + list.size());
        list.forEach(s -> {
            List<Map<String, Object>> clocDefectEntityList = new ArrayList<>();
            List<CLOCDefectEntity> clocDefectEntities =
                    clocDefectRepository.findByTaskIdAndToolNameIn(
                            s, Arrays.asList(Tool.SCC.name(), Tool.CLOC.name(), null));
            clocDefectEntities.forEach(clocDefectEntity -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(clocDefectEntity);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateString = sdf.format(new Date());
                map.put("washTime", dateString);

                clocDefectEntityList.add(map);
            });

            if (clocDefectEntityList.size() > 0) {
//                try {
//                    kafkaClient.send("tendata-bkdevops-296-topic-cloc-defect", objectMapper.writeValueAsString(clocDefectEntityList));
//                } catch (JsonProcessingException e) {
//                    e.printStackTrace();
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        log.info("end push cloc defect list size: " + list.size());
    }

    private void pushToRabbitMq(Map<String, Object> map, String topic) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = sdf.format(new Date());
        map.put("washTime", dateString);
//        try {
/*            rabbitTemplate.convertAndSend(
                    EXCHANGE_KAFKA_DATA_PLATFORM,
                    router,
                    objectMapper.writeValueAsString(map)
            );*/

//            kafkaClient.send(topic, objectMapper.writeValueAsString(map));
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
    }
}
