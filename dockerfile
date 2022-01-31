# latest oracle openjdk is the basis
FROM openjdk:oracle
# copy jar file into container image under app directory
COPY target/goophub-backend-0.0.1-SNAPSHOT.jar app/goophub-backend.jar
# expose server port accept connections
EXPOSE 8080
# start application
CMD ["java", "-jar", "app/goophub-backend.jar"]
