package io.pleo.antaeus.core.exceptions

class InvoiceAlreadyPaidException(invoiceId: Int)
    : Exception("Invoice $invoiceId has already been paid, cannot be processed again")
