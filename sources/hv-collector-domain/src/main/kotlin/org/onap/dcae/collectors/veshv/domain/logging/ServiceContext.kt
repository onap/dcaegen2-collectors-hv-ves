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
package org.onap.dcae.collectors.veshv.domain.logging

import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
object ServiceContext {
    val instanceId = UUID.randomUUID().toString()
    val serverFqdn = getHost().hostName!!

    val mdc = mapOf(
            OnapMdc.INSTANCE_ID to instanceId,
            OnapMdc.SERVER_FQDN to serverFqdn
    )

    private fun getHost() = try {
        InetAddress.getLocalHost()
    } catch (ex: UnknownHostException) {
        InetAddress.getLoopbackAddress()
    }
}
