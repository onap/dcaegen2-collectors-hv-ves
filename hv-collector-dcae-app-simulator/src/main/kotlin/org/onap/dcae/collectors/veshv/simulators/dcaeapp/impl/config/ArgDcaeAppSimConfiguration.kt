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

import arrow.core.ForOption
import arrow.core.Option
import arrow.core.fix
import arrow.instances.extensions
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.KAFKA_SERVERS
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.KAFKA_TOPICS
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.LISTEN_PORT

class ArgDcaeAppSimConfiguration : ArgBasedConfiguration<DcaeAppSimConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList: List<CommandLineOption> = listOf(
            LISTEN_PORT,
            KAFKA_SERVERS,
            KAFKA_TOPICS
    )

    override fun getConfiguration(cmdLine: CommandLine): Option<DcaeAppSimConfiguration> =
            ForOption extensions {
                binding {
                    val listenPort = cmdLine
                            .intValue(LISTEN_PORT)
                            .bind()
                    val kafkaBootstrapServers = cmdLine
                            .stringValue(KAFKA_SERVERS)
                            .bind()
                    val kafkaTopics = cmdLine
                            .stringValue(KAFKA_TOPICS)
                            .map { it.split(",").toSet() }
                            .bind()

                    DcaeAppSimConfiguration(
                            listenPort,
                            kafkaBootstrapServers,
                            kafkaTopics)
                }.fix()
            }
}
