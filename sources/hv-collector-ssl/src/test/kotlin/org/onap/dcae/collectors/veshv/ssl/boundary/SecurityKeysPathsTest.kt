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
package org.onap.dcae.collectors.veshv.ssl.boundary

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File


internal object SecurityKeysPathsTest : Spek({
    describe("SecurityKeysPaths") {
        describe("security keys loading") {
            on("valid security data") {

                val cut = SecurityKeysPaths(
                        validKeyStorePath,
                        keyStorePass,
                        validTrustStorePath,
                        trustStorePass
                )

                it("should successfully create security keys") {
                    val securityKeys = cut.asImmutableSecurityKeys()

                    assertThat(securityKeys.keyStore().path()).isEqualTo(validKeyStorePath)
                    securityKeys.keyStorePassword().use {
                        assertThat(String(it)).isEqualTo(keyStorePass)
                    }
                    assertThat(securityKeys.trustStore().path()).isEqualTo(validTrustStorePath)
                    securityKeys.trustStorePassword().use {
                        assertThat(String(it)).isEqualTo(trustStorePass)
                    }
                }
            }

            on("invalid security data") {

                val cut = SecurityKeysPaths(
                        validKeyStorePath,
                        keyStorePass,
                        invalidTrustStorePath,
                        trustStorePass
                )

                it("should fail creating security keys") {
                    assertThatThrownBy { cut.asImmutableSecurityKeys() }
                }
            }
        }
    }
})

private val validKeyStorePath = File("key.p12").toPath()
private val validTrustStorePath = File("trust.p12").toPath()
private val invalidTrustStorePath = File("trust.p").toPath()
private const val keyStorePass = "keyPass"
private const val trustStorePass = "trustPass"
