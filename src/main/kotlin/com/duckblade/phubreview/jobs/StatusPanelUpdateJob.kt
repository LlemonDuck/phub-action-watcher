package com.duckblade.phubreview.jobs

import com.duckblade.phubreview.ui.StatusPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.swing.Swing
import kotlin.time.Duration.Companion.milliseconds

class StatusPanelUpdateJob(
    private val statusPanel: StatusPanel,
    private val actionsWatcherJob: ActionsWatcherJob,
) {
    
    suspend fun start() {
        while (true) {
            delay(20.milliseconds)

            Dispatchers.Swing {
                statusPanel.rerender(actionsWatcherJob.lastRun)
            }
        }
    }
    
}