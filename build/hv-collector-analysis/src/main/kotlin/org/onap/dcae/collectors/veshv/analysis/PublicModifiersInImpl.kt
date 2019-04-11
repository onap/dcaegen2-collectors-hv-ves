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
            ISSUE_DESCRIPTION, Debt(mins=10))


    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if(file.packageFqName.toString().contains("impl")) {
            ImplVisitor().also {
                file.accept(it)
                if(it.containsPublicDeclarations())
                    report(CodeSmell(issue, Entity.from(file), REPORT_MESSAGE))
            }
        }
    }

    companion object {
        const val REPORT_MESSAGE = "Implementation files cannot have public declarations"
        const val ISSUE_DESCRIPTION = "Reports public modifiers inside '*.impl' package."
    }
}

private class ImplVisitor: DetektVisitor(){
    private var publicDeclarations = 0

    fun containsPublicDeclarations() = publicDeclarations > 0

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        if(classOrObject.isTopLevel() && classOrObject.isPublic){
            publicDeclarations++
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if(function.isTopLevel && function.isPublic){
            publicDeclarations++
        }
    }

    override fun visitProperty(property: KtProperty) {
        if(property.isTopLevel && property.isPublic) publicDeclarations++
    }
}