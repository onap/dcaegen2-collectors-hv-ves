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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.config

import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingFailure
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingSuccess
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ArgDcaeAppSimConfigurationTest {

    lateinit var cut: ArgDcaeAppSimConfiguration
    val listenPort = "1234"
    val kafkaBootstrapServers = "localhosting:123,localhostinger:12345"
    val kafkaTopics = "top1,top2"

    @BeforeEach

    fun setup() {
        cut = ArgDcaeAppSimConfiguration()
    }

    @Nested

    inner class `parsing arguments` {
        lateinit var result: DcaeAppSimConfiguration

        @Nested

        inner class `all parameters are present in the long form` {

            @BeforeEach

            fun setup() {
                result = cut.parseExpectingSuccess(
                        "--listen-port", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "--kafka-topics", kafkaTopics
                )
            }

            @Test

            fun `should set proper port`() {
                assertThat(result.apiAddress.port).isEqualTo(listenPort.toInt())
            }


            @Test


            fun `should set proper kafka bootstrap servers`() {
                assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            @Test

            fun `should set proper kafka topics`() {
                assertThat(result.kafkaTopics).isEqualTo(
                        setOf("top1", "top2")
                )
            }
        }

        @Nested

        inner class `some parameters are present in the short form` {

            @BeforeEach

            fun setup() {
                result = cut.parseExpectingSuccess(
                        "-p", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "-f", kafkaTopics)
            }

            @Test

            fun `should set proper port`() {
                assertThat(result.apiAddress.port).isEqualTo(listenPort.toInt())
            }

            @Test

            fun `should set proper kafka bootstrap servers`() {
                assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            @Test

            fun `should set proper kafka topics`() {
                assertThat(result.kafkaTopics).isEqualTo(
                        setOf("top1", "top2")
                )
            }
        }

        @Nested

        inner class `required parameter is absent` {
            @Nested
            inner class `kafka topics are missing` {
                @Test
                fun `should throw exception`() {
                    assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-s", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            @Nested

            inner class `kafka bootstrap servers is missing` {
                @Test
                fun `should throw exception`() {
                    assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-f", kafkaTopics
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            @Nested

            inner class `listen port is missing` {
                @Test
                fun `should throw exception`() {
                    assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-s", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }
    }
}