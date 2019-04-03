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

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import io.netty.buffer.ByteBufAllocator
import java.net.InetAddress
import java.security.cert.X509Certificate
import java.util.*

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
data class ClientContext(
        val alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT,
        var clientAddress: Option<InetAddress> = None,
        var clientCert: Option<X509Certificate> = None,
        val requestId: String = UUID.randomUUID().toString(), // Should be somehow propagated to DMAAP
        val invocationId: String = UUID.randomUUID().toString()) {

    val mdc: Map<String, String>
        get() = mapOf(
                OnapMdc.REQUEST_ID to requestId,
                OnapMdc.INVOCATION_ID to invocationId,
                OnapMdc.STATUS_CODE to DEFAULT_STATUS_CODE,
                OnapMdc.CLIENT_NAME to clientDn().getOrElse { DEFAULT_VALUE },
                OnapMdc.CLIENT_IP to clientIp().getOrElse { DEFAULT_VALUE }
        )

    val fullMdc: Map<String, String>
        get() = mdc + ServiceContext.mdc

    private fun clientDn(): Option<String> = clientCert.map { it.subjectX500Principal.toString() }
    private fun clientIp(): Option<String> = clientAddress.map(InetAddress::getHostAddress)

    companion object {
        const val DEFAULT_STATUS_CODE = "INPROGRESS"
        const val DEFAULT_VALUE = ""
    }
}
