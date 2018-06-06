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

import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader

internal class MessageValidator {

    val requiredFieldDescriptors = listOf(
            "version",
            "eventName",
            "domain",
            "eventId",
            "sourceName",
            "reportingEntityName",
            "priority",
            "startEpochMicrosec",
            "lastEpochMicrosec",
            "sequence")
    .map { fieldName -> CommonEventHeader.getDescriptor().findFieldByName(fieldName)}

    fun isValid(message: VesMessage): Boolean {
        val header = message.header
        return allMandatoryFieldsArePresent(header) && header.domain == CommonEventHeader.Domain.HVRANMEAS
    }

    private fun allMandatoryFieldsArePresent(header: CommonEventHeader) =
            requiredFieldDescriptors
                    .all { fieldDescriptor -> header.hasField(fieldDescriptor) }
}
