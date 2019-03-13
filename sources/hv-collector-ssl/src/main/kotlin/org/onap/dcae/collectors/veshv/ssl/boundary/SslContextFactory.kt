/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
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
package org.onap.dcae.collectors.veshv.ssl.boundary

import arrow.core.Option
import io.netty.handler.ssl.SslContext
import org.onap.dcaegen2.services.sdk.security.ssl.SslFactory

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
class SslContextFactory(private val sslFactory: SslFactory = SslFactory()) {
    fun createServerContext(secConfig: SecurityConfiguration): Option<SslContext> =
            secConfig.keys.map { sslFactory.createSecureServerContext(it) }

    fun createClientContext(secConfig: SecurityConfiguration): Option<SslContext> =
            secConfig.keys.map { sslFactory.createSecureClientContext(it) }
}
