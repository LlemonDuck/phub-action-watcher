package com.duckblade.phubreview

import com.duckblade.phubreview.jobs.ActionsWatcherJob
import com.duckblade.phubreview.jobs.MergeJob
import com.duckblade.phubreview.jobs.StatusPanelUpdateJob
import com.duckblade.phubreview.ui.MergeQueuePanel
import com.duckblade.phubreview.ui.StatusPanel
import com.duckblade.phubreview.ui.WatcherFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

suspend fun main() {

    val gh = Github("runelite/plugin-hub")
    val mergeQueue = MergeQueue(gh)

    val mergeQueuePanel = MergeQueuePanel(mergeQueue)
    val statusPanel = StatusPanel()
    val watcherFrame = WatcherFrame(statusPanel, mergeQueuePanel)

    val actionsWatcherJob = ActionsWatcherJob(gh)
    val statusPanelUpdateJob = StatusPanelUpdateJob(statusPanel, actionsWatcherJob)
    val mergeJob = MergeJob(actionsWatcherJob, mergeQueue, gh)

    coroutineScope {
        launch { actionsWatcherJob.start() }
        launch { mergeJob.start() }
        launch { statusPanelUpdateJob.start() }
        launch(Dispatchers.Swing) { watcherFrame.isVisible = true }
    }

}

