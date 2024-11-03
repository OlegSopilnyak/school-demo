# -- SpringBoot Main Application --

## Assembles all layers of application's backend

* **module endpoint** - *<ins>endpoints layer</ins>* to serve REST requests to the backend.
* **module business-logic** - *<ins>business-logic layer</ins>*, facades implementation to serve requests from *<ins>endpoints layer</ins>*.
* **module persistence** - *<ins>persistence layer</ins>*, facades implementation to serve requests to the database from *<ins>business-logic layer</ins>*.

![Application Layers Package Diagram](diagrams/Application%20Layers%20Package%20Diagram.png "Application Layers Diagram")

## Main Class to start the Application
### oleg.sopilnyak.test.application.Main
## Main Configuration File of the Application
### resources/application.properties
`#Application parameters`<br>
`school.courses.maximum.rooms=49`<br>
`school.students.maximum.courses=6`<br>
`school.mail.basic.domain=school.domain`<br>
<br>
`#REST controllers root context`<br>
`server.servlet.context-path=/school`<br>
<br>
`#Actuator parameters`<br>
`management.security.enabled=false`<br>
`management.endpoints.web.exposure.include=*`<br>
<br>
`#Data source parameters`<br>
`spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect`<br>
`school.jdbc.driverClassName=com.mysql.cj.jdbc.Driver`<br>
`school.jdbc.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/school_db`<br>
`school.jdbc.username=director`<br>
`school.jdbc.password=director_password`<br>
`school.hibernate.hbm2ddl.auto=update`<br>
