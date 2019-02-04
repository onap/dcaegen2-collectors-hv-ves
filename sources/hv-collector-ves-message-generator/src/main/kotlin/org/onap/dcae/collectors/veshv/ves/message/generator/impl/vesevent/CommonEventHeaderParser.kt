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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent

import arrow.core.Option
import com.google.protobuf.util.JsonFormat
import org.onap.dcae.collectors.veshv.domain.headerRequiredFieldDescriptors
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import javax.json.JsonObject

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since July 2018
 */
class CommonEventHeaderParser {
    fun parse(json: JsonObject): Option<CommonEventHeader> = Option.fromNullable(
            CommonEventHeader.newBuilder()
                    .apply { JsonFormat.parser().merge(json.toString(), this) }
                    .build()
                    .takeUnless { !isValid(it) }
    )


    private fun isValid(header: CommonEventHeader): Boolean =
            allMandatoryFieldsArePresent(header)


    private fun allMandatoryFieldsArePresent(header: CommonEventHeader) =
            headerRequiredFieldDescriptors
                    .all { fieldDescriptor -> header.hasField(fieldDescriptor) }

}
