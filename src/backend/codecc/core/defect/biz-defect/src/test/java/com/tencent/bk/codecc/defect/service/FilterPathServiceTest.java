package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.impl.CCNFilterPathBizServiceImpl;
import com.tencent.bk.codecc.defect.service.impl.CommonFilterPathBizServiceImpl;
import com.tencent.bk.codecc.defect.service.impl.DUPCFilterPathBizServiceImpl;
import com.tencent.bk.codecc.defect.service.impl.LintFilterPathBizServiceImpl;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import org.jetbrains.kotlin.com.google.common.collect.Sets;
import org.junit.Test;

/**
 * 测试屏蔽路径（黑白名单）逻辑
 * 使用告警文件绝对路径测试
 *
 * @author warmli
 */
public class FilterPathServiceTest {
    LintFilterPathBizServiceImpl lintFilterPathBizService = new LintFilterPathBizServiceImpl();

    CCNFilterPathBizServiceImpl ccnFilterPathBizService = new CCNFilterPathBizServiceImpl();

    DUPCFilterPathBizServiceImpl dupcFilterPathBizService = new DUPCFilterPathBizServiceImpl();

    CommonFilterPathBizServiceImpl commonFilterPathBizService = new CommonFilterPathBizServiceImpl();

    @Test
    public void lintFilterPath() {
        FilterPathInputVO<LintDefectV2Entity> filterPathInputVO = createLintFilterPathInputVO();
        Result<Boolean> res = lintFilterPathBizService.processBiz(filterPathInputVO);
        assert res.getData();
        assert filterPathInputVO.getDefectEntity().getStatus()
                == (ComConstants.DefectStatus.PATH_MASK.value() | ComConstants.DefectStatus.NEW.value());
    }

    @Test
    public void ccnFilterPath() {
        FilterPathInputVO<CCNDefectEntity> filterPathInputVO = createCcnFilterPathInputVO();
        Result<Boolean> res = ccnFilterPathBizService.processBiz(filterPathInputVO);
        assert res.getData();
        assert filterPathInputVO.getDefectEntity().getStatus()
                == (ComConstants.DefectStatus.PATH_MASK.value() | ComConstants.DefectStatus.NEW.value());
    }

    @Test
    public void commonFilterPath() {
        FilterPathInputVO<CommonDefectEntity> filterPathInputVO = createCommonFilterPathInputVO();
        Result<Boolean> res = commonFilterPathBizService.processBiz(filterPathInputVO);
        assert res.getData();
        assert filterPathInputVO.getDefectEntity().getStatus()
                == (ComConstants.DefectStatus.PATH_MASK.value() | ComConstants.DefectStatus.NEW.value());
    }

    @Test
    public void dupcFilterPath() {
        FilterPathInputVO<DUPCDefectEntity> filterPathInputVO = createDupcFilterPathInputVO();
        Result<Boolean> res = dupcFilterPathBizService.processBiz(filterPathInputVO);
        assert res.getData();
        assert filterPathInputVO.getDefectEntity().getStatus()
                == (ComConstants.DefectStatus.PATH_MASK.value() | ComConstants.DefectStatus.NEW.value());
    }

    @Test
    public void sccFilterPath() {

    }

    @SuppressWarnings("unchecked")
    private FilterPathInputVO<LintDefectV2Entity> createLintFilterPathInputVO() {
        LintDefectV2Entity lintDefectV2Entity = new LintDefectV2Entity();
        lintDefectV2Entity.setStatus(ComConstants.DefectStatus.NEW.value());
        lintDefectV2Entity.setFilePath("/cc/ee/rr/wq/ee/www/tt.c");
        FilterPathInputVO<LintDefectV2Entity> filterPathInputVO = new FilterPathInputVO<>();
        filterPathInputVO.setAllFilterPath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setWhitePath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setDefectEntity(lintDefectV2Entity);
        filterPathInputVO.setExcludeTime(System.currentTimeMillis());
        return filterPathInputVO;
    }

    @SuppressWarnings("unchecked")
    private FilterPathInputVO<CommonDefectEntity> createCommonFilterPathInputVO() {
        CommonDefectEntity commonDefectEntity = new CommonDefectEntity();
        commonDefectEntity.setStatus(ComConstants.DefectStatus.NEW.value());
        commonDefectEntity.setFileName("tt.c");
        commonDefectEntity.setFilePath("/cc/ee/rr/wq/ee/www/tt.c");
        FilterPathInputVO<CommonDefectEntity> filterPathInputVO = new FilterPathInputVO<>();
        filterPathInputVO.setAllFilterPath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setWhitePath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setDefectEntity(commonDefectEntity);
        filterPathInputVO.setExcludeTime(System.currentTimeMillis());
        filterPathInputVO.setToolName(ComConstants.Tool.COVERITY.name());
        return filterPathInputVO;
    }

    @SuppressWarnings("unchecked")
    private FilterPathInputVO<DUPCDefectEntity> createDupcFilterPathInputVO() {
        DUPCDefectEntity dupcDefectEntity = new DUPCDefectEntity();
        dupcDefectEntity.setStatus(ComConstants.DefectStatus.NEW.value());
        dupcDefectEntity.setFilePath("/cc/ee/rr/wq/ee/www/tt.c");
        FilterPathInputVO<DUPCDefectEntity> filterPathInputVO = new FilterPathInputVO<>();
        filterPathInputVO.setAllFilterPath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setWhitePath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setDefectEntity(dupcDefectEntity);
        filterPathInputVO.setExcludeTime(System.currentTimeMillis());
        return filterPathInputVO;
    }

    @SuppressWarnings("unchecked")
    private FilterPathInputVO<CCNDefectEntity> createCcnFilterPathInputVO() {
        CCNDefectEntity ccnDefectEntity = new CCNDefectEntity();
        ccnDefectEntity.setStatus(ComConstants.DefectStatus.NEW.value());
        ccnDefectEntity.setFilePath("/cc/ee/rr/wq/ee/www/tt.c");
        FilterPathInputVO<CCNDefectEntity> filterPathInputVO = new FilterPathInputVO<>();
        filterPathInputVO.setAllFilterPath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setWhitePath(Sets.newHashSet(".*/ee/*."));
        filterPathInputVO.setDefectEntity(ccnDefectEntity);
        filterPathInputVO.setExcludeTime(System.currentTimeMillis());
        return filterPathInputVO;
    }
}
