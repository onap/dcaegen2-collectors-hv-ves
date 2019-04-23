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
package org.onap.dcae.collectors.veshv.impl.adapters.kafka

import com.google.protobuf.MessageLite
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.ves.VesEventOuterClass.CommonEventHeader.*


internal class ProtobufSerializerTest : Spek({

    describe("ProtobufSerializerTest") {
        val serializer = ProtobufSerializer()

        on("serialize") {
            it("should return byte array from WTP Frame paylaod") {
                val header = getDefaultInstance()
                val payload = header.toByteArray()
                val msg: MessageLite = mock()

                serializer.serialize("", msg)

                verify(msg).toByteArray()
            }
        }

        on("configuring") {
            it("should do nothing") {
                // increase code coverage
                serializer.configure(hashMapOf<String, String>(), false)
            }
        }

        on("closing") {
            it("should do nothing") {
                // increase code coverage
                serializer.close()
            }
        }
    }


})