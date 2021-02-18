FROM nexus3.onap.org:10001/onap/integration-java11:8.0.0

LABEL copyright="Copyright (C) 2018 NOKIA" \
      license.name="The Apache Software License, Version 2.0" \
      license.url="http://www.apache.org/licenses/LICENSE-2.0" \
      maintainer="Nokia Wroclaw ONAP Team"

RUN apk install --no-cache curl

WORKDIR /opt/ves-hv-client-simulator

ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.simulators.xnf.MainKt"]

COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-xnf-simulator-*.jar ./
