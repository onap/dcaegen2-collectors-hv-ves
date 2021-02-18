FROM nexus3.onap.org:10001/onap/integration-java11:8.0.0

LABEL copyright="Copyright (C) 2018-2019 NOKIA" \
      license.name="The Apache Software License, Version 2.0" \
      license.url="http://www.apache.org/licenses/LICENSE-2.0" \
      maintainer="Nokia Wroclaw ONAP Team"

ARG HV_VES_USER=hvves
ARG HV_VES_GROUP=hvves
ARG HV_VES_USER_ID_NUMBER=1410

USER root
RUN apk add --no-cache curl netcat-openbsd
RUN addgroup $HV_VES_GROUP && adduser --system --disabled-password \
    --no-create-home --uid ${HV_VES_USER_ID_NUMBER} --ingroup $HV_VES_GROUP $HV_VES_USER && \
    mkdir -p /etc/ves-hv/ssl && \
    mkdir -p /var/log/ONAP/dcae-hv-ves-collector/ && \
    chown -R ${HV_VES_USER}:${HV_VES_GROUP} /etc/ves-hv /var/log/ONAP/ /opt && \
    chmod g+s /opt /etc/ves-hv

WORKDIR /opt/ves-hv-collector

COPY --chown=${HV_VES_USER}:${HV_VES_GROUP} target/libs/external/* ./
COPY --chown=${HV_VES_USER}:${HV_VES_GROUP} target/libs/internal/* ./
COPY --chown=${HV_VES_USER}:${HV_VES_GROUP} src/main/docker/*.sh ./
COPY --chown=${HV_VES_USER}:${HV_VES_GROUP} src/main/docker/base.json /etc/ves-hv/configuration/base.json
COPY --chown=${HV_VES_USER}:${HV_VES_GROUP} target/hv-collector-main-*.jar ./

USER ${HV_VES_USER}

ENTRYPOINT ["./entry.sh"]
CMD ["--configuration-file /etc/ves-hv/configuration/base.json"]

