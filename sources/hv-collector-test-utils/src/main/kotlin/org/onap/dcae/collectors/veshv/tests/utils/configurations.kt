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
package org.onap.dcae.collectors.veshv.tests.utils

import arrow.core.identity
import org.onap.dcae.collectors.veshv.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError


fun <T> ArgBasedConfiguration<T>.parseExpectingSuccess(vararg cmdLine: String): T =
        parse(cmdLine).fold(
                { throw AssertionError("Parsing result should be present") },
                ::identity
        )

fun <T> ArgBasedConfiguration<T>.parseExpectingFailure(vararg cmdLine: String): WrongArgumentError =
        parse(cmdLine).fold(
                ::identity,
                { throw AssertionError("parsing should have failed") }
        )
