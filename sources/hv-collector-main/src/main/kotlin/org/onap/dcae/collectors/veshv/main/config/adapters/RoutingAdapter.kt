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
package org.onap.dcae.collectors.veshv.main.config.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.onap.dcae.collectors.veshv.model.Route
import org.onap.dcae.collectors.veshv.model.Routing
import java.lang.reflect.Type

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since March 2019
 */
class RoutingAdapter : JsonDeserializer<Routing> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Routing {
        val parametrizedType = TypeToken.getParameterized(List::class.java, Route::class.java).type
        return Routing(context?.deserialize<List<Route>>(json, parametrizedType)!!)
    }

}
