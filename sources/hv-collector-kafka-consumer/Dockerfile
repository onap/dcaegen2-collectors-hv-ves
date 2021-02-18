FROM nexus3.onap.org:10001/onap/integration-java11

LABEL copyright="Copyright (C) 2019 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

RUN apt-get update \
        && apt-get install -y --no-install-recommends curl  \
        && apt-get clean

WORKDIR /opt/hv-ves-kafka-consumer

ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.kafkaconsumer.MainKt"]

COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-kafka-consumer-*.jar ./
