FROM openjdk:10-jre-slim

LABEL copyright="Copyright (C) 2018 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

EXPOSE 5000

WORKDIR /opt/ves-hv-client-simulator
ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.simulators.xnf.MainKt"]
CMD ["--ves-host", "ves-hv-collector", "--ves-port", "6061"]
COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-xnf-simulator-*.jar ./
