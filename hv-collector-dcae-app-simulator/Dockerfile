FROM openjdk:10-jre-slim

LABEL copyright="Copyright (C) 2018 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

EXPOSE 5000

WORKDIR /opt/ves-hv-dcae-app-simulator
ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.simulators.dcaeapp.MainKt"]
CMD ["--kafka-bootstrap-servers", "kafka:9092", "--kafka-topics", "ves_hvRanMeas", "--listen-port", "5000"]
COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-dcae-app-simulator-*.jar ./
