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
import com.google.protobuf.Descriptors
import org.onap.ves.VesEventOuterClass

data class HeaderValidator(val header: VesEventOuterClass.CommonEventHeader) {

    fun <A> validate(read: HeaderReader<A>, field: A): Validated<ValidationError, A> {
        val s = read.read(header, field)

        return when (s) {
            is Some -> s.t.valid()
            is None -> {
                when (field) {
                    is Descriptors.FieldDescriptor -> ValidationError.MissingField(field.name).invalid()
                    is String -> ValidationError.VersionMatch(field).invalid()
                    else -> throw IllegalStateException("Not possible type")
                }
            }
        }
    }

    fun parallelValidate(
        v1: Validated<ValidationError, String>,
        v2: List<Validated<ValidationError, Descriptors.FieldDescriptor>>
    ): Validated<NonEmptyList<ValidationError>, VesEventOuterClass.CommonEventHeader> {
        val invalidFields = v2.filter { value -> value is Validated.Invalid }.map { (it as Validated.Invalid) }

        return when (v1) {
            is Validated.Valid ->
                if (invalidFields.isEmpty()) Validated.Valid(header)
                else {
                    val size = invalidFields.size
                    val tail = if (size > 1) invalidFields.subList(
                        1,
                        size - 1
                    ) else emptyList()

                    Validated.Invalid(
                        NonEmptyList(
                            invalidFields[0].e,
                            tail.map { it.e }
                        )
                    )
                }
            is Validated.Invalid ->
                if (invalidFields.isEmpty()) v1.toValidatedNel()
                else Validated.Invalid(
                    NonEmptyList(v1.e, invalidFields.map { it.e })
                )
            else -> throw IllegalStateException("Not possible value")
        }
    }
}
