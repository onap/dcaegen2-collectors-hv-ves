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

import com.google.gson.*
import java.lang.reflect.Type
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
class AddressAdapter : JsonDeserializer<InetSocketAddress> {
    val DEFAULT_PORT = 12000
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): InetSocketAddress {
        if (json != null) {
            val address = json.asString
            val parts = address.split(":")
            if (parts.size == 2) {
                return InetSocketAddress(parts[0], parts[1].toInt())
            } else if (parts.size == 1) {
                // TODO: may not be needed
                return InetSocketAddress(parts[0], DEFAULT_PORT)
            } else {
                throw InvalidAddressException("Cannot parse '"+address+"' to address")
            }
        } else {
            throw InvalidAddressException("Missing address")
        }
    }
}

class InvalidAddressException(reason:String) : RuntimeException(reason) {
}
