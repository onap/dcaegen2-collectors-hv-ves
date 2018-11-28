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
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiIdentifier
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since November 2018
 */
class SuboptimalLoggerUsage(config: Config) : Rule(config) {

    override val issue = Issue(javaClass.simpleName,
            Severity.Performance,
            "This rule reports usage of unoptimized logger calls",
            Debt(mins = 15))

    private val loggerNames = setOf("logger", "LOGGER", "log", "LOG")
    private val loggingMethods = setOf("trace", "debug", "info", "warn", "error", "severe")

    override fun visitCallExpression(expression: org.jetbrains.kotlin.psi.KtCallExpression) {
//        expression.getCalleeExpressionIfAny()?.let {
//            println(it.text)
//            if (it.textMatches("logger")) {
//                println("we are using logger")
//            }
//        }
//        expression.getCallNameExpression()?.let {
//            println(it.text)
//            if (it.textMatches("logger")) {
//                println("we are using logger")
//                val parenthesizedExpression = it.getReceiverExpression()
//            }
//        }

        val targetObject = expression.parent.firstChild
        val methodName = expression.firstChild

        println(targetObject.text)

        if (loggerNames.any(targetObject::textMatches) && loggingMethods.any(methodName::textMatches)) {
            println("we are using logger")
            val methodArgument = methodName.nextSibling
            if (methodArgument !is KtLambdaExpression) {
                report(CodeSmell(issue, Entity.from(expression), "string concatenation when using logger"))
            }
        }
//
//        if (targetObject.textMatches("logger") && expression.firstChild.textMatches("debug")) {
//            println("we are using logger")
//            val secondSubExpr = expression.children[1]
//            if (secondSubExpr.textContains('+')) {
//                report(CodeSmell(issue, Entity.from(expression), "string concatenation when using logger"))
//            }
//        }
    }

}
