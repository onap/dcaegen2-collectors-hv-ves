/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * Copyright (C) 2026 Deutsche Telekom AG
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

import arrow.core.Some
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import java.net.InetAddress
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
internal class ClientContextTest {
    @Nested
    inner class `ClientContext tests` {
        @Nested
        inner class `default instance` {
            val cut = ClientContext()

            @Nested

            inner class `mapped diagnostic context` {
                val mdc = cut.mdc

                @Test

                fun `should contain ${OnapMdc REQUEST_ID}`() {
                    assertThat(mdc[OnapMdc.REQUEST_ID]).isEqualTo(cut.requestId)
                }

                @Test

                fun `should contain ${OnapMdc INVOCATION_ID}`() {
                    assertThat(mdc[OnapMdc.INVOCATION_ID]).isEqualTo(cut.invocationId)
                }

                @Test

                fun `should contain ${OnapMdc STATUS_CODE}`() {
                    assertThat(mdc[OnapMdc.STATUS_CODE]).isEqualTo("INPROGRESS")
                }

                @Test

                fun `should contain ${OnapMdc CLIENT_NAME}`() {
                    assertThat(mdc[OnapMdc.CLIENT_NAME]).isBlank()
                }

                @Test

                fun `should contain ${OnapMdc CLIENT_IP}`() {
                    assertThat(mdc[OnapMdc.CLIENT_IP]).isBlank()
                }
            }
        }

        @Nested

        inner class `instance with client data` {
            val clientDn = "C=PL, O=Nokia, CN=NokiaBTS"
            val clientIp = "192.168.52.34"
            val cert: X509Certificate = mock()
            val principal: X500Principal = mock()
            val cut = ClientContext(
                    clientAddress = Some(InetAddress.getByName(clientIp)),
                    clientCert = Some(cert))

            init {
            whenever(cert.subjectX500Principal).thenReturn(principal)

            whenever(principal.toString()).thenReturn(clientDn)
            }
            @Nested

            inner class `mapped diagnostic context` {
                val mdc = cut.mdc

                @Test

                fun `should contain ${OnapMdc CLIENT_NAME}`() {
                    assertThat(mdc[OnapMdc.CLIENT_NAME]).isEqualTo(clientDn)
                }

                @Test

                fun `should contain ${OnapMdc CLIENT_IP}`() {
                    assertThat(mdc[OnapMdc.CLIENT_IP]).isEqualTo(clientIp)
                }
            }
        }
    }
}
