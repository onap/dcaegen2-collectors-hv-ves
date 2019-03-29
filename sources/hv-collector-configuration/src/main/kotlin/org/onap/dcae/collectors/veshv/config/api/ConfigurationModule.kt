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
import org.onap.dcae.collectors.veshv.config.impl.CbsConfigurationProvider
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationMerger
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationValidator
import org.onap.dcae.collectors.veshv.config.impl.FileConfigurationReader
import org.onap.dcae.collectors.veshv.config.impl.HvVesCommandLineParser
import org.onap.dcae.collectors.veshv.utils.arrow.rightOrThrow
import org.onap.dcae.collectors.veshv.utils.arrow.throwOnLeft
import org.onap.dcae.collectors.veshv.utils.logging.MappedDiagnosticContext
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties
import reactor.core.publisher.Flux

class ConfigurationModule {

    private val cmd = HvVesCommandLineParser()
    private val configReader = FileConfigurationReader()
    private val configValidator = ConfigurationValidator()
    
    fun healthCheckPort(args: Array<String>): Int = cmd.getHealthcheckPort(args)

    fun hvVesConfigurationUpdates(args: Array<String>,
                                  configStateListener: ConfigurationStateListener,
                                  mdc: MappedDiagnosticContext): Flux<HvVesConfiguration> =
            Flux.just(cmd.getConfigurationFile(args))
                    .throwOnLeft { MissingArgumentException(it.message, it.cause) }
                    .map { it.reader().use(configReader::loadConfig) }
                    .cache()
                    .flatMap { basePartialConfig ->
                        val baseConfig = configValidator.validate(basePartialConfig).rightOrThrow { ValidationException(it.message) }
                        val cbsConfigProvider = CbsConfigurationProvider(
                                CbsClientFactory.createCbsClient(EnvProperties.fromEnvironment()),
                                baseConfig.cbs,
                                configStateListener,
                                mdc)
                        val merger = ConfigurationMerger()
                        cbsConfigProvider()
                                .map { merger.merge(basePartialConfig, it) }
                                .map { configValidator.validate(it) }
                                .throwOnLeft { ValidationException(it.message) }
                    }

}
