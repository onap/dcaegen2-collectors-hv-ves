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
package org.onap.dcae.collectors.veshv.ssl.boundary

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.fix
import arrow.core.monad
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.utils.commandline.hasOption
import org.onap.dcae.collectors.veshv.utils.commandline.stringValue
import java.io.File

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */


const val KEY_STORE_FILE = "/etc/ves-hv/server.p12"
const val TRUST_STORE_FILE = "/etc/ves-hv/trust.p12"

fun createSecurityConfiguration(cmdLine: CommandLine): Option<SecurityConfiguration> {
    val sslDisable = cmdLine.hasOption(CommandLineOption.SSL_DISABLE)

    return if (sslDisable) disabledSecurityConfiguration(sslDisable) else enabledSecurityConfiguration(cmdLine)
}

private fun disabledSecurityConfiguration(sslDisable: Boolean): Some<SecurityConfiguration> {
    return Some(SecurityConfiguration(
            sslDisable = sslDisable,
            keys = None
    ))
}

private fun enabledSecurityConfiguration(cmdLine: CommandLine): Option<SecurityConfiguration> {
    return Option.monad().binding {
        val ksFile = cmdLine.stringValue(CommandLineOption.KEY_STORE_FILE, KEY_STORE_FILE)
        val ksPass = cmdLine.stringValue(CommandLineOption.KEY_STORE_PASSWORD).bind()
        val tsFile = cmdLine.stringValue(CommandLineOption.TRUST_STORE_FILE, TRUST_STORE_FILE)
        val tsPass = cmdLine.stringValue(CommandLineOption.TRUST_STORE_PASSWORD).bind()

        val keys = JdkKeys(
                keyStore = streamFromFile(ksFile),
                keyStorePassword = ksPass.toCharArray(),
                trustStore = streamFromFile(tsFile),
                trustStorePassword = tsPass.toCharArray()
        )

        SecurityConfiguration(
                sslDisable = false,
                keys = Some(keys)
        )
    }.fix()
}

private fun streamFromFile(file: String) = { File(file).inputStream() }
