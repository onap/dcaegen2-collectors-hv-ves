FROM docker.io/openjdk:11-jre-slim

LABEL copyright="Copyright (C) 2018 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

RUN apt-get update \
        && apt-get install -y --no-install-recommends curl netcat \
        && apt-get clean

WORKDIR /opt/ves-hv-collector

ENTRYPOINT ["./entry.sh"]
CMD ["--configuration-file /etc/ves-hv/configuration/base.json"]

COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY src/main/docker/*.sh ./
COPY src/main/docker/base.json /etc/ves-hv/configuration/base.json

COPY target/hv-collector-main-*.jar ./
