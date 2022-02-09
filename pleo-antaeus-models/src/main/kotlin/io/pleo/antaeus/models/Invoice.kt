package io.pleo.antaeus.models

import org.joda.time.DateTime

data class Invoice(
    val id: Int,
    val customerId: Int,
    val amount: Money,
    val status: InvoiceStatus,
    val creationTime: DateTime,
    val retryPaymentTime: DateTime?
)
