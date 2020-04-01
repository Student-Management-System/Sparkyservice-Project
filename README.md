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
	
# Developing

### Test instance:
For developers which uses this project, we provide a test live instance reachable under `147.172.178.30:8080`. 

### Swagger
Swagger is reachable under `/swagger-ui.html`. On the live instance: `147.172.178.30:8080/swagger-ui.html`.

### Versioning
The sub-modules inherit the version from the parent project. Currently they are maintained together and only combined releases are scheduled.

For version editing make use of mavens version plugin:


	mvn versions:set -DnewVersion=2.50.1-SNAPSHOT
	mvn versions:revert
	mvn versions:commit


> Currently this project is in alpha state and not ready to use. 

### IDE
We aim to provide additional configuration files for eclipse. Until that, use the eclipse import or the maven eclipse goal manually:

    mvn eclipse:eclipse
    
We recommend to do this for each project separately.

# Configuration
If you run the application for the first time, you have to create all tables. Set the following property to `update` or `create`:

	spring.jpa.hibernate.ddl-auto = update
	
With this setting a strict dependency to the configured database is set. 

# License
coming soon.
