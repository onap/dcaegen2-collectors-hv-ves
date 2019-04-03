/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.impl.gsonadapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.onap.dcaegen2.services.sdk.model.streams.StreamType
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.DataStreams
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParser
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamPredicates
import java.lang.reflect.Type

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since April 2019
 */
internal class StreamsAdapter(private val streamParser: StreamFromGsonParser<KafkaSink>)
    : JsonDeserializer<List<KafkaSink>> {

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext?): List<KafkaSink> =
            DataStreams.namedSinks(wrapWithStreamsRootKey(json))
                    .filter(StreamPredicates.streamOfType(StreamType.KAFKA))
                    .map(streamParser::unsafeParse)
                    .asIterable()
                    .toList()

    private fun wrapWithStreamsRootKey(streamsJson: JsonElement) =
            JsonObject().apply { add(STREAMS_ROOT_KEY, streamsJson) }

    companion object {
        private const val STREAMS_ROOT_KEY = "streamsPublishes"
    }
}
