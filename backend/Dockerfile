# 后端 Dockerfile
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

# 使用Maven wrapper或直接下载maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre

WORKDIR /app

# 创建上传目录
RUN mkdir -p /app/uploads

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
