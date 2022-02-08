/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getNotificationService
import getPaymentProvider
import io.pleo.antaeus.batch.startScheduler
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.rest.AntaeusRest
import setupInitialData
import java.io.File

fun main() {
    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    val dal = connectToDbAndBuildDal(dbFile)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()
    val notificationService = getNotificationService()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
        paymentProvider = paymentProvider,
        notificationService = notificationService
    )

    // Create and start scheduled jobs
    startScheduler(
        invoiceService = invoiceService,
        billingService = billingService
    )

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}
