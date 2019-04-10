/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.data.Invalid
import arrow.data.Validated
import org.onap.dcae.collectors.veshv.config.api.model.ValidationException
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import kotlin.reflect.KProperty0

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since March 2019
 */
internal class ConfigurationValidator {

    fun validate(partial: PartialConfiguration): Either<ValidationException, ValidatedPartialConfiguration> =
            logger.info { "About to validate configuration: $partial" }.let {
                val validatedFields = setOf(
                        validate(partial::streamPublishers)
                )
                        .union(validatedCbsConfiguration(partial))
                        .union(validatedServerConfiguration(partial))
                        .union(validatedSecurity(partial))

                validatedFields.optionOfValidationError()
                        .toEither { partial.validated() }
                        .swap()
            }

    fun validatedCbsConfiguration(partial: PartialConfiguration) = listOf(
            validate(partial::firstRequestDelaySec),
            validate(partial::requestIntervalSec)
    )

    private fun validatedServerConfiguration(partial: PartialConfiguration) = listOf(
            validate(partial::listenPort),
            validate(partial::maxPayloadSizeBytes),
            validate(partial::idleTimeoutSec)
    )

    private fun validatedSecurity(partial: PartialConfiguration) =
            partial.sslDisable.fold({
                validatedSecurityConfiguration(partial)
            }, { sslDisabled ->
                if (sslDisabled) {
                    listOf(Validated.Valid("sslDisable flag is set to true"))
                } else {
                    validatedSecurityConfiguration(partial)
                }
            })

    private fun validatedSecurityConfiguration(partial: PartialConfiguration) = listOf(
            validate(partial::keyStoreFile),
            validate(partial::keyStorePassword),
            validate(partial::trustStoreFile),
            validate(partial::trustStorePassword)
    )


    private fun <A> validate(property: KProperty0<Option<A>>) =
            Validated.fromOption(property.get(), { "- missing property: ${property.name}\n" })

    companion object {
        private val logger = Logger(ConfigurationValidator::class)

        fun <A> validationMessageFrom(invalidFields: List<Validated<String, A>>): String =
                "Some required configuration properties are missing: \n" +
                        invalidFields.map { it as Invalid }
                                .map { it.e }
                                .fold("", String::plus)
    }
}

internal fun <A> Collection<Validated<String, A>>.optionOfValidationError(): Option<ValidationException> {
    val invalidFields = this.filter { it.isInvalid }

    if (invalidFields.isNotEmpty()) {
        val validationMessage = ConfigurationValidator.validationMessageFrom(invalidFields)
        return Some(ValidationException(validationMessage))
    }
    return None
}
