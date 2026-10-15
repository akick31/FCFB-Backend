package com.fcfb.arceus.util.job

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

class AsyncJobTracker<T : TrackedJob>(private val shutdownMessage: String) {
    private val workerJob = SupervisorJob()
    private val workerScope = CoroutineScope(Dispatchers.IO + workerJob)
    private val shutdownSignal = CompletableDeferred<Unit>()
    private val workerLock = Any()
    private val activeJobs = ConcurrentHashMap<String, T>()

    val isShuttingDown: Boolean
        get() = shutdownSignal.isCompleted

    fun launch(
        job: T,
        onCancelled: (String) -> Unit,
        process: suspend () -> Unit,
    ) {
        synchronized(workerLock) {
            check(!shutdownSignal.isCompleted) { shutdownMessage }
            activeJobs[job.jobId] = job
            workerScope.launch {
                try {
                    process()
                } catch (e: CancellationException) {
                    onCancelled(job.jobId)
                    throw e
                }
            }
        }
    }

    fun update(
        jobId: String,
        update: (T) -> T,
    ): T = activeJobs.computeIfPresent(jobId) { _, job -> update(job) }!!

    fun get(jobId: String): T? = activeJobs[jobId]

    fun contains(jobId: String): Boolean = activeJobs.containsKey(jobId)

    fun all(): Collection<T> = activeJobs.values

    suspend fun awaitShutdownOrTimeout(durationMs: Long) {
        withTimeoutOrNull(durationMs) { shutdownSignal.await() }
    }

    fun stop() {
        synchronized(workerLock) {
            shutdownSignal.complete(Unit)
            workerJob.complete()
        }
        runBlocking { workerJob.join() }
    }
}
