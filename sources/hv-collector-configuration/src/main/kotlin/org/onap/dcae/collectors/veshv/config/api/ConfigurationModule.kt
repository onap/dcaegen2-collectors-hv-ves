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
import org.onap.dcae.collectors.veshv.config.impl.CbsConfigurationProvider
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationMerger
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationTransformer
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationValidator
import org.onap.dcae.collectors.veshv.config.impl.HvVesCommandLineParser
import org.onap.dcae.collectors.veshv.config.impl.JsonConfigurationParser
import org.onap.dcae.collectors.veshv.config.impl.PartialConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.throwOnLeft
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.MappedDiagnosticContext
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ConfigurationModule {

    private val cmd = HvVesCommandLineParser()
    private val configParser = JsonConfigurationParser()
    private val configMerger = ConfigurationMerger()
    private val configValidator = ConfigurationValidator()
    private val configTransformer = ConfigurationTransformer()

    fun healthCheckPort(args: Array<String>): Int = cmd.getHealthcheckPort(args)

    fun hvVesConfigurationUpdates(args: Array<String>,
                                  configStateListener: ConfigurationStateListener,
                                  mdc: MappedDiagnosticContext): Flux<HvVesConfiguration> =
            Mono.just(cmd.getConfigurationFile(args))
                    .throwOnLeft(::MissingArgumentException)
                    .doOnNext { logger.info { "Using base configuration file: ${it.absolutePath}" } }
                    .map { it.reader().use(configParser::parse) }
                    .doOnNext { logger.info { "Successfully parsed configuration file to: $it" } }
                    .cache()
                    .flatMapMany { basePartialConfig ->
                        cbsConfigurationProvider(basePartialConfig, configStateListener, mdc)
                                .invoke()
                                .map { configMerger.merge(basePartialConfig, it) }
                                .map(configValidator::validate)
                                .throwOnLeft()
                                .map(configTransformer::toFinalConfiguration)
                    }

    private fun cbsConfigurationProvider(basePartialConfig: PartialConfiguration,
                                         configStateListener: ConfigurationStateListener,
                                         mdc: MappedDiagnosticContext) =
            CbsConfigurationProvider(
                    CbsClientFactory.createCbsClient(EnvProperties.fromEnvironment()),
                    cbsConfigurationFrom(basePartialConfig),
                    configParser,
                    configStateListener,
                    mdc)

    private fun cbsConfigurationFrom(basePartialConfig: PartialConfiguration) =
            configValidator.validatedCbsConfiguration(basePartialConfig)
                    .let { configTransformer.toCbsConfiguration(it) }

    companion object {
        private val logger = Logger(ConfigurationModule::class)
    }

}
