FROM eclipse-temurin:11-jre-alpine

LABEL copyright="Copyright (C) 2019 NOKIA" \
      license.name="The Apache Software License, Version 2.0" \
      license.url="http://www.apache.org/licenses/LICENSE-2.0" \
      maintainer="Nokia Wroclaw ONAP Team"

USER root
RUN apk add --no-cache curl

WORKDIR /opt/hv-ves-kafka-consumer

COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-kafka-consumer-*.jar ./

ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.kafkaconsumer.MainKt"]
