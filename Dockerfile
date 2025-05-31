FROM gradle:jdk21
WORKDIR /home/gradle/app
ADD . .
ARG module
RUN gradle clean build

FROM eclipse-temurin:21
ARG module
WORKDIR /home/java
COPY --from=0 /home/gradle/app/build/libs/*.jar app.jar
COPY --from=0 /home/gradle/app/src/main/resources ./src/main/resources

CMD java -jar ./app.jar