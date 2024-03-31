#Build jar file
FROM eclipse-temurin:latest as build-jar
WORKDIR /app
COPY . /app/
RUN ./gradlew clean
RUN ./gradlew build

#Run jar file
FROM eclipse-temurin:latest
COPY --from=build-jar /app/build/libs/factoid-converters-0.4.2.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]