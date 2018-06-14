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

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.config.DefaultValues.API_PORT
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.KAFKA_SERVERS
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.KAFKA_TOPICS
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.LISTEN_PORT

internal object DefaultValues {
    const val API_PORT = 8080
    const val KAFKA_SERVERS = "kafka:9092"
    const val KAFKA_TOPICS = "ves_hvRanMeas"
}

class ArgBasedDcaeAppSimConfiguration : ArgBasedConfiguration<DcaeAppSimConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList: List<CommandLineOption> = listOf(
            LISTEN_PORT,
            KAFKA_SERVERS,
            KAFKA_TOPICS
    )

    override fun getConfiguration(cmdLine: CommandLine): DcaeAppSimConfiguration {
        val port = cmdLine.intValue(LISTEN_PORT, API_PORT)
        val kafkaBootstrapServers = cmdLine.stringValue(KAFKA_SERVERS, DefaultValues.KAFKA_SERVERS)
        val kafkaTopics = cmdLine.stringValue(KAFKA_TOPICS, DefaultValues.KAFKA_TOPICS).split(",").toSet()
        return DcaeAppSimConfiguration(
                port,
                kafkaBootstrapServers,
                kafkaTopics)
    }

}
