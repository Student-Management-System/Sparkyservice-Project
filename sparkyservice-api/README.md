# Sparkyservice-Api

Sparkyservice-Api provides a REST API developed with Spring Boot. 

# Build & Run
Spring Boot provides an embedded Tomcat8 server by default: 

    mvn compile
    mvn spring-boot:run

It will listen on `*:8080`. 

Currently we aren't provide any other method. Planned things:

- Provide an executable jar with all dependencies in it to run it on any system with java installed
- (Maybe we stop using the embedded Tomcat and switch to war-packaging)

# Testing
Our integration tests are running with docker. Through this `docker` is a pre-request for executing them. They are 
automatically configured, started and stopped through our application. The user which executes the jar must have access
to the docker socket. 

Otherwise skip all integrations tests

	mvn install -DskipIts