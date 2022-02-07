package io.pleo.antaeus.core.external

/**
 * External service to queue and send notifications to people.
 * This can be implemented in various ways: email, SMS, push notifications, etc.
 * For the purposes of this exercise, this is not implemented, just left as a stub.
 *
 * It is assumed that the notification service will handle any errors internally, and not propagate them back to the caller.
 */
interface NotificationService {

    /**
     * Send a notification of some kind to the account owner about a given invoice.
     * @param accountId ID of the account to notify
     * @param invoiceId ID of the invoice this notification relates to
     * @param message Message to send to the user
     */
    fun notifyAccountOwner(accountId: Int, invoiceId: Int, message: String)

    /**
     * Send a notification of some kind to a system admin about a given invoice.
     * @param invoiceId ID of the invoice this notification relates to
     * @param message Message to send to the admin
     */
    fun notifyAdministrator(invoiceId: Int, message: String)
}