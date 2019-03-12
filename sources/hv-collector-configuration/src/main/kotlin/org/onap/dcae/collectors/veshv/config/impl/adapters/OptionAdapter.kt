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
package org.onap.dcae.collectors.veshv.config.impl.adapters

import arrow.core.Option
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since March 2019
 */
class OptionAdapter : JsonDeserializer<Option<Any>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Option<Any> {
        val parametrizedType = typeOfT as ParameterizedType
        val typeParameter = parametrizedType.actualTypeArguments.first()
        return Option.fromNullable(context.deserialize<Any>(json, typeParameter))
    }

}
