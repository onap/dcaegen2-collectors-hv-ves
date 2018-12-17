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
package org.onap.dcae.collectors.veshv.model

import arrow.core.None
import arrow.core.Some
import arrow.core.Option
import arrow.core.getOrElse
import io.netty.buffer.ByteBufAllocator
import org.onap.dcae.collectors.veshv.utils.logging.OnapMdc
import java.net.InetAddress
import java.security.cert.X509Certificate
import java.util.*

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
class ClientContext constructor() {

    var clientAddress: Option<InetAddress> = None
    var clientCert: Option<X509Certificate> = None

    var alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT

    val requestId: String = UUID.randomUUID().toString() // Should be somehow propagated to DMAAP
    val invocationId: String = UUID.randomUUID().toString()

    val mdc: Map<String, String>
        get() = mapOf(
                OnapMdc.CLIENT_IP to clientIp().getOrElse { DEFAULT_VALUE },
                OnapMdc.CLIENT_NAME to clientDn().getOrElse { DEFAULT_VALUE },
                OnapMdc.REQUEST_ID to requestId,
                OnapMdc.INVOCATION_ID to invocationId,
                OnapMdc.STATUS_CODE to DEFAULT_STATUS_CODE
        )

    val fullMdc: Map<String, String>
        get() = mdc + ServiceContext.mdc

    private var mdcOverride: Map<String, Option<Any>> = emptyMap()

    constructor(alloc: ByteBufAllocator) : this() {
        this.alloc = alloc
    }

    fun setMdc(map: Map<String, Option<Any>>) {
        this.mdcOverride = map
    }

    private fun clientIp(): Option<String> {
        return when (mdcOverride.containsKey(OnapMdc.CLIENT_IP)) {
            true -> mdcOverride.getOrDefault(OnapMdc.CLIENT_IP, None).fold(
                    { None },
                    { Some((it as InetAddress).hostAddress) })
            else -> None
        }
    }

    private fun clientDn(): Option<String> {
        return when (mdcOverride.containsKey(OnapMdc.CLIENT_NAME)) {
            true -> mdcOverride.getOrDefault(OnapMdc.CLIENT_NAME, None).fold(
                    { None },
                    { Some((it as X509Certificate).subjectX500Principal.toString()) })
            else -> None
        }
    }

    companion object {
        const val DEFAULT_STATUS_CODE = "INPROGRESS"
        const val DEFAULT_VALUE = ""
    }
}
