# 运行镜像。jar 由 dev.sh 在本机 mvn package 生成。
# 不在镜像里编译：podman machine 只有 2GiB，容器内 Maven 会把几秒的重编变成几分钟。
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY lake-console/target/app.jar /app/console.jar
COPY s3-gateway/target/app.jar /app/gateway.jar
COPY docker/lake-start.sh /app/start.sh
RUN useradd --system --uid 10001 --home /app lake \
    && chmod 755 /app/start.sh \
    && chown -R lake:lake /app
USER lake
EXPOSE 8080 9109
ENTRYPOINT ["/app/start.sh"]
