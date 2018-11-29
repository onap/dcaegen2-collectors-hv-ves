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
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.headerRequiredFieldDescriptors
import org.onap.dcae.collectors.veshv.domain.vesEventListenerVersionRegex
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.ves.VesEventOuterClass.CommonEventHeader

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
            if (message.isValid()) {
                Either.right { "Protocol buffers message is valid" }
            } else {
                Either.left { "Unsupported protocol buffers message." }
            }

    fun VesMessage.isValid() = allMandatoryFieldsArePresent(this.header)
            .and(vesEventListenerVersionRegex.matches(header.vesEventListenerVersion))

    private fun allMandatoryFieldsArePresent(header: CommonEventHeader) =
            headerRequiredFieldDescriptors
                    .all { fieldDescriptor -> header.hasField(fieldDescriptor) }

}
