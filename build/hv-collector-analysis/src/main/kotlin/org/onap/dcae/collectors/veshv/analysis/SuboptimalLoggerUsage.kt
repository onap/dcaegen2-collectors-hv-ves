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
package org.onap.dcae.collectors.veshv.analysis

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since November 2018
 */
class SuboptimalLoggerUsage(config: Config) : Rule(config) {

    override val issue = Issue(javaClass.simpleName,
            Severity.Performance,
            """
                Reports usage of unoptimized logger calls.
                In Kotlin every method call (including logger calls) is eagerly evaluated by default. That means that
                each argument will be evaluated even if loglevel is higher than in current call. The most common way of
                mitigating this issue is to use lazy loading by means of lambda expressions, so instead of
                log.debug("a=${'$'}a") we can write log.debug{ "a=${'$'}a" }. Logging string literals is fine - no
                additional computation will be performed.""".trimIndent(),
            Debt(mins = 10))

    private val loggerNames = config.valueOrDefault("loggerNames", DEFAULT_LOGGER_NAMES).split(",")

    private val loggingMethods = config.valueOrDefault("loggingMethodNames", DEFAULT_LOGGING_METHOD_NAMES).split(",")

    override fun visitCallExpression(expression: KtCallExpression) {
        val targetObject = expression.parent.firstChild
        val methodName = expression.firstChild

        logExpressionArguments(targetObject, methodName)
                ?.let(this::checkGettingWarningMessage)
                ?.let { reportCodeSmell(expression, it) }
    }

    private fun logExpressionArguments(targetObject: PsiElement, methodName: PsiElement) =
            if (isLogExpression(targetObject, methodName))
                methodName.nextSibling as? KtValueArgumentList
            else null

    private fun isLogExpression(targetObject: PsiElement, methodName: PsiElement) =
            loggerNames.any(targetObject::textMatches) && loggingMethods.any(methodName::textMatches)

    private fun checkGettingWarningMessage(args: KtValueArgumentList) = when {
        args.anyDescendantOfType<KtOperationExpression> { true } ->
            "should not use any operators in logging expression"
        args.anyDescendantOfType<KtCallExpression> { true } ->
            "should not call anything in logging expression"
        args.anyDescendantOfType<KtStringTemplateExpression> { it.hasInterpolation() } ->
            "should not use string interpolation in logging expression"
        else -> null
    }

    private fun reportCodeSmell(expression: KtCallExpression, message: String) {
        report(CodeSmell(issue, Entity.from(expression), message))
    }

    companion object {
        const val DEFAULT_LOGGER_NAMES = "logger,LOGGER,log,LOG"
        const val DEFAULT_LOGGING_METHOD_NAMES = "trace,debug,info,warn,error,severe"
    }
}
