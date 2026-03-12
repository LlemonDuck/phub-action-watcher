package com.duckblade.phubreview.jobs

import com.duckblade.phubreview.Github
import com.duckblade.phubreview.MergeQueue
import com.duckblade.phubreview.MergeState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class MergeJob(
    private val actionsWatcherJob: ActionsWatcherJob,
    private val mergeQueue: MergeQueue,
    private val gh: Github,
) {

    private var lastSeen: Long = 0
    private var seenYellow = true
    private var greenCount = 0

    suspend fun start() {
        while (true) {
            delay(20.milliseconds)

            val lastRun = actionsWatcherJob.lastRun ?: continue
            val lastRunTime = actionsWatcherJob.lastRunTime
            if (lastSeen == lastRunTime) continue
            lastSeen = lastRunTime

            if (lastRun.status != "completed") {
                seenYellow = true
                greenCount = 0
            } else {
                if (seenYellow && lastRun.conclusion == "success") {
                    greenCount++
                }
            }

            if (seenYellow && greenCount > 5) {
                val pr = mergeQueue.pop() ?: continue

                println("Merging ${pr.number}: ${pr.subject}")
                pr.state = MergeState.MERGING
                
                seenYellow = false
                gh.merge(pr)
            }
        }
    }

}
