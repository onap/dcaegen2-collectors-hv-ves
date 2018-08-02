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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.config

import arrow.core.Failure
import arrow.core.Success
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.utils.commandline.WrongArgumentException


internal class ArgBasedDcaeAppSimConfigurationTest : Spek({

    lateinit var cut: ArgBasedDcaeAppSimConfiguration
    val kafkaBootstrapServers = "localhosting:123,localhostinger:12345"
    val kafkaTopics = "top1,top2"

    beforeEachTest {
        cut = ArgBasedDcaeAppSimConfiguration()
    }

    fun parseExpectingSuccess(vararg cmdLine: String): DcaeAppSimConfiguration {
        val result = cut.parse(cmdLine)
        return when (result) {
            is Success -> result.value
            is Failure -> throw AssertionError("Parsing result should be present")
        }
    }

    fun parseExpectingFailure(vararg cmdLine: String): Throwable {
        val result = cut.parse(cmdLine)
        return when (result) {
            is Success -> throw AssertionError("parsing should have failed")
            is Failure -> result.exception
        }
    }

    describe("parsing arguments") {
        lateinit var result: DcaeAppSimConfiguration

        given("all parameters are present in the long form") {

            beforeEachTest {
                result = parseExpectingSuccess("--listen-port", "6969",
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "--kafka-topics", kafkaTopics
                )
            }

            it("should set proper port") {
                assertThat(result.apiPort).isEqualTo(6969)
            }


            it("should set proper kafka boostrap servers") {
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
                result = parseExpectingSuccess("-p", "666",
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "-f", kafkaTopics)
            }

            it("should set proper port") {
                assertThat(result.apiPort).isEqualTo(666)
            }

            it("should set proper kafka boostrap servers") {
                assertThat(result.kafkaBootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper kafka topics") {
                assertThat(result.kafkaTopics).isEqualTo(
                        setOf("top1", "top2")
                )
            }
        }

        given("all optional parameters are absent") {

            beforeEachTest {
                result = parseExpectingSuccess("-s", kafkaBootstrapServers, "-f", kafkaTopics)
            }

            it("should set default port") {
                assertThat(result.apiPort).isEqualTo(DefaultValues.API_PORT)
            }
        }

        describe("required parameter is absent") {
            given("kafka topics are missing") {
                it("should throw exception") {
                    assertThat(parseExpectingFailure("-s", kafkaBootstrapServers))
                            .isInstanceOf(WrongArgumentException::class.java)
                }
            }

            given("kafka bootstrap servers are missing") {
                it("should throw exception") {
                    assertThat(parseExpectingFailure("-f", kafkaTopics))
                            .isInstanceOf(WrongArgumentException::class.java)
                }
            }
        }
    }
})