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
import arrow.core.getOrElse
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
                val validatedFields = mutableSetOf(
                        validate(partial::streamPublishers)
                )
                        .union(cbsConfigurationValidation(partial))
                        .union(serverConfigurationValidation(partial))
                        .union(securityValidation(partial))

                validatedFields.optionOfValidationError()
                        .toEither { partial.validated() }
                        .swap()
            }

    fun validatedCbsConfiguration(partial: PartialConfiguration) = ValidatedCbsConfiguration(
            firstRequestDelaySec = getOrThrowValidationException(partial::firstRequestDelaySec),
            requestIntervalSec = getOrThrowValidationException(partial::requestIntervalSec)
    )

    private fun cbsConfigurationValidation(partial: PartialConfiguration) = setOf(
            validate(partial::firstRequestDelaySec),
            validate(partial::requestIntervalSec)
    )

    private fun serverConfigurationValidation(partial: PartialConfiguration) = setOf(
            validate(partial::listenPort),
            validate(partial::maxPayloadSizeBytes),
            validate(partial::idleTimeoutSec)
    )

    private fun securityValidation(partial: PartialConfiguration) =
            partial.sslDisable.fold({
                validatedSecurityConfiguration(partial)
            }, { sslDisabled ->
                if (sslDisabled) {
                    listOf(Validated.Valid("sslDisable flag is set to true"))
                } else {
                    validatedSecurityConfiguration(partial)
                }
            })

    private fun validatedSecurityConfiguration(partial: PartialConfiguration) = setOf(
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

private fun PartialConfiguration.validated() = ValidatedPartialConfiguration(
        listenPort = getOrThrowValidationException(::listenPort),
        idleTimeoutSec = getOrThrowValidationException(::idleTimeoutSec),
        maxPayloadSizeBytes = getOrThrowValidationException(::maxPayloadSizeBytes),
        cbsConfiguration = ValidatedCbsConfiguration(
                firstRequestDelaySec = getOrThrowValidationException(::firstRequestDelaySec),
                requestIntervalSec = getOrThrowValidationException(::requestIntervalSec)
        ),
        streamPublishers = getOrThrowValidationException(::streamPublishers),
        sslDisable = sslDisable,
        keyStoreFile = keyStoreFile,
        keyStorePassword = keyStorePassword,
        trustStoreFile = trustStoreFile,
        trustStorePassword = trustStorePassword,
        logLevel = logLevel
)

private fun <A> getOrThrowValidationException(property: KProperty0<Option<A>>) =
        property().getOrElse {
            throw ValidationException("Field `${property.name}` was not validated and is missing in configuration")
        }
