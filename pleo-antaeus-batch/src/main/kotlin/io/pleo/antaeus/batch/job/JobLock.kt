package io.pleo.antaeus.batch.job

@Synchronized
fun withLock(op: () -> Unit) {
    op()
}
