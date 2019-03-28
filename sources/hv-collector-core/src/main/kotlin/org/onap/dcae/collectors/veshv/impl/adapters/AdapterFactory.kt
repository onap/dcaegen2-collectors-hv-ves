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
package org.onap.dcae.collectors.veshv.impl.adapters

import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.impl.adapters.kafka.KafkaSinkProvider
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object AdapterFactory {
    fun sinkCreatorFactory(): SinkProvider = KafkaSinkProvider()

    fun configurationProvider(config: CbsConfiguration): ConfigurationProvider =
            ConfigurationProviderImpl(
                    CbsClientFactory.createCbsClient(EnvProperties.fromEnvironment()),
                    config)
}
