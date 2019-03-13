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
package org.onap.dcae.collectors.veshv.config.api

import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.MissingArgumentException
import org.onap.dcae.collectors.veshv.config.api.model.ValidationException
import org.onap.dcae.collectors.veshv.config.impl.ArgHvVesConfiguration
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationAdapter
import org.onap.dcae.collectors.veshv.config.impl.FileConfigurationReader
import org.onap.dcae.collectors.veshv.utils.arrow.rightOrThrow
import reactor.core.publisher.Flux
import java.time.Duration

class ConfigurationModule {

    private val configurationReader = FileConfigurationReader()
    private val configurationAdapter = ConfigurationAdapter()

    fun hvVesConfigurationUpdates(args: Array<String>): Flux<HvVesConfiguration> =
            Flux.just(ArgHvVesConfiguration().parse(args))
                    .map { it.rightOrThrow { err -> MissingArgumentException(err.message, err.cause) } }
                    .map { configurationReader.loadConfig(it.reader()) }
                    .map { configurationAdapter.createConfiguration(it) }
                    .map {
                        it.rightOrThrow { err -> ValidationException(err.message) }
                    }
                    .repeat()
                    .delayElements(Duration.ofSeconds(10))

    companion object {
        const val VES_HV_PACKAGE = "org.onap.dcae.collectors.veshv"
        const val PROGRAM_NAME = "java $VES_HV_PACKAGE.main.MainKt"
    }
}
