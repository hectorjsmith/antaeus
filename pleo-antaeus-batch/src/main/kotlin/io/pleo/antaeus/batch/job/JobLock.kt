package io.pleo.antaeus.batch.job

// This method synchronizes all the batch jobs to ensure only one can run at a time
@Synchronized
fun withLock(op: () -> Unit) {
    op()
}
