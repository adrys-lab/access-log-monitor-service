FROM maven:3.6.3-jdk-11-openj9 AS MAVEN3

ARG SPRING_PROFILE

COPY . /tmp/

WORKDIR /tmp

RUN mvn package -Pdev -DskipTests

FROM adoptopenjdk/openjdk11:slim

COPY --from=MAVEN3  /tmp/app/target/access-log-monitor-service.jar /usr/src/access-log-monitor-service/

WORKDIR /usr/src/access-log-monitor-service

CMD ["java","-Dspring.profiles.active=${SPRING_PROFILE}", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "access-log-monitor-service.jar"]