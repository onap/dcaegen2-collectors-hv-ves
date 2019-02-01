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

import arrow.core.*
import arrow.instances.option.monad.monad
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.domain.SslKeys
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.utils.commandline.hasOption
import org.onap.dcae.collectors.veshv.utils.commandline.stringValue
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.Password
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.io.File
import java.net.URI
import java.nio.file.Paths

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

fun extractSecurity(config: SecurityConfiguration): SecurityKeys? {
//    Problem here is that SecurityKeys need a Paths to stores however SecurityConfiguration holds
//    InputStreams
    return if (config.sslDisable) {
        null
    } else {
        val securityKeys: SslKeys? = config.keys.getOrElse { null }
        if (securityKeys as? JdkKeys != null)
            ImmutableSecurityKeys.builder()
                    .keyStore(securityKeys.keyStore)
                    .keyStorePassword(Password(securityKeys.keyStorePassword))
                    .trustStore(securityKeys.trustStore)
                    .trustStorePassword(Password(securityKeys.trustStorePassword))
                    .build()
        else null

    }

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
                keyStore = Paths.get(URI.create(ksFile)),
                keyStorePassword = ksPass.toCharArray(),
                trustStore = Paths.get(URI.create(tsFile)),
                trustStorePassword = tsPass.toCharArray()
        )

        SecurityConfiguration(
                sslDisable = false,
                keys = Some(keys)
        )
    }.fix()
}

private fun streamFromFile(file: String) = { File(file).inputStream() }
