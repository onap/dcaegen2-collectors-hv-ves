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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters

import org.onap.dcae.collectors.veshv.simulators.xnf.impl.Status
import org.onap.dcae.collectors.veshv.utils.http.Content
import org.onap.dcae.collectors.veshv.utils.http.ContentType
import org.onap.dcae.collectors.veshv.utils.http.HttpStatus
import org.onap.dcae.collectors.veshv.utils.http.Response
import java.util.*
import javax.json.Json

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
object Responses {

    fun acceptedResponse(id: UUID): Response {
        return Response(
                HttpStatus.ACCEPTED,
                Content(ContentType.TEXT, id)
        )
    }

    fun statusResponse(status: Status): Response {
        return Response(HttpStatus.OK,
                Content(ContentType.JSON,
                        Json.createObjectBuilder()
                                .add("status", status.toString())
                                .add("message", status.message)
                                .build()))
    }
}