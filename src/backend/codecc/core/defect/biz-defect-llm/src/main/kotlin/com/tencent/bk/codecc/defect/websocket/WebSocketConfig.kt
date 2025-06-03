package com.tencent.bk.codecc.defect.websocket

import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.HandshakeInterceptor

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/websocket/user/warn").addInterceptors(
            object : HandshakeInterceptor {
                override fun beforeHandshake(
                    request: ServerHttpRequest,
                    response: ServerHttpResponse,
                    wsHandler: WebSocketHandler,
                    attributes: MutableMap<String, Any>
                ): Boolean {
                    val req = request as ServletServerHttpRequest
                    val user = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_USER_ID)
                    val taskId = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_TASK_ID)
                    val projectId = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    val bkTicket = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_BK_TICKET)
                    LLMConstants.BK_TICKET_FOR_USER.put(user, bkTicket)
                    logger.info("before connect websocket: /websocket/user/warn, " +
                            "user: $user, " + "taskId: $taskId, projectId: $projectId")
                    return true
                }

                override fun afterHandshake(
                    request: ServerHttpRequest,
                    response: ServerHttpResponse,
                    wsHandler: WebSocketHandler,
                    exception: java.lang.Exception?
                ) {
                    logger.info("after hand shake, end point established")
                    val req = request as ServletServerHttpRequest
                    val taskId = req.servletRequest.getParameter(AUTH_HEADER_DEVOPS_TASK_ID)
                }
            }
        ).setAllowedOriginPatterns("*").withSockJS()
    }
}
