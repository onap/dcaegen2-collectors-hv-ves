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
package org.onap.dcae.collectors.veshv.analysis

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class PublicModifiersInImpl(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(javaClass.simpleName, Severity.Maintainability,
            ISSUE_DESCRIPTION, Debt(mins = 10))

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if (file.packageFqName.toString().contains("impl")) {
            checkAccessModifiers(file)
        }
    }

    private fun checkAccessModifiers(file: KtFile) {
        val implVisitor = ImplVisitor()

        file.accept(implVisitor)
        if (implVisitor.publicDeclarations.isNotEmpty()) {
            reportCodeSmells(implVisitor)
        }
    }

    private fun reportCodeSmells(it: ImplVisitor) {
        for (entity in it.publicDeclarations)
            report(CodeSmell(issue, entity, REPORT_MESSAGE))
    }

    companion object {
        private val REPORT_MESSAGE = """
                                Implementation package members cannot have public declarations.
                                Please, add `internal` modifier for this element to disallow usage outside of module
                            """.trimIndent()
        private const val ISSUE_DESCRIPTION = "Reports public modifiers inside '*.impl' package."
    }
}

private class ImplVisitor : DetektVisitor() {
    var publicDeclarations = mutableListOf<Entity>()

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        if (classOrObject.isTopLevel() && classOrObject.isPublic) {
            publicDeclarations.add(Entity.from(classOrObject))
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.isTopLevel && function.isPublic) {
            publicDeclarations.add(Entity.from(function))
        }
    }

    override fun visitProperty(property: KtProperty) {
        if (property.isTopLevel && property.isPublic) publicDeclarations.add(Entity.from(property))
    }
}