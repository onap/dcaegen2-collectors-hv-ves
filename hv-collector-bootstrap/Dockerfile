FROM docker.io/openjdk:8-jre-slim

LABEL copyright="Copyright (C) 2018 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

WORKDIR /opt/ves-hv-bootstrap
COPY bootstrap.sh ./

RUN apt-get update \
        && apt-get install -y --no-install-recommends curl  \
        && apt-get clean \
        && chmod +x ./bootstrap.sh

ENTRYPOINT ["./bootstrap.sh"]