FROM docker.io/byrnedo/alpine-curl:0.1

LABEL copyright="Copyright (C) 2018 NOKIA"
LABEL license.name="The Apache Software License, Version 2.0"
LABEL license.url="http://www.apache.org/licenses/LICENSE-2.0"
LABEL maintainer="Nokia Wroclaw ONAP Team"

WORKDIR /opt/ves-hv-bootstrap

COPY bootstrap.sh ./
COPY config.json ./

RUN chmod +x ./bootstrap.sh

ENTRYPOINT ["/bin/sh", "./bootstrap.sh"]
