package com.tencent.devops.common.web.actuator

import com.tencent.devops.common.storage.constant.StorageType
import org.springframework.boot.availability.AvailabilityState
import java.io.File

object CommonStateIndicator {

    fun getState(
        storageType: String?, rejectStatus: AvailabilityState,
        defaultStatus: AvailabilityState
    ): AvailabilityState {
        if (storageType.isNullOrEmpty() || !StorageType.isMountType(storageType)) {
            return defaultStatus
        }
        val path = System.getProperty("codecc.storage.mouth.path")
        if (path.isNullOrEmpty()) {
            return defaultStatus
        }
        val dir = File(path)
        val existDir = dir.exists() && dir.isDirectory
        if (!existDir) {
            return rejectStatus
        }
        return defaultStatus
    }
}
