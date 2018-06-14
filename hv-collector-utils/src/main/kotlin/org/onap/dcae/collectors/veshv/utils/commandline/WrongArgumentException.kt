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

import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options


class WrongArgumentException(
        message: String,
        private val options: Options,
        parent: Throwable? = null
) : Exception(message, parent) {

    constructor(par: Throwable, options: Options) : this(par.message ?: "", options, par)

    fun printMessage() {
        println(message)
    }

    fun printHelp(programName: String) {
        val formatter = HelpFormatter()
        formatter.printHelp(programName, options)
    }
}
