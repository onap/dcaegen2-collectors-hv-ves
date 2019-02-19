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
package org.onap.dcae.collectors.veshv.ves.message.generator.api

import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType.INVALID_WIRE_FRAME
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.options.ImmutableWireFrameVersion
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.options.WireFrameVersion
import org.onap.ves.VesEventOuterClass

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
sealed class MessageParameters

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
class WireFrameParameters(val messageType: WireFrameType,
                          val amount: Long = -1) : MessageParameters() {

    val wireFrameVersion: WireFrameVersion
        get() = ImmutableWireFrameVersion.builder().let {
            if (messageType == INVALID_WIRE_FRAME)
                it.major(UNSUPPORTED_MAJOR_VERSION)
            else
                it
        }.build()

    companion object {
        private const val UNSUPPORTED_MAJOR_VERSION: Short = 2
    }
}

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
class VesEventParameters(val commonEventHeader: VesEventOuterClass.CommonEventHeader,
                         val messageType: VesEventType,
                         val amount: Long = -1) : MessageParameters()
