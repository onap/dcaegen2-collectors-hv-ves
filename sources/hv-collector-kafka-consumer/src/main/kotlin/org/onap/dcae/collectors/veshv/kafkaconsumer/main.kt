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
package org.onap.dcae.collectors.veshv.kafkaconsumer

import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.dcae.collectors.veshv.commandline.handleWrongArgumentErrorCurried
import org.onap.dcae.collectors.veshv.kafka.api.KafkaPropertiesFactory
import org.onap.dcae.collectors.veshv.kafkaconsumer.config.ArgKafkaConsumerConfiguration
import org.onap.dcae.collectors.veshv.kafkaconsumer.config.KafkaConsumerConfiguration
import org.onap.dcae.collectors.veshv.kafkaconsumer.impl.OffsetKafkaConsumer
import org.onap.dcae.collectors.veshv.kafkaconsumer.impl.ProcessingKafkaConsumer
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.MicrometerMetrics
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.http.PrometheusApiServer
import org.onap.dcae.collectors.veshv.utils.process.ExitCode
import org.onap.dcae.collectors.veshv.utils.process.ExitSuccess

private const val PACKAGE_NAME = "org.onap.dcae.collectors.veshv.kafkaconsumer"
const val PROGRAM_NAME = "java $PACKAGE_NAME.MainKt"

fun main(args: Array<String>): Unit =
        ArgKafkaConsumerConfiguration().parse(args)
                .fold(handleWrongArgumentErrorCurried(PROGRAM_NAME), ::startApp)
                .let(ExitCode::doExit)


private fun startApp(config: KafkaConsumerConfiguration): ExitSuccess {
    val kafkaConsumer = KafkaConsumer<ByteArray, ByteArray>(KafkaPropertiesFactory.create(
            config.kafkaBootstrapServers)
    )

        runBlocking {
            if (config.disableProcessing) {
                OffsetKafkaConsumer(kafkaConsumer, config.kafkaTopics, MicrometerMetrics.INSTANCE)
                        .start()
            } else {
                ProcessingKafkaConsumer(kafkaConsumer, config.kafkaTopics, MicrometerMetrics.INSTANCE)
                        .start()
            }
        }

    PrometheusApiServer(config.apiAddress, MicrometerMetrics.INSTANCE)
            .start().block()!!.await().block() // TODO refactor netty server logic

    return ExitSuccess
}
