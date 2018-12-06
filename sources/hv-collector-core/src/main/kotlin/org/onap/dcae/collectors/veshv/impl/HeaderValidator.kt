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

import arrow.core.None
import arrow.core.Some
import arrow.data.NonEmptyList
import arrow.data.Validated
import arrow.data.invalid
import arrow.data.valid
import arrow.syntax.collections.tail
import com.google.protobuf.Descriptors.FieldDescriptor
import org.onap.dcae.collectors.veshv.domain.headerRequiredFieldDescriptors
import org.onap.ves.VesEventOuterClass.CommonEventHeader

class HeaderValidator {
    companion object {
        private fun <A> validate(
            header: CommonEventHeader,
            reader: HeaderReader<A>,
            field: A
        ): Validated<ValidationError, A> {
            val readingResult = reader.read(header, field)

            return when (readingResult) {
                is Some -> readingResult.t.valid()
                is None -> {
                    when (field) {
                        is FieldDescriptor -> ValidationError.MissingField(field.name).invalid()
                        is String -> when (reader) {
                            is EventListenerVersionReader -> ValidationError.VersionMatch(field).invalid()
                            else -> throw IllegalStateException("Not possible reader type")
                        }
                        else -> throw IllegalStateException("Not possible type")
                    }
                }
            }
        }

        fun parallelValidate(header: CommonEventHeader): Validated<NonEmptyList<ValidationError>, CommonEventHeader> {
            val versionValidationResult = validate(header, EventListenerVersionReader, header.vesEventListenerVersion)
            val requiredFieldsValidationResults = headerRequiredFieldDescriptors.map { fieldDescriptor ->
                validate(
                    header,
                    RequiredFieldReader,
                    fieldDescriptor
                )
            }

            val invalidFields = requiredFieldsValidationResults.filter { value -> value is Validated.Invalid }
                .map { (it as Validated.Invalid) }

            return when (versionValidationResult) {
                is Validated.Valid ->
                    if (invalidFields.isEmpty()) Validated.Valid(header)
                    else Validated.Invalid(NonEmptyList(invalidFields[0].e, invalidFields.tail().map { it.e }))
                is Validated.Invalid ->
                    if (invalidFields.isEmpty()) versionValidationResult.toValidatedNel()
                    else Validated.Invalid(
                        NonEmptyList(versionValidationResult.e, invalidFields.map { it.e })
                    )
                else -> throw IllegalStateException("Not possible value")
            }
        }
    }
}
