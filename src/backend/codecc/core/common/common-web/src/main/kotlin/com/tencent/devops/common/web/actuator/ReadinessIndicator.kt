package com.tencent.devops.common.web.actuator

import com.tencent.devops.common.web.actuator.CommonStateIndicator.getState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.AvailabilityState
import org.springframework.boot.availability.ReadinessState

class ReadinessIndicator(availability: ApplicationAvailability?) : ReadinessStateHealthIndicator(availability) {

    companion object {
        private val logger = LoggerFactory.getLogger(ReadinessIndicator::class.java)
    }

    @Value("\${storage.type:#{null}}")
    var storageType: String? = null

    override fun getState(applicationAvailability: ApplicationAvailability?): AvailabilityState {
        return getState(
            storageType, ReadinessState.REFUSING_TRAFFIC, super.getState(applicationAvailability)
        )
    }
}