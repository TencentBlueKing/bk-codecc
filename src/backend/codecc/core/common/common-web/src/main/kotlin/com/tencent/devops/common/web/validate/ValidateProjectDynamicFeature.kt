package com.tencent.devops.common.web.validate

import com.tencent.devops.common.web.security.AuthMethod
import com.tencent.devops.common.web.security.filter.PermissionAuthFilter
import com.tencent.devops.common.web.validate.filter.ValidateProjectFilter
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext

class ValidateProjectDynamicFeature : DynamicFeature {
    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        if (resourceInfo.resourceMethod.isAnnotationPresent(ValidateProject::class.java)) {
            context.register(ValidateProjectFilter())
            return
        }
    }
}