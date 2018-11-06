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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.config

import arrow.core.Option
import arrow.core.fix
import arrow.instances.option.monad.monad
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.ssl.boundary.createSecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.KEY_STORE_FILE
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.KEY_STORE_PASSWORD
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.LISTEN_PORT
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.MAXIMUM_PAYLOAD_SIZE_BYTES
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.SSL_DISABLE
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.TRUST_STORE_FILE
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.TRUST_STORE_PASSWORD
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.VES_HV_HOST
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.VES_HV_PORT
import org.onap.dcae.collectors.veshv.utils.commandline.intValue
import org.onap.dcae.collectors.veshv.utils.commandline.stringValue


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class ArgXnfSimulatorConfiguration : ArgBasedConfiguration<SimulatorConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            VES_HV_PORT,
            VES_HV_HOST,
            LISTEN_PORT,
            MAXIMUM_PAYLOAD_SIZE_BYTES,
            SSL_DISABLE,
            KEY_STORE_FILE,
            KEY_STORE_PASSWORD,
            TRUST_STORE_FILE,
            TRUST_STORE_PASSWORD)

    override fun getConfiguration(cmdLine: CommandLine): Option<SimulatorConfiguration> =
            Option.monad().binding {
                val listenPort = cmdLine.intValue(LISTEN_PORT).bind()
                val vesHost = cmdLine.stringValue(VES_HV_HOST).bind()
                val vesPort = cmdLine.intValue(VES_HV_PORT).bind()
                val maxPayloadSizeBytes = cmdLine.intValue(MAXIMUM_PAYLOAD_SIZE_BYTES, WireFrameMessage.DEFAULT_MAX_PAYLOAD_SIZE_BYTES)

                SimulatorConfiguration(
                        listenPort,
                        vesHost,
                        vesPort,
                        maxPayloadSizeBytes,
                        createSecurityConfiguration(cmdLine).bind())
            }.fix()


}
