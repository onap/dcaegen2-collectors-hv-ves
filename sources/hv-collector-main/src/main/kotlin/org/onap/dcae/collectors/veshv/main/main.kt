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
package org.onap.dcae.collectors.veshv.main

import org.onap.dcae.collectors.veshv.config.api.ConfigurationModule
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.main.servers.HealthCheckServer
import org.onap.dcae.collectors.veshv.main.servers.VesServer
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.registerShutdownHook
import reactor.core.scheduler.Schedulers
import java.util.concurrent.atomic.AtomicReference


private const val VES_HV_PACKAGE = "org.onap.dcae.collectors.veshv"
private val logger = Logger("$VES_HV_PACKAGE.main")

private val hvVesServer = AtomicReference<ServerHandle>()
private val configurationModule = ConfigurationModule()

fun main(args: Array<String>) {
    val configStateListener = object : ConfigurationStateListener {
        override fun retryingForConfiguration() {
            HealthState.INSTANCE.changeState(HealthDescription.RETRYING_FOR_DYNAMIC_CONFIGURATION)
        }

    }

    HealthCheckServer.start(configurationModule.healthCheckPort(args))
    ConfigurationModule()
            .hvVesConfigurationUpdates(args, configStateListener, ServiceContext::mdc)
    configurationModule
            .hvVesConfigurationUpdates(args)
            .publishOn(Schedulers.single(Schedulers.elastic()))
            .doOnNext(::startServer)
            .doOnError(::logServerStartFailed)
            .neverComplete() // TODO: remove after merging configuration stream with cbs
            .block()
}

private fun startServer(config: HvVesConfiguration) {
    stopRunningServer()
    Logger.setLogLevel(VES_HV_PACKAGE, config.logLevel)
    logger.info { "Using configuration: $config" }

    VesServer.start(config).let {
        registerShutdownHook { shutdownGracefully(it) }
        hvVesServer.set(it)
    }
}

private fun stopRunningServer() = hvVesServer.get()?.close()?.unsafeRunSync()

internal fun shutdownGracefully(serverHandle: ServerHandle,
                                healthState: HealthState = HealthState.INSTANCE) {
    logger.debug(ServiceContext::mdc) { "Graceful shutdown started" }
    healthState.changeState(HealthDescription.SHUTTING_DOWN)
    serverHandle.close().unsafeRunSync()
    logger.info(ServiceContext::mdc) { "Graceful shutdown completed" }
}

private fun logServerStartFailed(ex: Throwable) =
        logger.withError(ServiceContext::mdc) { log("Failed to start a server", ex) }

