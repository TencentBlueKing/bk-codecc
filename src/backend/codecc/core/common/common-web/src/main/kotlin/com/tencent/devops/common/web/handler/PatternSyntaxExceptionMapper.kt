package com.tencent.devops.common.web.handler

import com.sun.imageio.plugins.common.I18N
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.constant.CommonMessageCode
import org.slf4j.LoggerFactory
import java.util.regex.PatternSyntaxException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class PatternSyntaxExceptionMapper : ExceptionMapper<PatternSyntaxException> {

    companion object {
        val logger = LoggerFactory.getLogger(PatternSyntaxExceptionMapper::class.java)!!
    }

    override fun toResponse(exception: PatternSyntaxException): Response {
        logger.error(exception.message, exception)
        val status = Response.Status.BAD_REQUEST
        val errorMsg = I18N.getString(CommonMessageCode.REGULAR_EXP_INVALID)

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(
                    Result<Void>(
                        status = status.statusCode,
                        code = CommonMessageCode.PARAMETER_IS_INVALID,
                        message = errorMsg
                    )
                )
                .build()
    }

}