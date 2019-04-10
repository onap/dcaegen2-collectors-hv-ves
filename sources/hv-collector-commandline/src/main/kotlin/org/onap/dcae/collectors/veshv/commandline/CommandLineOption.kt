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
package org.onap.dcae.collectors.veshv.commandline

import org.apache.commons.cli.Option


enum class CommandLineOption(val option: Option, val required: Boolean = false) {
    HEALTH_CHECK_API_PORT(
            Option.builder("H")
                    .longOpt("health-check-api-port")
                    .hasArg()
                    .desc("Health check rest api listen port")
                    .build()
    ),
    CONFIGURATION_FILE(
            Option.builder("c")
                    .longOpt("configuration-file")
                    .hasArg()
                    .desc("Json file containing HV-VES configuration")
                    .build(),
            required = true
    ),
    LISTEN_PORT(
            Option.builder("p")
                    .longOpt("listen-port")
                    .hasArg()
                    .desc("Listen port")
                    .build(),
            required = true
    ),
    VES_HV_PORT(
            Option.builder("v")
                    .longOpt("ves-port")
                    .hasArg()
                    .desc("VesHvCollector port")
                    .build(),
            required = true
    ),
    VES_HV_HOST(
            Option.builder("h")
                    .longOpt("ves-host")
                    .hasArg()
                    .desc("VesHvCollector host")
                    .build(),
            required = true
    ),
    KAFKA_SERVERS(
            Option.builder("s")
                    .longOpt("kafka-bootstrap-servers")
                    .hasArg()
                    .desc("Comma-separated Kafka bootstrap servers in <host>:<port> format")
                    .build(),
            required = true
    ),
    KAFKA_TOPICS(
            Option.builder("f")
                    .longOpt("kafka-topics")
                    .hasArg()
                    .desc("Comma-separated Kafka topics")
                    .build(),
            required = true
    ),
    SSL_DISABLE(
            Option.builder("l")
                    .longOpt("ssl-disable")
                    .desc("Disable SSL encryption")
                    .build()
    ),
    KEY_STORE_FILE(
            Option.builder("k")
                    .longOpt("key-store")
                    .hasArg()
                    .desc("Key store in PKCS12 format")
                    .build()
    ),
    KEY_STORE_PASSWORD_FILE(
            Option.builder("kp")
                    .longOpt("key-store-password-file")
                    .hasArg()
                    .desc("File with key store password")
                    .build()
    ),
    TRUST_STORE_FILE(
            Option.builder("t")
                    .longOpt("trust-store")
                    .hasArg()
                    .desc("File with trusted certificate bundle in PKCS12 format")
                    .build()
    ),
    TRUST_STORE_PASSWORD_FILE(
            Option.builder("tp")
                    .longOpt("trust-store-password-file")
                    .hasArg()
                    .desc("File with trust store password")
                    .build()
    ),
    MAXIMUM_PAYLOAD_SIZE_BYTES(
            Option.builder("m")
                    .longOpt("max-payload-size")
                    .hasArg()
                    .desc("Maximum supported payload size in bytes")
                    .build()
    );

    fun environmentVariableName(prefix: String = DEFAULT_ENV_PREFIX): String =
            option.longOpt.toUpperCase().replace('-', '_').let { mainPart ->
                "${prefix}_${mainPart}"
            }

    companion object {
        private const val DEFAULT_ENV_PREFIX = "VESHV"
    }
}
