package com.tencent.devops.common.web.validate

import com.tencent.devops.common.web.validate.filter.ValidateProjectFilter
import jakarta.ws.rs.container.DynamicFeature
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.FeatureContext

class ValidateProjectDynamicFeature : DynamicFeature {
    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        if (resourceInfo.resourceMethod.isAnnotationPresent(ValidateProject::class.java)) {
            context.register(ValidateProjectFilter())
            return
        }
    }
}