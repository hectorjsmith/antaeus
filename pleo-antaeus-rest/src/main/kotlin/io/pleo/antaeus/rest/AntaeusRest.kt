/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.InvoiceValidationService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import org.eclipse.jetty.http.HttpStatus
import java.io.ByteArrayInputStream
import java.io.InputStream

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

fun handleBadRequestException(ex: Exception, ctx: Context) {
    ctx.status(HttpCode.BAD_REQUEST)
    ctx.json(ex.message ?: "bad request data")
}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService,
    private val invoiceValidationService: InvoiceValidationService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create {
            it.jsonMapper(JavalinMapper())
        }
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(HttpCode.NOT_FOUND)
            }

            // Handle exceptions related to trying to process an invoice that should not be processed
            exception(InvoiceAlreadyPaidException::class.java)  { ex, ctx -> handleBadRequestException(ex, ctx) }
            exception(InvoiceNotDueException::class.java) { ex, ctx -> handleBadRequestException(ex, ctx) }
            exception(InvoiceAlreadyInProcessException::class.java) { ex, ctx -> handleBadRequestException(ex, ctx) }

            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(HttpStatus.NOT_FOUND_404) { ctx -> ctx.json("not found") }
            error(HttpStatus.INTERNAL_SERVER_ERROR_500) { ctx -> ctx.json("internal server error") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        path("{id}") {
                            // URL: /rest/v1/invoices/{:id}
                            get {
                                it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                            }

                            // URL: /rest/v1/invoices/{:id}/retry
                            get("retry") {
                                val invoice = invoiceService.fetch(it.pathParam("id").toInt())
                                if (invoice.status != InvoiceStatus.FAILED) {
                                    handleBadRequestException(
                                        Exception("Cannot retry an invoice that isn't in the FAILED state"),
                                        it
                                    )
                                } else {
                                    it.json(billingService.processAndSaveInvoice(invoice))
                                }
                            }

                            // URL: /rest/v1/invoices/{:id}/validate
                            get("validate") {
                                val invoice = invoiceService.fetch(it.pathParam("id").toInt())
                                it.json(invoiceValidationService.validateAndSaveInvoice(invoice))
                            }

                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get("{id}") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }
                }
            }
        }
    }
}

class JavalinMapper : io.javalin.plugin.json.JsonMapper {
    private val mapper = JsonMapper()
        .registerModule(JodaModule())

    override fun toJsonString(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }

    override fun toJsonStream(obj: Any): InputStream {
        return ByteArrayInputStream(mapper.writeValueAsBytes(obj))
    }

    override fun <T : Any?> fromJsonString(json: String, targetClass: Class<T>): T {
        return mapper.readValue(json, targetClass)
    }

    override fun <T : Any?> fromJsonStream(json: InputStream, targetClass: Class<T>): T {
        return mapper.readValue(json, targetClass)
    }
}
