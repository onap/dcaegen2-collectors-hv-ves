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
import org.onap.dcae.collectors.veshv.config.impl.gsonadapters.OptionAdapter
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.io.Reader

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal class JsonConfigurationParser {
    private val gson = GsonBuilder()
            .registerTypeAdapter(Option::class.java, OptionAdapter())
            .create()

    fun parse(input: Reader): PartialConfiguration = gson
            .fromJson(input, PartialConfiguration::class.java)
            .also { logger.info { "Successfully read file and parsed json to configuration: $it" } }

    companion object {
        private val logger = Logger(JsonConfigurationParser::class)
    }
}
