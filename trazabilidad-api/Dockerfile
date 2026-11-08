# API para produccion: se compila el .jar y se ejecuta con Java sin el JDK ni el codigo fuente.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
# Los tests necesitan Docker y ya se pasan antes de desplegar; aqui solo se compila
RUN sh ./gradlew --no-daemon bootJar -x test && cp "$(ls build/libs/*.jar | grep -v plain)" app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home api
COPY --from=build /app/app.jar app.jar
USER api
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
