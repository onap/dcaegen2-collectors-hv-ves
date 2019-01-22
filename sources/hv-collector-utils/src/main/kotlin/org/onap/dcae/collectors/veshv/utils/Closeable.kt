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
package org.onap.dcae.collectors.veshv.utils

import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monadError.monadError
import arrow.typeclasses.binding

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since January 2019
 */
interface Closeable {
    fun close(): IO<Unit> = IO.unit

    companion object {
        fun closeAll(closeables: Iterable<Closeable>) =
                IO.monadError().binding {
                    closeables.forEach { it.close().bind() }
                }.fix()
    }
}
