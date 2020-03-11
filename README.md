This a multimodule maven project.
```
                sparkyservice-project
                          |
        ------------------------------------
        |                                  |
 sparkyservice-api                 sparkyservice-jpa
``` 
# Projects
### sparkyservice-project
Parent pom. Defines java versions and dependencies along all child projects.

### sparkyservice-jpa
Describes the data management with "Java Persistence API". It has no dependencies to any module of this project (expect the parent-pom).

### sparkyservice-api 
Rest API written with the Spring Boot framework. Handles user management and authentication and routing as well. 

# Build and development

Run `mvn install` inside the root directory to build all sub-modules of this project. 

If you only want to use/develop with `sparkyservice-jpa` you have to install the parent-pom first. 

    mvn -N -f pom.xml install 

where the `pom.xml` is the pom of `sparkyservice-project`. Afterwards you could install `sparkyservice-jpa` normally. 

## Versioning
The sub-modules inherit the version from the parent project. Currently they are maintained together and only combined releases are scheduled.

> Currently this project is in alpha state and not ready to use. 

## IDE
We aim to provide additional configuration files for eclipse. Until that, use the eclipse import or the maven eclipse goal manually:

    mvn eclipse:eclipse
    
We recommend to do this for each project separately.
    
# License
coming soon.
