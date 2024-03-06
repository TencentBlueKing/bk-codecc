package com.tencent.devops.common.web.actuator

import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.AvailabilityState
import org.springframework.boot.availability.LivenessState

class LivenessIndicator(availability: ApplicationAvailability?) : LivenessStateHealthIndicator(availability) {

    companion object {
        private val logger = LoggerFactory.getLogger(LivenessIndicator::class.java)
    }

    @Value("\${storage.type:#{null}}")
    var storageType: String? = null

    override fun getState(applicationAvailability: ApplicationAvailability?): AvailabilityState {
        return CommonStateIndicator.getState(
            storageType, LivenessState.BROKEN, super.getState(applicationAvailability)
        )
    }
}