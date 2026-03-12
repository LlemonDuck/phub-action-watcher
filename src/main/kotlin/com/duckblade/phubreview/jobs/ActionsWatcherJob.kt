package com.duckblade.phubreview.jobs

import com.duckblade.phubreview.Github
import com.duckblade.phubreview.WorkflowRun
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

class ActionsWatcherJob(
    private val gh: Github,
) {

    private val lastRunTimeAtomic = AtomicReference(0L)
    private val lastRunAtomic = AtomicReference<WorkflowRun>(null)
    
    val lastRun: WorkflowRun?
        get() = lastRunAtomic.get()
    
    val lastRunTime: Long
        get() = lastRunTimeAtomic.get()

    suspend fun start() {
        while (true) {
            delay(5.seconds)

            try {
                val workflows = gh.getWorkflowRuns()

                val mostRecent = workflows.workflowRuns
                    .filter { it.headRepository.fullName == "runelite/plugin-hub" } // filter out forks' "master" branches
                    .maxByOrNull { it.updatedAt }
                lastRunAtomic.set(mostRecent)
                lastRunTimeAtomic.set(System.currentTimeMillis())
            } catch (t: Throwable) {
            }
        }
    }

}