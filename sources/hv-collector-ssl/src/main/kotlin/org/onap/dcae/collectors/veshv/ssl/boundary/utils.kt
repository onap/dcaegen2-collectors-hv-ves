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
import arrow.core.Some
import arrow.core.Try
import org.apache.commons.cli.CommandLine
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.utils.commandline.hasOption
import org.onap.dcae.collectors.veshv.utils.commandline.stringValue
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords
import java.nio.file.Path

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */

const val KEY_STORE_FILE = "/etc/ves-hv/server.p12"
const val KEY_STORE_PASSWORD_FILE = "/etc/ves-hv/server.pass"
const val TRUST_STORE_FILE = "/etc/ves-hv/trust.p12"
const val TRUST_STORE_PASSWORD_FILE = "/etc/ves-hv/trust.[ass"

fun createSecurityConfiguration(cmdLine: CommandLine): Try<SecurityConfiguration> =
        if (cmdLine.hasOption(CommandLineOption.SSL_DISABLE))
            Try { disabledSecurityConfiguration() }
        else
            enabledSecurityConfiguration(cmdLine)

private fun disabledSecurityConfiguration() = SecurityConfiguration(keys = None)

private fun enabledSecurityConfiguration(cmdLine: CommandLine) = Try {
    val ksFile = cmdLine.stringValue(CommandLineOption.KEY_STORE_FILE, KEY_STORE_FILE)
    val ksPass = cmdLine.stringValue(CommandLineOption.KEY_STORE_PASSWORD_FILE, KEY_STORE_PASSWORD_FILE)
    val tsFile = cmdLine.stringValue(CommandLineOption.TRUST_STORE_FILE, TRUST_STORE_FILE)
    val tsPass = cmdLine.stringValue(CommandLineOption.TRUST_STORE_PASSWORD_FILE, TRUST_STORE_PASSWORD_FILE)

    val keys = ImmutableSecurityKeys.builder()
            .keyStore(ImmutableSecurityKeysStore.of(pathFromFile(ksFile)))
            .keyStorePassword(Passwords.fromPath(pathFromFile(ksPass)))
            .trustStore(ImmutableSecurityKeysStore.of(pathFromFile(tsFile)))
            .trustStorePassword(Passwords.fromPath(pathFromFile(tsPass)))
            .build()

    SecurityConfiguration(keys = Some(keys))
}

private fun pathFromFile(file: String) = Path.of(file)
