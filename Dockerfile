# ---------- Stage 1: compile the Java code and build the WAR ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY lib lib
COPY src src
RUN mkdir -p out/WEB-INF/classes out/WEB-INF/lib \
 && cp -r src/main/webapp/. out/ \
 && javac --release 17 -encoding UTF-8 -cp "lib/*" -d out/WEB-INF/classes $(find src/main/java -name "*.java") \
 && cp lib/postgresql-*.jar out/WEB-INF/lib/ \
 && cd out && jar --create --file /app/ROOT.war .

# ---------- Stage 2: run it on Tomcat 10.1 ----------
# Settings (DB_HOST, DB_USER, DB_PASSWORD, ADMIN_PASSWORD, ...) come from environment variables on the host.
FROM tomcat:10.1-jdk21
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/ROOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
