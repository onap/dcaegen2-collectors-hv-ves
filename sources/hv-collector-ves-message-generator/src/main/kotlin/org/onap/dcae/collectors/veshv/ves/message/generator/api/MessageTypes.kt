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
package org.onap.dcae.collectors.veshv.ves.message.generator.api

import arrow.core.Try

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
enum class VesEventType {
    VALID,
    TOO_BIG_PAYLOAD,
    FIXED_PAYLOAD;

    companion object {
        fun isVesEventType(str: String): Boolean = Try { valueOf(str) }.isSuccess()
    }
}

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
enum class WireFrameType {
    INVALID_WIRE_FRAME,
    INVALID_GPB_DATA;

    companion object {
        fun isWireFrameType(str: String): Boolean = Try { WireFrameType.valueOf(str) }.isSuccess()
    }
}
