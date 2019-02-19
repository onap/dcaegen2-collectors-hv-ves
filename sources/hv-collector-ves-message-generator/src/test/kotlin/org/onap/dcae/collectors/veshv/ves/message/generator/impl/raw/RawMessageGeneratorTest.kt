/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.raw

import com.google.protobuf.InvalidProtocolBufferException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType
import org.onap.dcae.collectors.veshv.ves.message.generator.generators.RawMessageGenerator
import org.onap.ves.VesEventOuterClass
import reactor.test.test
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
object WireFrameGeneratorTest : Spek({

    val maxPayloadSizeBytes = 1024
    val cut = RawMessageGenerator()

    on("message type requesting invalid GPB data ") {
        it("should createVesEventGenerator flux of messages with invalid payload") {
            cut
                    .createMessageFlux(WireFrameParameters(
                            WireFrameType.INVALID_GPB_DATA, 1
                    ))
                    .test()
                    .assertNext {
                        val decodedBytes = it.array().toString(Charset.defaultCharset())
                        assertThat(decodedBytes).isEqualTo("invalid vesEvent")
                        assertThat(it.capacity()).isLessThan(maxPayloadSizeBytes)

                        Assertions.assertThatExceptionOfType(InvalidProtocolBufferException::class.java)
                                .isThrownBy { extractCommonEventHeader(it) }
                    }
                    .verifyComplete()
        }
    }
})

private fun extractCommonEventHeader(bytes: ByteBuffer): VesEventOuterClass.CommonEventHeader =
        VesEventOuterClass.VesEvent.parseFrom(bytes).commonEventHeader
