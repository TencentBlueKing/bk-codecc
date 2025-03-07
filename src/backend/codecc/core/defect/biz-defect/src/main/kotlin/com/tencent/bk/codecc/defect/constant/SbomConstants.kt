package com.tencent.bk.codecc.defect.constant

const val SBOM_LICENSE_SEPARATOR: String = "OR"

enum class SbomRelationshipType {
    DESCRIBES,
    DEPENDENCY_MANIFEST_OF,
    VARIANT_OF,
    CONTAINED_BY
}
