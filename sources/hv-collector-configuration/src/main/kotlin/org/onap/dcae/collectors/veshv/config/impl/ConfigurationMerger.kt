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
import arrow.core.getOrElse
import arrow.core.toOption

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since March 2019
 */
internal class ConfigurationMerger {
    @Suppress("MaxLineLength")
    fun merge(base: PartialConfiguration, update: PartialConfiguration): PartialConfiguration =
            PartialConfiguration(
                    listenPort = base.listenPort.updateToGivenOrNone(update.listenPort),
                    idleTimeoutSec = base.idleTimeoutSec.updateToGivenOrNone(update.idleTimeoutSec),

                    firstRequestDelaySec = base.firstRequestDelaySec.updateToGivenOrNone(update.firstRequestDelaySec),
                    requestIntervalSec = base.requestIntervalSec.updateToGivenOrNone(update.requestIntervalSec),

                    sslDisable = base.sslDisable.updateToGivenOrNone(update.sslDisable),
                    keyStoreFile = base.keyStoreFile.updateToGivenOrNone(update.keyStoreFile),
                    keyStorePasswordFile = base.keyStorePasswordFile.updateToGivenOrNone(update.keyStorePasswordFile),
                    trustStoreFile = base.trustStoreFile.updateToGivenOrNone(update.trustStoreFile),
                    trustStorePasswordFile = base.trustStorePasswordFile.updateToGivenOrNone(update.trustStorePasswordFile),

                    streamPublishers = base.streamPublishers.updateToGivenOrNone(update.streamPublishers),

                    logLevel = base.logLevel.updateToGivenOrNone(update.logLevel)
            )

    private fun <T> Option<T>.updateToGivenOrNone(update: Option<T>) =
            update.getOrElse(this::orNull).toOption()
}
