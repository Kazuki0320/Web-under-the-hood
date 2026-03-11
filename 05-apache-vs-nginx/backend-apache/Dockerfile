FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY src ./src
RUN javac src/Main.java

CMD ["javac", "-cp", "src", "Main"]