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
import arrow.core.Left
import arrow.core.Right
import arrow.data.Invalid
import arrow.data.Validated
import org.onap.dcae.collectors.veshv.config.api.model.ValidationException
import org.onap.dcae.collectors.veshv.utils.arrow.flatFold
import org.onap.dcae.collectors.veshv.utils.logging.Logger


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since March 2019
 */
internal class ConfigurationValidator {

    fun validate(partial: PartialConfiguration): Either<ValidationException, ValidatedPartialConfiguration> =
            logger.info { "About to validate configuration: $partial" }.let {
                val invalidFields = mutableSetOf(
                        validate(partial::streamPublishers)
                )
                        .union(cbsConfigurationValidation(partial))
                        .union(serverConfigurationValidation(partial))
                        .union(securityValidation(partial))
                        .filter { it.isInvalid }

                if (invalidFields.isNotEmpty()) {
                    return Left(ValidationException(validationMessageFrom(invalidFields)))
                }

                Right(partial.forceValidated())
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
            validate(partial::idleTimeoutSec)
    )

    private fun securityValidation(partial: PartialConfiguration) =
            partial.sslDisable.flatFold({
                validatedSecurityConfiguration(partial)
            }, {
                setOf(Validated.Valid("sslDisable flag is set to true"))
            })

    private fun validatedSecurityConfiguration(partial: PartialConfiguration) = setOf(
            validate(partial::keyStoreFile),
            validate(partial::keyStorePasswordFile),
            validate(partial::trustStoreFile),
            validate(partial::trustStorePasswordFile)
    )

    private fun <A> validate(property: ConfigProperty<A>) =
            Validated.fromOption(property.get(), { "- missing property: ${property.name}\n" })

    private fun <A> validationMessageFrom(invalidFields: List<Validated<String, A>>): String =
            invalidFields.map { it as Invalid }
                    .map { it.e }
                    .fold("", String::plus)
                    .let { "Some required configuration properties are missing: \n$it" }

    companion object {
        private val logger = Logger(ConfigurationValidator::class)
    }
}
