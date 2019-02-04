package org.onap.dcae.collectors.veshv.ves.message.generator.impl.wireframe

import com.google.protobuf.InvalidProtocolBufferException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.ves.message.generator.api.wireframe.WireFrameParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.wireframe.WireFrameType
import org.onap.ves.VesEventOuterClass
import reactor.test.test
import kotlin.test.assertTrue

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since January 2019
 */
object WireFrameGeneratorTest : Spek({

    val maxPayloadSizeBytes = 1024
    val cut = WireFrameGenerator()

    on("message type requesting invalid GPB data ") {
        it("should createVesEventGenerator flux of messages with invalid payload") {
            cut
                    .createMessageFlux(WireFrameParameters(
                            WireFrameType.INVALID_GPB_DATA, 1
                    ))
                    .test()
                    .assertNext {
                        assertTrue(it.validate().isRight())
                        assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                        Assertions.assertThatExceptionOfType(InvalidProtocolBufferException::class.java)
                                .isThrownBy { extractCommonEventHeader(it.payload) }
                    }
                    .verifyComplete()
        }
    }

    on("message type requesting invalid wire frame ") {
        it("should createVesEventGenerator flux of messages with invalid version") {
            cut
                    .createMessageFlux(WireFrameParameters(
                            WireFrameType.INVALID_WIRE_FRAME, 1
                    ))
                    .test()
                    .assertNext {
                        assertTrue(it.validate().isLeft())
                        assertThat(it.payloadSize).isLessThan(maxPayloadSizeBytes)
                        assertThat(it.versionMajor).isNotEqualTo(WireFrameMessage.SUPPORTED_VERSION_MINOR)
                    }
                    .verifyComplete()
        }
    }

})

fun extractCommonEventHeader(bytes: ByteData): VesEventOuterClass.CommonEventHeader =
        VesEventOuterClass.VesEvent.parseFrom(bytes.unsafeAsArray()).commonEventHeader
