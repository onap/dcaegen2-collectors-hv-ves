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

import com.nhaarman.mockitokotlin2.mock
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier


internal object CbsClientMockSupplier : Supplier<CbsClient> {

    private val logger = Logger(CbsClientMockSupplier::class)
    private val cbsClientSupplierException = Exception("Test was configured to fail at client creation.")

    private var shouldEmitError = false
    val requestsAmount = AtomicInteger(0)
    val cbsClientMock = mock<CbsClient>()

    override fun get(): CbsClient = requestsAmount.incrementAndGet().let {
        if (shouldEmitError) {
            throw cbsClientSupplierException
        } else {
            cbsClientMock
        }
    }

    fun setIsCbsClientCreationSuccessful(isCreationSuccessful: Boolean) {
        logger.trace { "Setting CBS creation success result to : $isCreationSuccessful" }
        shouldEmitError = !isCreationSuccessful
    }

    fun throwedException(): Throwable = cbsClientSupplierException

    fun reset() {
        com.nhaarman.mockitokotlin2.reset(cbsClientMock)
        setIsCbsClientCreationSuccessful(true)
        this.requestsAmount.set(0)
    }
}
