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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.Option
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.onap.dcae.collectors.veshv.config.impl.gsonadapters.DurationOfSecondsAdapter
import org.onap.dcae.collectors.veshv.config.impl.gsonadapters.OptionAdapter
import org.onap.dcae.collectors.veshv.config.impl.gsonadapters.SecurityAdapter
import org.onap.dcae.collectors.veshv.config.impl.gsonadapters.StreamsAdapter
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers
import java.io.Reader
import java.time.Duration

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal class JsonConfigurationParser {
    private val gson = GsonBuilder()
            .registerTypeAdapter(Option::class.java, OptionAdapter())
            .registerTypeAdapter(PartialSecurityConfig::class.java, SecurityAdapter())
            .registerTypeAdapter(Duration::class.java, DurationOfSecondsAdapter())
            .registerTypeAdapter(
                    TypeToken.getParameterized(List::class.java, KafkaSink::class.java).type,
                    StreamsAdapter(StreamFromGsonParsers.kafkaSinkParser()))
            .create()

    fun parseConfiguration(input: Reader): PartialConfiguration =
            gson.fromJson(input, PartialConfiguration::class.java)
                    .also { logger.info { "Successfully parsed json to configuration: $it" } }

    companion object {
        private val logger = Logger(JsonConfigurationParser::class)
    }
}
