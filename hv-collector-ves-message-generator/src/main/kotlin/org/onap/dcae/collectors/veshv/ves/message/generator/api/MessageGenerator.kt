/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
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

import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.ves.message.generator.config.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.impl.MessageGeneratorImpl
import org.onap.dcae.collectors.veshv.ves.message.generator.impl.PayloadGenerator
import org.onap.ves.VesEventV5
import reactor.core.publisher.Flux
import javax.json.JsonObject

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
interface MessageGenerator {
    fun createMessageFlux(messageParameters: MessageParameters): Flux<PayloadWireFrameMessage>
    fun parseCommonHeader(json: JsonObject): VesEventV5.VesEvent.CommonEventHeader

    companion object {
        val INSTANCE: MessageGenerator by lazy {
            MessageGeneratorImpl(PayloadGenerator())
        }
    }
}

