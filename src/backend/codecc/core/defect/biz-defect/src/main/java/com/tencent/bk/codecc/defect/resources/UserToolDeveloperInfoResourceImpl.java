package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserToolDeveloperInfoResource;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.ToolDeveloperInfoRepository;
import com.tencent.bk.codecc.defect.model.ToolDeveloperInfoEntity;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

@RestResource
public class UserToolDeveloperInfoResourceImpl implements UserToolDeveloperInfoResource {

    @Autowired
    ToolDeveloperInfoRepository toolDeveloperInfoRepository;

    /**
     * 工具开发者角色类型
     */
    enum RoleType {
        DEVELOPER(1),
        OWNER(2),
        MASTER(4);

        private int value;

        RoleType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public Result<Boolean> addUserAsRole(String userId, String toolName, String userName, Integer role) {
        if (role == null) {
            role = RoleType.DEVELOPER.getValue();
        }

        ToolDeveloperInfoEntity entity = toolDeveloperInfoRepository.findFirstByToolName(toolName);

        if (entity == null) {
            entity = new ToolDeveloperInfoEntity();
            entity.setCreatedBy(userId);
            entity.setCreatedDate(System.currentTimeMillis());
            entity.setToolName(toolName);
        }

        entity.setUpdatedBy(userId);
        entity.setUpdatedDate(System.currentTimeMillis());

        if (role == RoleType.DEVELOPER.getValue()) {
            Set<String> developers = entity.getDevelopers();
            if (developers == null) {
                developers = new HashSet<>();
            }

            developers.add(userName);
            entity.setDevelopers(developers);
        } else if (role == RoleType.OWNER.getValue()) {
            Set<String> owners = entity.getOwners();
            if (owners == null) {
                owners = new HashSet<>();
            }

            owners.add(userName);
            entity.setOwners(owners);
        } else if (role == RoleType.MASTER.getValue()) {
            Set<String> masters = entity.getMasters();
            if (masters == null) {
                masters = new HashSet<>();
            }

            masters.add(userName);
            entity.setMasters(masters);
        }

        toolDeveloperInfoRepository.save(entity);

        return new Result<>(true);
    }
}
