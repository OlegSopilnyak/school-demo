# -- School Application --

### Environment:

* Java 25
* SpringBoot 3.5
* Gradle 9.2

### Used modules.

* **common** - module with declared abstract service model and facades.
* **[endpoint](./endpoint/README.md)** - module to manage REST requests to the system.
* **[business-logic](./business-logic/README.md)** - module to process requests from REST-Controllers.
* **persistence** - the module is providing support for persistence layer of the system.
* **[application](./application/README.md)** - module to run system as a SpringBoot application, in docker container as well.


To run application in:

* **Standalone** mode use **gradlew bootRun**
* **Docker** mode use **gradlew dockerRun**
* **Docker-Compose** mode use **gradlew composeUp**

For **Standalone** and **Docker** modes I would recommend to run separate mysql image locally using command:<BR>
_$ docker run --rm -d --name school-mysql --network=school-docker-network -p 3306:3306 -e MYSQL_ROOT_PASSWORD= -e
MYSQL_ALLOW_EMPTY_PASSWORD=true -e MYSQL_USER=director -e MYSQL_PASSWORD=director_password -e MYSQL_DATABASE=school_db
mysql:8.0_

First of all you need to create **docker network** in order to make
communication between application docker-container and mysql docker-container, before you start **school-mysql**
container using command:<BR>
_$ docker network create school-docker-network_<BR>

In order to support application's health-check in **Docker-Compose** mode there is running **SpringActuator** which
provides some endpoints.
One of them (**GET** http://localhost:8080/school/actuator/health) is used for health-check support.
Other could be used for application's observation.
