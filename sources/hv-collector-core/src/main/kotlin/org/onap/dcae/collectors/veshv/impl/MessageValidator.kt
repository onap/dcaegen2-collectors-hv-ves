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
package org.onap.dcae.collectors.veshv.impl

import arrow.core.Either
import arrow.data.Nel
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage

typealias ValidationFailMessage = () -> String
typealias ValidationSuccessMessage = () -> String
typealias ValidationResult = Either<ValidationFailMessage, ValidationSuccessMessage>

internal object MessageValidator {

    fun validateFrameMessage(message: WireFrameMessage): ValidationResult =
        message.validate().fold({
            Either.left { "Invalid wire frame header, reason: ${it.message}" }
        }, {
            Either.right { "Wire frame header is valid" }
        })

    fun validateProtobufMessage(message: VesMessage): ValidationResult =
        HeaderValidator.validate(message.header).fold(
            { validationErrors: Nel<ValidationError> ->
                Either.left {
                    "Protocol buffer message is invalid, reasons:" + validationErrors.all
                        .joinToString(prefix = "\n-") { it.errorMessage }
                }
            },
            {
                Either.right { "Protocol buffers message is valid" }
            })
}
