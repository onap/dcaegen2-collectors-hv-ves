FROM openjdk:10-jre-slim
LABEL maintainer="Nokia Wroclaw ONAP Team"

EXPOSE 8081

WORKDIR /opt/ves-hv-collector
ENTRYPOINT ["java", "-cp", "*:", "org.onap.dcae.collectors.veshv.main.MainKt"]
CMD ["--listen-port", "8081", "--config-url", ""]
COPY target/libs/external/* ./
COPY target/libs/internal/* ./
COPY target/hv-collector-main-*.jar ./