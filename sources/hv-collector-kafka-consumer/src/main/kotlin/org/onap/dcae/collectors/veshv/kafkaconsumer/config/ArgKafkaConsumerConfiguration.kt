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

import arrow.core.Option
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.commandline.stringValue
import org.onap.dcae.collectors.veshv.utils.arrow.OptionUtils.binding


internal class ArgKafkaConsumerConfiguration : ArgBasedConfiguration<KafkaConsumerConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList: List<CommandLineOption> = listOf(CommandLineOption.KAFKA_SERVERS,
            CommandLineOption.KAFKA_TOPICS)

    override fun getConfiguration(cmdLine: CommandLine): Option<KafkaConsumerConfiguration> =
            binding {
                val kafkaServers = cmdLine.stringValue(CommandLineOption.KAFKA_SERVERS).bind()
                val kafkaTopics = cmdLine
                        .stringValue(CommandLineOption.KAFKA_TOPICS)
                        .map { it.split(",").toSet() }
                        .bind()
                KafkaConsumerConfiguration(kafkaServers, kafkaTopics)
            }
}
