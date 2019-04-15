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
import arrow.data.NonEmptyList
import com.google.protobuf.Descriptors
import org.onap.dcae.collectors.veshv.domain.headerRequiredFieldDescriptors
import org.onap.dcae.collectors.veshv.domain.vesEventListenerVersionRegex
import org.onap.ves.VesEventOuterClass.CommonEventHeader

internal typealias Validator = (CommonEventHeader) -> List<ValidationError>

internal object HeaderValidator {
    private val validators = (listOf(validateEventListenerVersion()) +
            headerRequiredFieldDescriptors.map { fieldDescriptor ->
                validateRequiredField(fieldDescriptor)
            })


    fun validate(header: CommonEventHeader): Either<Nel<ValidationError>, CommonEventHeader> {
        val result: List<ValidationError> = validators.flatMap { it(header) }

        return Either.cond(result.isEmpty(), { header }, { NonEmptyList.fromListUnsafe(result) })
    }

    private fun validateEventListenerVersion(): Validator = { header: CommonEventHeader ->
        if (!vesEventListenerVersionRegex.matches(header.vesEventListenerVersion))
            listOf(ValidationError.VersionMismatch(header.vesEventListenerVersion))
        else
            emptyList()
    }

    private fun validateRequiredField(requiredField: Descriptors.FieldDescriptor): Validator =
        { header: CommonEventHeader ->
            if (!header.hasField(requiredField))
                listOf(ValidationError.MissingField(requiredField.name))
            else
                emptyList()
        }
}
