FROM openjdk:8-jre-slim

LABEL copyright="Copyright (C) 2018 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

WORKDIR /opt/ves-hv-collector
ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.main.MainKt"]
COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-main-*.jar ./
