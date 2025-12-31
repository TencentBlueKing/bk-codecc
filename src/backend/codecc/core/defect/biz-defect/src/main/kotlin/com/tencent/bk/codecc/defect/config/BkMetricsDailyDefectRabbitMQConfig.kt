package com.tencent.bk.codecc.defect.config

import com.tencent.bk.codecc.defect.condition.DefectCondition
import com.tencent.bk.codecc.defect.consumer.BkMetricsDailyDefectConsumer
import com.tencent.codecc.common.mq.AbstractMQConfig
import com.tencent.devops.common.web.mq.EXCHANGE_BK_METRICS_DAILY_DEFECT_STATISTIC
import com.tencent.devops.common.web.mq.QUEUE_BK_METRICS_DAILY_DEFECT_STATISTIC
import com.tencent.devops.common.web.mq.ROUTE_BK_METRICS_DAILY_DEFECT_STATISTIC
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration

@Conditional(DefectCondition::class)
@Configuration
class BkMetricsDailyDefectRabbitMQConfig : AbstractMQConfig(
    exchangeName = EXCHANGE_BK_METRICS_DAILY_DEFECT_STATISTIC,
    queueName = QUEUE_BK_METRICS_DAILY_DEFECT_STATISTIC,
    routeName = ROUTE_BK_METRICS_DAILY_DEFECT_STATISTIC
) {
    @Bean
    fun bkMetricsDailyDefectExchange() = abstractExchange()

    @Bean
    fun bkMetricsDailyDefectQueue() = abstractQueue()

    @Bean
    fun bkMetricsDailyDefectBinding(
        @Autowired bkMetricsDailyDefectExchange: CustomExchange,
        @Autowired bkMetricsDailyDefectQueue: Queue
    ) = abstractBinding(bkMetricsDailyDefectQueue, bkMetricsDailyDefectExchange)

    @Bean
    fun bkMetricsDailyDefectListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired bkMetricsDailyDefectConsumer: BkMetricsDailyDefectConsumer
    ) = abstractListenerContainer(
        bkMetricsDailyDefectConsumer,
        bkMetricsDailyDefectConsumer::consumer.name,
        connectionFactory
    )
}
