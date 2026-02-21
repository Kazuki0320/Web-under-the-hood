FROM tomcat:10.1-jdk17-temurin

WORKDIR /work

COPY src ./src
COPY webapp ./webapp

RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
    && javac -cp /usr/local/tomcat/lib/servlet-api.jar \
        -d /usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
        $(find ./src/main/java -name "*.java")

RUN cp -r ./webapp/* /usr/local/tomcat/webapps/ROOT/ 2>/dev/null || true