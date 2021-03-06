/*
 * ============LICENSE_START=======================================================
 * csit-dcaegen2-collectors-hv-ves
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

server = true
bootstrap = true
ui = true
client_addr = "0.0.0.0"

service {
  # name under which hv-ves collector should seek cbs
  # usually set as CONFIG_BINDING_SERVICE environment variable
  Name = "CBS"
  # address of CBS as seen by hv-ves collector
  Address = "config-binding-service"
  Port = 10000
}

