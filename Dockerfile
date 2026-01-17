FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache libstdc++ gcompat
WORKDIR /app
RUN mkdir -p /data/rocksdb && chmod 777 /data/rocksdb
COPY build/libs/*.jar app.jar

ENV SERVER_PORT=9090
EXPOSE 9090

ENTRYPOINT ["java", \
"-Dstate.dir=/data/rocksdb", \
"-Dserver.port=9090", \
"--add-opens", "java.base/java.lang=ALL-UNNAMED", \
"--add-opens", "java.base/java.util=ALL-UNNAMED", \
"-jar", "app.jar"]