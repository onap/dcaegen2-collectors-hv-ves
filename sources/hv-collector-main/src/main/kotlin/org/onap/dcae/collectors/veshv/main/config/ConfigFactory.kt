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
package org.onap.dcae.collectors.veshv.main.config

import arrow.core.Option
import com.google.gson.GsonBuilder
import org.onap.dcae.collectors.veshv.main.config.adapters.AddressAdapter
import org.onap.dcae.collectors.veshv.main.config.adapters.OptionAdapter
import org.onap.dcae.collectors.veshv.main.config.adapters.RouteAdapter
import org.onap.dcae.collectors.veshv.main.config.adapters.RoutingAdapter
import org.onap.dcae.collectors.veshv.model.Route
import org.onap.dcae.collectors.veshv.model.Routing
import java.io.Reader
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
class ConfigFactory {
    fun loadConfig(input: Reader): PartialConfiguration {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(InetSocketAddress::class.java, AddressAdapter())
        gsonBuilder.registerTypeAdapter(Route::class.java, RouteAdapter())
        gsonBuilder.registerTypeAdapter(Routing::class.java, RoutingAdapter())
        gsonBuilder.registerTypeAdapter(Option::class.java, OptionAdapter())
        var config = gsonBuilder.create().fromJson(input, PartialConfiguration::class.java)
        return config
    }
}
