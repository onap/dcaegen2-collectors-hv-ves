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
package org.onap.dcae.collectors.veshv.kafkaconsumer.config

import org.assertj.core.api.Assertions
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingFailure
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingSuccess

private const val listenPort = "1234"
private const val kafkaBootstrapServers = "localhost:1234,10.1.14.10:8090"
private const val T1 = "boring_topic"
private const val T2 = "exciting_topic"
private const val kafkaTopicsString = "$T1,$T2"
private val PARSED_TOPICS_SET = setOf(T1, T2)

internal object ArgKafkaConsumerConfigurationTest : Spek({
    lateinit var cut: ArgKafkaConsumerConfiguration

    beforeEachTest {
        cut = ArgKafkaConsumerConfiguration()
    }

    describe("parsing arguments") {
        lateinit var result: KafkaConsumerConfiguration

        given("all parameters are present in the long form") {

            beforeEachTest {
                result = cut.parseExpectingSuccess(
                        "--listen-port", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "--kafka-topics", kafkaTopicsString,
                        "--disable-processing"
                )
            }

            it("should set proper port") {
                Assertions.assertThat(result.apiAddress.port).isEqualTo(listenPort.toInt())
            }

            it("should set proper kafka bootstrap servers") {
                Assertions.assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper kafka topics") {
                Assertions.assertThat(result.kafkaTopics).isEqualTo(PARSED_TOPICS_SET)
            }

            it("should disable processing") {
                Assertions.assertThat(result.disableProcessing).isTrue()
            }
        }

        given("some parameters are present in the short form") {

            beforeEachTest {
                result = cut.parseExpectingSuccess(
                        "--listen-port", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "--kafka-topics", kafkaTopicsString)
            }

            it("should set proper port") {
                Assertions.assertThat(result.apiAddress.port).isEqualTo(listenPort.toInt())
            }

            it("should set proper kafka bootstrap servers") {
                Assertions.assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper kafka topics") {
                Assertions.assertThat(result.kafkaTopics).isEqualTo(PARSED_TOPICS_SET)
            }
        }

        given("some missing disable-processing flag") {
            beforeEachTest {
                result = cut.parseExpectingSuccess(
                        "-p", listenPort,
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "-f", kafkaTopicsString)
            }

            it("should NOT disable processing") {
                Assertions.assertThat(result.disableProcessing).isFalse()
            }
        }

        describe("required parameter is absent") {
            given("kafka topics are missing") {
                it("should throw exception") {
                    Assertions.assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-T1", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("kafka bootstrap servers is missing") {
                it("should throw exception") {
                    Assertions.assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-f", kafkaTopicsString
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("listen port is missing") {
                it("should throw exception") {
                    Assertions.assertThat(cut.parseExpectingFailure(
                            "-p", listenPort,
                            "-T1", kafkaBootstrapServers
                    )).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }
    }
})
