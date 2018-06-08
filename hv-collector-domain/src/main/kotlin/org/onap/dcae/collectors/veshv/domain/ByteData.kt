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
package org.onap.dcae.collectors.veshv.domain

import com.google.protobuf.MessageLite
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class ByteData(private val data: ByteArray) {

    fun size() = data.size

    /**
     * This will expose mutable state of the data.
     *
     * @return wrapped data buffer (NOT a copy)
     */
    fun unsafeAsArray() = data

    fun writeTo(byteBuf: ByteBuf) {
        byteBuf.writeBytes(data)
    }

    fun asString(charset: Charset = Charset.defaultCharset()) = String(data, charset)

    companion object {
        val EMPTY = ByteData(byteArrayOf())

        fun readFrom(byteBuf: ByteBuf, length: Int): ByteData {
            val dataArray = ByteArray(length)
            byteBuf.readBytes(dataArray)
            return ByteData(dataArray)
        }
    }
}

fun MessageLite.toByteData(): ByteData = ByteData(toByteArray())
