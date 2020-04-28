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
    CONFIGURATION_FILE(required = true,
            option = option {
                shortOpt = "c"
                longOpt = "configuration-file"
                desc = "Json file containing HV-VES configuration"
                hasArgument = true
            }),
    LISTEN_PORT(required = true,
            option = option {
                shortOpt = "p"
                longOpt = "listen-port"
                desc = "Listen port"
                hasArgument = true
            }),
    VES_HV_PORT(required = true,
            option = option {
                shortOpt = "v"
                longOpt = "ves-port"
                desc = "VesHvCollector port"
                hasArgument = true
            }),
    VES_HV_HOST(required = true,
            option = option {
                shortOpt = "h"
                longOpt = "ves-host"
                desc = "VesHvCollector host"
                hasArgument = true
            }),
    KAFKA_SERVERS(required = true,
            option = option {
                shortOpt = "s"
                longOpt = "kafka-bootstrap-servers"
                desc = "Comma-separated Kafka bootstrap servers in <host>:<port> format"
                hasArgument = true
            }),
    KAFKA_TOPICS(required = true,
            option = option {
                shortOpt = "f"
                longOpt = "kafka-topics"
                desc = "Comma-separated Kafka topics"
                hasArgument = true
            }),
    KAFKA_PARTITIONS(required = true,
            option = option {
                shortOpt = "pn"
                longOpt = "kafka-partitions"
                desc = "Number of partitions"
                hasArgument = true
            }),
    HEALTH_CHECK_API_PORT(option {
        shortOpt = "H"
        longOpt = "health-check-api-port"
        desc = "Health check rest api listen port"
        hasArgument = true
    }),
    SSL_DISABLE(option {
        shortOpt = "l"
        longOpt = "ssl-disable"
        desc = "Disable SSL encryption"
    }),
    KEY_STORE_FILE(option {
        shortOpt = "k"
        longOpt = "key-store"
        desc = "Key store in PKCS12 format"
        hasArgument = true
    }),
    KEY_STORE_PASSWORD_FILE(option {
        shortOpt = "kp"
        longOpt = "key-store-password-file"
        desc = "File with key store password"
        hasArgument = true
    }),
    TRUST_STORE_FILE(option {
        shortOpt = "t"
        longOpt = "trust-store"
        desc = "File with trusted certificate bundle in PKCS12 format"
        hasArgument = true
    }),
    TRUST_STORE_PASSWORD_FILE(option {
        shortOpt = "tp"
        longOpt = "trust-store-password-file"
        desc = "File with trust store password"
        hasArgument = true
    }),
    MAXIMUM_PAYLOAD_SIZE_BYTES(option {
        shortOpt = "m"
        longOpt = "max-payload-size"
        desc = "Maximum supported payload size in bytes"
        hasArgument = true
    }),
    DISABLE_PROCESSING(option {
        shortOpt = "d"
        longOpt = "disable-processing"
        desc = "Message queue consumer option. Indicates whether messages should be fully processed"
    });

    fun environmentVariableName(prefix: String = ""): String =
            option.longOpt.toUpperCase().replace('-', '_').let { mainPart ->
                if (prefix.isNotBlank()) {
                    "${prefix}_${mainPart}"
                } else {
                    mainPart
                }
            }
}


private class OptionDSL {
    lateinit var shortOpt: String
    lateinit var longOpt: String
    lateinit var desc: String
    var hasArgument: Boolean = false
}

private fun option(conf: OptionDSL.() -> Unit): Option {
    val dsl = OptionDSL().apply(conf)
    return Option.builder(dsl.shortOpt)
            .longOpt(dsl.longOpt)
            .hasArg(dsl.hasArgument)
            .desc(dsl.desc)
            .build()
}
