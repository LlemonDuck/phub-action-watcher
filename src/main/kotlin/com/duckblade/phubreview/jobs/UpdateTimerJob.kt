package com.duckblade.phubreview.jobs

import com.duckblade.phubreview.humanFormat
import com.duckblade.phubreview.ui.StatusPanel
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private val START_TIME = Clock.System.now()

class UpdateTimerJob(
    private val statusPanel: StatusPanel,
) {

    suspend fun start() {
        while (true) {
            delay(50.milliseconds)
            statusPanel.updateTimer((Clock.System.now() - START_TIME).humanFormat());
        }
    }
}
