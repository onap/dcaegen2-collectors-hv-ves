package org.onap.dcae.collectors.veshv.simulators.dcaeapp

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertEquals

object DummyTest : Spek({
    on("sum of 2 and 3") {
        val sum = 2 + 3
        it("outcome should be equals 5") {
            assertEquals(5, sum)
        }
    }
})