package io.pleo.antaeus.core.exceptions

class InvoiceAlreadyInProcessException(invoiceId: Int)
    : Exception("Invoice $invoiceId is already in PROCESSING state, cannot be processed again")
