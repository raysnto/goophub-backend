# GoopHub-Backend
Backend for GoopHub application using Spring Boot.

## Requirements
- Stardog Server 6.1.2 running in localhost
- $STADOG_HOME set on path

## How To Run in Eclipse / IntelliJ
- Open Eclipse > Import > Existing Maven Project
- Select the project folder > Maven > Update
- Wait until download dependecies
- Select the main class and run the application

## How To Run via .jar file
- Run `mvn package`
- Run `java -jar goophub-backend-0.0.1-SNAPSHOT.jar`

# Build or Update Docker Image
- `docker buildocker image build -t goop-app-backend:latest . `

# Run Container
- `--network=host` is used for the docker aplication connect with the Stardog Server running localy
- `docker container run --network=host -p 8080:8080 -d --name goophub-backend goop-app-backend`

## Routes
Work in progress...
