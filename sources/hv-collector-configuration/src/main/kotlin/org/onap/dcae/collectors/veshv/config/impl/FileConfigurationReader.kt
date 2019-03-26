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
import org.onap.dcae.collectors.veshv.config.impl.gsonadapters.SecurityAdapter
import java.io.Reader
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal class FileConfigurationReader {
    private val gson = GsonBuilder()
            .registerTypeAdapter(Option::class.java, OptionAdapter())
            .registerTypeAdapter(PartialSecurityConfig::class.java, SecurityAdapter())
            .create()

    fun loadConfig(input: Reader): PartialConfiguration =
            gson.fromJson(input, PartialConfiguration::class.java)
}
