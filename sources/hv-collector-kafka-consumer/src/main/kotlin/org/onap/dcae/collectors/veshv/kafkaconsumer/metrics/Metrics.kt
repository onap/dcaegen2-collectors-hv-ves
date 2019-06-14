package org.onap.dcae.collectors.veshv.kafkaconsumer.metrics

internal interface Metrics {
    fun notifyMessageOffset(size: Int)
}