package com.duckblade.phubreview

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

enum class MergeState {
    LOADING,
    READY,
    MERGING,
}

data class QueuedItem(
    val number: Int,
    var state: MergeState = MergeState.LOADING,
    var sha: String? = null,
    var subject: String? = null,
)

class MergeQueue(
    private val gh: Github,
) {

    private val queue = ArrayDeque<QueuedItem>()
    private val queueMutex = Mutex()

    private val queueListeners = mutableListOf<suspend (List<QueuedItem>) -> Unit>()

    suspend fun add(pr: Int): Unit = run {
        val qi = QueuedItem(pr)
        queueMutex.withLock {
            if (queue.any { it.number == pr }) {
                println("PR $pr already in queue, skipping")
                return@run
            }

            println("Loading metadata for PR $pr...")
            queue.add(qi)
        }
        notifyListeners()

        val pr = gh.getPullRequest(pr)
        if (pr.updatedAt >= Clock.System.now() - 1.minutes) {
            println("PR ${pr.number} updated too recently, dropping")
            queueMutex.withLock { queue.remove(qi) }
            notifyListeners()
            return
        }

        val subject = gh.getMergeSubject(pr.number)
        if (subject == null) {
            println("PR ${pr.number} missing subject, dropping")
            queueMutex.withLock { queue.remove(qi) }
            notifyListeners()
            return
        }

        qi.state = MergeState.READY
        qi.sha = pr.head.sha
        qi.subject = subject
        println("PR ${pr.number} added to queue: $subject")
        notifyListeners()
    }

    suspend fun remove(pr: Int) {
        queueMutex.withLock {
            queue.removeAll { it.number == pr }
        }
        notifyListeners()
    }

    suspend fun pop(): QueuedItem? {
        val ret = queueMutex.withLock {
            val peek = queue.firstOrNull() ?: return null
            if (peek.state == MergeState.LOADING) {
                return null
            }

            queue.removeFirst()
        }

        notifyListeners()
        return ret
    }

    fun addQueueListener(listener: suspend (List<QueuedItem>) -> Unit) {
        queueListeners.add(listener)
    }

    private suspend fun notifyListeners() {
        val queueCopy = queueMutex.withLock { queue.toList() }
        queueListeners.forEach { listener ->
            listener(queueCopy)
        }
    }

}