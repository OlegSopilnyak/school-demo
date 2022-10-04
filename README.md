# -- School registration system --

### Used modules
* **common** - module with declared abstract service model and facades.
* **endpoint** - module to manage REST request to the system. 
* **business-logic** - module to process requests from REST-Controllers.
* **persistence** - the module is providing support for persistence layer of the system.
* **application** - module to run system as a SpringBoot application, in docker as well.

To run application in:
* **Standalone** mode use **gradlew bootRun**
* **Docker** mode use **gradlew dockerRun**
* **Docker-Compose** mode use **gradlew composeUp**

For **Standalone** and **Docker** modes I would recommend to run separate mysql image locally using command<BR>
_docker run --rm -d --name school-mysql --network=school-docker-network -p 3306:3306 -e MYSQL_ROOT_PASSWORD= -e MYSQL_ALLOW_EMPTY_PASSWORD=true -e MYSQL_USER=director -e MYSQL_PASSWORD=director_password -e MYSQL_DATABASE=school_db mysql:8.0_
For **Docker** mode you need to create network in order to communicate application docker with mysql docker before start<BR> 
_docker network create school-docker-network_<BR>_

In order to support application's health-check in **Docker-Compose** mode there is running **SpringActuator** which provides some endpoints.
One of them (**GET** http://localhost:8080/school/actuator/health) is used for health-check support.
Other could be used for application's observation.

### Endpoints
* **POST** **_/school/courses_** - to create new instance of the Course<BR>
  parameter _**CourseDto** newCourse_ (see **oleg.sopilnyak.test.endpoint.dto.CourseDto**)
* **PUT** **_/school/courses_** - to update exists instance of the Course<BR>
  parameter _**CourseDto** course_ (see **oleg.sopilnyak.test.endpoint.dto.CourseDto**)
* **GET** **_/school/courses/empty_** - to get courses without students<BR>returns **List&lt;Course&gt;**
* **GET** **_/school/courses/registered/{studentId}_** - to get courses registered in the student<BR>returns **List&lt;Course&gt;**,  parameter _studentId_ (String)
* **GET** **_/school/courses/{courseId}_** - to get the course by ID<BR>returns **oleg.sopilnyak.test.endpoint.dto.CourseDto**, parameter _courseId_ (String)
* **DELETE** **_/school/courses/{courseId}_** - to delete the course by ID<BR>returns **nothing**
  parameter _courseId_ (String)
--------------------------------------------------------------------
* **POST** **_/school/students_** - to create new instance of the Student<BR>
  parameter _**StudentDto** newStudent_ (see **oleg.sopilnyak.test.endpoint.dto.StudentDto**)
* **PUT** **_/school/students_** - to update exists instance of the Student<BR>
  parameter _**StudentDto** student_ (see **oleg.sopilnyak.test.endpoint.dto.StudentDto**)
* **GET** **_/school/students/empty_** - to get students who are not enrolled to any course<BR>
  returns **List&lt;Student&gt;**
* **GET** **_/school/students/enrolled/{courseId}_** - to get students who are enrolled to particular course<BR>
  returns **List&lt;Student&gt;**, parameter _courseId_ (String)
* **GET** **_/school/students/{studentId}_** - to get student by ID<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.StudentDto**, parameter _studentId_ (String)
* **DELETE** **_/school/students/{studentId}_** - to delete student by ID<BR>
  returns **nothing**, parameter _studentId_ (String)
--------------------------------------------------------------------
* **PUT** **_/school/register/{studentId}/to/{courseId}_** - to register the Student to the Course<BR>
  returns **nothing**, parameters _studentId_ (String), _courseId_ (String) 
* **DELETE** **_/school/register/{studentId}/to/{courseId}_** - to un-register the Student from the Course<BR>
  returns **nothing**, parameters _studentId_ (String), _courseId_ (String) 
