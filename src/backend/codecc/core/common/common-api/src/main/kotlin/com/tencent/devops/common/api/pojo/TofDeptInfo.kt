package com.tencent.devops.common.api.pojo

data class TofDeptInfo(
    val ID: Int,
    val Name: String,
    val ParentId: String,
    /**
     * @see com.tencent.devops.common.enums.OrganizationType
     */
    val TypeId: String,
    val LeaderId: String,
    val Level: String,
    val Enabled: String,
    val SecretaryId: String,
    val TypeName: String,
    val VicePresidentId: String,
    val ExProperties: String,
    val ExchangeGroupName: String
)
