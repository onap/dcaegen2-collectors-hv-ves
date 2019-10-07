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
package org.onap.dcae.collectors.veshv.tests.utils

import arrow.core.Try
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.search.RequiredSearch
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.assertj.core.api.Assertions


fun <T> PrometheusMeterRegistry.verifyGauge(name: String, verifier: (Gauge) -> T) =
        verifyMeter(findMeter(name), RequiredSearch::gauge, verifier)

fun <T> PrometheusMeterRegistry.verifyGauge(name: String, tagKey: String = "partition", tagValue: String, verifier: (Gauge) -> T) =
        verifyMeter(findMeter(name, tagKey, tagValue), RequiredSearch::gauge, verifier)

fun <T> PrometheusMeterRegistry.verifyTimer(name: String, verifier: (Timer) -> T) =
        verifyMeter(findMeter(name), RequiredSearch::timer, verifier)

fun <T> PrometheusMeterRegistry.verifyCounter(name: String, verifier: (Counter) -> T) =
        verifyCounter(findMeter(name), verifier)

fun <T> PrometheusMeterRegistry.verifyCounter(name: String, tags: Tags, verifier: (Counter) -> T) =
        verifyCounter(findMeter(name).tags(tags), verifier)

private fun PrometheusMeterRegistry.findMeter(meterName: String) = RequiredSearch.`in`(this).name(meterName)

private fun PrometheusMeterRegistry.findMeter(meterName: String, tagKey: String, tagValue: String) =
        RequiredSearch.`in`(this).tag(tagKey, tagValue).name(meterName)

private fun <T> verifyCounter(search: RequiredSearch, verifier: (Counter) -> T) =
        verifyMeter(search, RequiredSearch::counter, verifier)

private inline fun <M, T> verifyMeter(search: RequiredSearch,
                                      map: (RequiredSearch) -> M,
                                      verifier: (M) -> T) =
        Try { map(search) }.fold(
                { ex -> Assertions.assertThat(ex).doesNotThrowAnyException() },
                verifier
        )