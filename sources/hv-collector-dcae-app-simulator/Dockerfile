FROM eclipse-temurin:11-jre-alpine

LABEL copyright="Copyright (C) 2018 NOKIA, Copyright (C) 2026 Deutsche Telekom AG" \
      license.name="The Apache Software License, Version 2.0" \
      license.url="http://www.apache.org/licenses/LICENSE-2.0" \
      maintainer="Nokia Wroclaw ONAP Team"

ARG APP_USER=appuser
ARG APP_USER_GROUP=appuser
ARG APP_USER_ID=1000

USER root
RUN apk add --no-cache curl && \
    addgroup $APP_USER_GROUP && \
    adduser --system --disabled-password --no-create-home --uid ${APP_USER_ID} --ingroup $APP_USER_GROUP $APP_USER

WORKDIR /opt/ves-hv-dcae-app-simulator

COPY --chown=${APP_USER}:${APP_USER_GROUP} target/libs/external/* ./
COPY --chown=${APP_USER}:${APP_USER_GROUP} target/libs/internal/* ./
COPY --chown=${APP_USER}:${APP_USER_GROUP} target/hv-collector-dcae-app-simulator-*.jar ./

USER ${APP_USER}

ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.simulators.dcaeapp.MainKt"]
