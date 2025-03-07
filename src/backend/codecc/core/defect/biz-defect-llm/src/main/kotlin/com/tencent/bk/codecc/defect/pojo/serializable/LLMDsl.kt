package com.tencent.bk.codecc.defect.pojo.serializable

/**
 * A marker annotations for DSLs.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
public annotation class LLMDsl
