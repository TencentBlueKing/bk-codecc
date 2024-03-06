package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo;

import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ArchivingFileModel<T> {

    private ColdDataArchivingType type;

    private List<T> defectList;
}