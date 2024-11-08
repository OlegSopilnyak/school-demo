# --  Business-Logic for education and staff of the school

## Process calls to Business-Logic Facades 

Implementation of facades use particular command-strategy for any facade's method<br>
for further processing and calling **<ins>persistence layer</ins>** through persistence facades.

![Business Logic Layers General Diagram](diagrams/Business%20Logic%20Layers%20General%20Diagram.png "Business Logic Layer General")

### Module is built using patterns [Strategy](https://en.wikipedia.org/wiki/Strategy_pattern) and [Factory](https://en.wikipedia.org/wiki/Abstract_factory_pattern)
![Business Logic Layers Sequence Diagram](diagrams/Business%20Logic%20Layers%20Sequence%20Diagram.png "Business Logic Layer Sequence")

### Facades Implementation and Persistence Facade Usage

|Facade Implementation| Commands                                  | Persistence Facade#Method                                       |
|---------|-------------------------------------------|-----------------------------------------------------------------|
|CoursesFacadeImpl| **FindCourseCommand**                     | `CoursesPersistenceFacade#findCourseById(id)`                   |
|| **FindRegisteredCoursesCommand**          | `RegisterPersistenceFacade#findCoursesRegisteredForStudent(id)` |
|| **FindCoursesWithoutStudentsCommand**     | `RegisterPersistenceFacade#findCoursesWithoutStudents(id)`      |
|| **CreateOrUpdateCourseCommand**           | `CoursesPersistenceFacade#save(Course course)`                  |
|| **RegisterStudentToCourseCommand**        | `EducationPersistenceFacade#link(student, course)`              |
|| **UnRegisterStudentFromCourseCommand**    | `EducationPersistenceFacade#unLink(student, course)`            |
|StudentsFacadeImpl| **FindStudentCommand**                    | `StudentsPersistenceFacade#findStudentById(id)`                 |
|| **FindEnrolledStudentsCommand**           | `RegisterPersistenceFacade#findEnrolledStudentsByCourseId(id)`  |
|| **FindNotEnrolledStudentsCommand**        | `RegisterPersistenceFacade#findNotEnrolledStudents()`           |
|| **CreateOrUpdateStudentCommand**          | `StudentsPersistenceFacade#save(Student student)`               |
|| **CreateStudentMacroCommand**             | **macro-command**                                               |
||| see **CreateOrUpdateStudentProfileCommand**                     |
||| see **CreateOrUpdateStudentCommand**                            |
|| **DeleteStudentCommand**          | `StudentsPersistenceFacade#deleteStudent(id)`|
|| **DeleteStudentMacroCommand**             | **macro-command**|
||| see **DeleteStudentProfileCommand**|
||| see **DeleteStudentCommand**|
|AuthorityPersonFacadeImpl| **LoginAuthorityPersonCommand**           | `PersistenceFacade#findPrincipalProfileByLogin(userName)`       |
||                                           | `PersistenceFacade#findAuthorityPersonByProfileId(id)`          |
|| **LogoutAuthorityPersonCommand**          | not use yet                                                     |
|| **FindAllAuthorityPersonsCommand**        | `AuthorityPersonPersistenceFacade#findAllAuthorityPersons()`    |
|| **FindAuthorityPersonCommand**            | `AuthorityPersonPersistenceFacade#findAuthorityPersonById(id)`  |
|| **CreateOrUpdateAuthorityPersonCommand**  | `AuthorityPersonPersistenceFacade#save(AuthorityPerson person)` |
|| **CreateAuthorityPersonMacroCommand**     | macro-command (**create-person** + **create-person-profile**)   |
|| **DeleteAuthorityPersonMacroCommand**     | macro-command (**delete-person** + **delete-person-profile**)   |
|FacultyFacadeImpl| **FindAllFacultiesCommand**               | `FacultyPersistenceFacade#findAllFaculties()`                   |
|| **FindFacultyCommand**                    | `FacultyPersistenceFacade#findFacultyById(id)`                  |
|| **CreateOrUpdateFacultyCommand**          | `FacultyPersistenceFacade#save(Faculty instance)`               |
|| **DeleteFacultyCommand**                  | `FacultyPersistenceFacade#deleteFaculty(id)`                    |
|StudentsGroupFacadeImpl| **FindAllStudentsGroupsCommand**          | `StudentsGroupPersistenceFacade#findAllStudentsGroups()`        |
|| **FindStudentsGroupCommand**              | `StudentsGroupPersistenceFacade#findStudentsGroupById(id)`      |
|| **CreateOrUpdateStudentsGroupCommand**    | `StudentsGroupPersistenceFacade#save(StudentsGroup instance)`   |
|| **DeleteStudentsGroupCommand**            | `StudentsGroupPersistenceFacade#deleteStudentsGroup(id)`        |
|PrincipalProfileFacadeImpl| **FindPrincipalProfileCommand**| `ProfilePersistenceFacade#findPrincipalProfileById(id)`         |
|| **CreateOrUpdatePrincipalProfileCommand** | `ProfilePersistenceFacade#save(PrincipalProfile input)`         |
|| **DeletePrincipalProfileCommand**         | `ProfilePersistenceFacade#deleteProfileById(id)`                |
|StudentProfileFacadeImpl| **FindStudentProfileCommand**             | `ProfilePersistenceFacade#findStudentProfileById(id)`           |
|| **CreateOrUpdateStudentProfileCommand**   | `ProfilePersistenceFacade#save(StudentProfile input)`           |
|| **DeleteStudentProfileCommand**           | `ProfilePersistenceFacade#deleteProfileById(id)`                |
### Module Data Model
![Business Logic Model Classes Diagram](diagrams/Model%20Classes%20Diagram.png "Business Logic Layer Sequence")
