# Sparkyservice-Api [![Build Status](https://jenkins-2.sse.uni-hildesheim.de/buildStatus/icon?job=Teaching_Sparkyservice-Project&style=flat-square)](https://jenkins-2.sse.uni-hildesheim.de/view/Teaching/job/Teaching_Sparkyservice-Project/)

Sparkyservice-Api provides a REST API developed with Spring Boot. 


# Build & Run
Spring Boot provides an embedded Tomcat8 server by default: 

    mvn compile
    mvn spring-boot:run

The service will listen on `*:8080`. 
For a full build cycle

	mvn install

### Run without maven
We provide a jar with [(here)](https://jenkins-2.sse.uni-hildesheim.de/view/Teaching/job/Teaching_Sparkyservice-Project) which includes an embedded Tomcat8 Server and all other dependencies. 

1. Create custom `application-release.properties` (you can find an example in our wiki)
2. Run the application: 

	java -cp sparkyservice.jar:application-release.properties org.springframework.boot.loader.JarLauncher

Planned deployments:

- Provide an additional war in order to deploy the project to an existing Tomcat server

# Testing
Our integration tests are running with docker. Through this `docker` is a pre-request for executing them. They are 
automatically configured, started and stopped through our application. The user which executes the jar must have access
to the docker socket. 

Otherwise skip all integrations tests

	mvn install -DskipIts

# Configuration

See https://github.com/Student-Management-System/Sparkyservice-Project/wiki/Properties

Short version: Take the example porperty file and make neccessary changes. When running the application for the first time, the following property must be set to `update` or `create` for creating database tables:

	spring.jpa.hibernate.ddl-auto = update

Unless this setting is removed, the application won't start when the database is offline.

# License
coming soon.
