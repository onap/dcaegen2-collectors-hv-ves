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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.factory

import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.HvVesClient
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.ClientConfiguration
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.HvVesProducerFactory
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.options.ImmutableProducerOptions
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.options.WireFrameVersion

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
class ClientFactory(private val configuration: ClientConfiguration) {

    fun create(wireFrameVersion: WireFrameVersion): HvVesClient =
            buildClient(createPartialConfiguration().wireFrameVersion(wireFrameVersion))

    fun create(): HvVesClient = buildClient(createPartialConfiguration())

    /*
        TODO: REMOVE COMMENTS AFTER REVIEW
        I decided to create whole partial config at every "create" function call rather than keeping
        part of it as a field and then applying the security part dynamically,
        because found it more readable. In this case the overhead difference is minimal.
    */
    private fun createPartialConfiguration() = ImmutableProducerOptions
            .builder()
            .collectorAddresses(configuration.collectorAddresses)
            .let { options ->
                configuration.securityProvider().keys.fold(
                        { options },
                        { options.securityKeys(it) })
            }

    private fun buildClient(config: ImmutableProducerOptions.Builder) =
            HvVesClient(HvVesProducerFactory.create(config.build()))
}
