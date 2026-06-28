FROM public.ecr.aws/docker/library/eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app


RUN apk add --no-cache maven


COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests


FROM public.ecr.aws/docker/library/eclipse-temurin:25-jre-alpine

WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup


COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appgroup /app/app.jar


USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]