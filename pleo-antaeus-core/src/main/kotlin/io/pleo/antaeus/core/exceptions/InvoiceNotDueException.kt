package io.pleo.antaeus.core.exceptions

class InvoiceNotDueException(invoiceId: Int):
    Exception("Invoice $invoiceId is not yet due for payment")
