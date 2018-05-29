package org.onap.dcae.collectors.veshv.main.config

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
data class ClientConfiguration( val vesHost: String, val vesPort: Int ,val messagesAmount: Int)
