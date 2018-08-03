FROM docker.io/openjdk:8-jre-slim

LABEL copyright="Copyright (C) 2018 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

RUN apt-get update \
        && apt-get install -y --no-install-recommends curl  \
        && apt-get clean

WORKDIR /opt/ves-hv-dcae-app-simulator
ENTRYPOINT ["./run-java.sh", "run"]
COPY target/docker-extra/run-java/run-java.sh ./

ENV JAVA_MAIN_CLASS=org.onap.dcae.collectors.veshv.simulators.dcaeapp.MainKt

COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-dcae-app-simulator-*.jar ./
