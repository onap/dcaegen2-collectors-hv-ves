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
import arrow.core.Option
import arrow.core.Some
import com.google.protobuf.Descriptors
import org.onap.dcae.collectors.veshv.domain.vesEventListenerVersionRegex
import org.onap.ves.VesEventOuterClass

abstract class HeaderReader<A> {
    abstract fun read(header: VesEventOuterClass.CommonEventHeader, s: A): Option<A>

    companion object {

        val REQUIRED_FIELD_READ: HeaderReader<Descriptors.FieldDescriptor> =
            object : HeaderReader<Descriptors.FieldDescriptor>() {
                override fun read(
                    header: VesEventOuterClass.CommonEventHeader,
                    s: Descriptors.FieldDescriptor
                ): Option<Descriptors.FieldDescriptor> =
                    if (header.hasField(s)) Some(s) else None
            }

        val EVENT_LISTENER_VERSION_READ: HeaderReader<String> =
            object : HeaderReader<String>() {
                override fun read(header: VesEventOuterClass.CommonEventHeader, s: String): Option<String> {
                    return if (vesEventListenerVersionRegex.matches(header.vesEventListenerVersion)) Some(header.vesEventListenerVersion) else None
                }
            }

    }
}
