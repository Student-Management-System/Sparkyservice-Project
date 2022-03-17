# Sparkyservice-Api [![Build Status](https://jenkins-2.sse.uni-hildesheim.de/buildStatus/icon?job=Teaching_Sparkyservice-Project&style=flat-square)](https://jenkins-2.sse.uni-hildesheim.de/view/Teaching/job/Teaching_Sparkyservice-Project/)

Sparkyservice-Api provides a REST API developed with Spring Boot. 


# Build

	git clone https://github.com/Student-Management-System/Sparkyservice-Project/
	cd Sparkyservice-Project
	mvn package -DskipIts

A jar (name ends with `spring-boot.jar`) appears in the `target` directory.

If you don't want to build the software by your own, you can download jar from our [Jenkins](https://jenkins-2.sse.uni-hildesheim.de/view/Teaching/job/Teaching_Sparkyservice-Project) which includes an embedded Tomcat8 Server and all other dependencies.

## Configuration
For a full configuration explanation this wiki article: https://github.com/Student-Management-System/Sparkyservice-Project/wiki/Properties

Short summary:
1. Create a file called `application-prod.yml` alongside the JAR file (or into `src/main/resources` when using maven).
2. Copy the example content from the wiki into it
3. Adapt the values to your needs
  
To create the database tables on the very first start, set the following configuration inside your spring profile (and remove it aftewards):

```
spring:
	jpa:
		hibernate:
			ddl-auto: create
```

## Starting

To run the jar run:

	java -jar -Dspring.profiles.active=prod,postgres sparkyservice-spring-boot.jar

Change the profiles to your need. See: [Configuration](https://github.com/Student-Management-System/Sparkyservice-Project/wiki/Properties)

# Development
coming soon

    mvn compile
    mvn spring-boot:run

## Testing
Our integration tests are running with docker. Through this `docker` is a pre-request for executing them. They are 
automatically configured, started and stopped through our application. The user which executes the jar must have access
to the docker socket. 

Otherwise skip all integrations tests

	mvn install -DskipIts


# License
coming soon.
