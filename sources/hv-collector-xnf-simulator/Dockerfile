FROM eclipse-temurin:11-jre-alpine

LABEL copyright="Copyright (C) 2018 NOKIA" \
      license.name="The Apache Software License, Version 2.0" \
      license.url="http://www.apache.org/licenses/LICENSE-2.0" \
      maintainer="Nokia Wroclaw ONAP Team"

USER root
RUN apk add --no-cache curl

WORKDIR /opt/ves-hv-client-simulator

COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-xnf-simulator-*.jar ./

ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.simulators.xnf.MainKt"]
