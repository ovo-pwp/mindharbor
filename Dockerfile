# ============================================================
# 多阶段构建：Maven 编译 → JRE 运行
# 前端已构建进 src/main/resources/static，jar 内自带页面
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# 国内加速：换阿里云 Maven 镜像
COPY docker/maven-settings.xml /root/.m2/settings.xml

# 先只拷 pom，利用层缓存把依赖下好
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre-jammy

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && groupadd -r app && useradd -r -g app app

WORKDIR /app

COPY --from=build /build/target/psych-counseling-1.0.0.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

# 用 bash 的 /dev/tcp 探活，避免额外装 curl
HEALTHCHECK --interval=30s --timeout=5s --start-period=70s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
