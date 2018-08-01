package org.onap.dcae.collectors.veshv.main

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.main.impl.MessageFactory
import kotlin.test.assertEquals

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
object WireFrameTest : Spek({

    val factory = MessageFactory()


    given("no parameters") {
        it("should return flux with one message") {
            val result = factory.createMessageFlux()

            assertEquals(1, result.count().block())
        }
    }
    given("messages amount") {
        it("should return message flux of specified size") {
            val result = factory.createMessageFlux(5)
            assertEquals(5, result.count().block())
        }
    }
})