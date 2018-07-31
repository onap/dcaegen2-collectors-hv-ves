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
package org.onap.dcae.collectors.veshv.utils.commandline

import org.apache.commons.cli.Option


enum class CommandLineOption(val option: Option) {
    LISTEN_PORT(Option.builder("p")
            .longOpt("listen-port")
            .required()
            .hasArg()
            .desc("Listen port")
            .build()
    ),
    CONSUL_CONFIG_URL(Option.builder("c")
            .longOpt("config-url")
            .required()
            .hasArg()
            .desc("URL of ves configuration on consul")
            .build()
    ),
    CONSUL_FIRST_REQUEST_DELAY(Option.builder("d")
            .longOpt("first-request-delay")
            .hasArg()
            .desc("Delay of first request to consul in seconds")
            .build()
    ),
    CONSUL_REQUEST_INTERVAL(Option.builder("I")
            .longOpt("request-interval")
            .hasArg()
            .desc("Interval of consul configuration requests in seconds")
            .build()
    ),
    VES_HV_PORT(Option.builder("v")
            .longOpt("ves-port")
            .required()
            .hasArg()
            .desc("VesHvCollector port")
            .build()
    ),
    VES_HV_HOST(Option.builder("h")
            .longOpt("ves-host")
            .required()
            .hasArg()
            .desc("VesHvCollector host")
            .build()
    ),
    KAFKA_SERVERS(Option.builder("s")
            .longOpt("kafka-bootstrap-servers")
            .required()
            .hasArg()
            .desc("Comma-separated Kafka bootstrap servers in <host>:<port> format")
            .build()
    ),
    KAFKA_TOPICS(Option.builder("f")
            .longOpt("kafka-topics")
            .required()
            .hasArg()
            .desc("Comma-separated Kafka topics")
            .build()
    ),
    SSL_DISABLE(Option.builder("l")
            .longOpt("ssl-disable")
            .desc("Disable SSL encryption")
            .build()
    ),
    PRIVATE_KEY_FILE(Option.builder("k")
            .longOpt("private-key-file")
            .hasArg()
            .desc("File with private key in PEM format")
            .build()
    ),
    CERT_FILE(Option.builder("e")
            .longOpt("cert-file")
            .hasArg()
            .desc("File with certificate bundle")
            .build()
    ),
    TRUST_CERT_FILE(Option.builder("t")
            .longOpt("trust-cert-file")
            .hasArg()
            .desc("File with trusted certificate bundle for trusting connections")
            .build()
    ),
    IDLE_TIMEOUT_SEC(Option.builder("i")
            .longOpt("idle-timeout-sec")
            .hasArg()
            .desc("""Idle timeout for remote hosts. After given time without any data exchange the
                |connection might be closed.""".trimMargin())
            .build()
    ),
    DUMMY_MODE(Option.builder("u")
            .longOpt("dummy")
            .desc("If present will start in dummy mode (dummy external services)")
            .build()
    ),
}
