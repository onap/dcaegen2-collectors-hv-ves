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
package org.onap.dcae.collectors.veshv.kafka.config

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingFailure
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingSuccess


internal class ArgDcaeAppSimConfigurationTest : Spek({

    lateinit var cut: ArgDcaeAppSimConfiguration
    val listenPort = "1234"
    val kafkaBootstrapServers = "localhosting:123,localhostinger:12345"
    val kafkaTopics = "top1,top2"

    beforeEachTest {
        cut = ArgDcaeAppSimConfiguration()
    }

    describe("parsing arguments") {
        lateinit var result: DcaeAppSimConfiguration

        given("all parameters are present in the long form") {

            beforeEachTest {
                result = cut.parseExpectingSuccess(
                        "--listen-port", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "--kafka-topics", kafkaTopics
                )
            }

            it("should set proper port") {
                assertThat(result.apiAddress.port).isEqualTo(listenPort.toInt())
            }


            it("should set proper kafka bootstrap servers") {
                assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper kafka topics") {
                assertThat(result.kafkaTopics).isEqualTo(
                        setOf("top1", "top2")
                )
            }
        }

        given("some parameters are present in the short form") {

            beforeEachTest {
                result = cut.parseExpectingSuccess(
                        "-p", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "-f", kafkaTopics)
            }

            it("should set proper port") {
                assertThat(result.apiAddress.port).isEqualTo(listenPort.toInt())
            }

            it("should set proper kafka bootstrap servers") {
                assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper kafka topics") {
                assertThat(result.kafkaTopics).isEqualTo(
                        setOf("top1", "top2")
                )
            }
        }

        describe("required parameter is absent") {
            given("kafka topics are missing") {
                it("should throw exception") {
                    assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-s", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("kafka bootstrap servers is missing") {
                it("should throw exception") {
                    assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-f", kafkaTopics
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("listen port is missing") {
                it("should throw exception") {
                    assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-s", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }
    }
})