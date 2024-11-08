# --  REST endpoints for the School Application

## Receives HTTP request and servers them 

Received by REST controllers http requests pre-processing
for further processing<br> and sending to **<ins>business-logic layer</ins>** through service facades.  

![Endpoint Layer General Diagram](diagrams/Endpoints%20Layer%20General%20Diagram.png "Endpoint Layer Diagram")
### General Request Processing
![Endpoint Layer General Diagram](diagrams/Endpoints%20Layer%20Sequence%20Diagram.png "Endpoint Layer Diagram")
* ### education
#### StudentsRestController
* **GET** **_/school/students/{studentId}_** - to get student by ID<BR>
  calls the`StudentsFacade#findById(id)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.education.StudentDto**, input parameter `String studentId`
* **GET** **_/school/students/enrolled/{courseId}_** - to get students who are enrolled to particular course<BR>
  calls the`StudentsFacade#findEnrolledTo(id)`<BR>
  returns **List&lt;oleg.sopilnyak.test.endpoint.dto.education.StudentDto&gt;**, input parameter `String courseId`
* **GET** **_/school/students/empty_** - to get students who are not enrolled to any course<BR>
  calls the`StudentsFacade#findNotEnrolled()`<BR>
  returns **List&lt;oleg.sopilnyak.test.endpoint.dto.education.StudentDto&gt;**, no parameters
* **POST** **_/school/students_** - to create new instance of the Student<BR>
  calls the`StudentsFacade#create(studentDto)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.education.StudentDto**, input parameter `StudentDto studentDto`
* **PUT** **_/school/students_** - to update exists instance of the Student<BR>
  calls the`StudentsFacade#createOrUpdate(studentDto)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.education.StudentDto**, input parameter `StudentDto studentDto`
* **DELETE** **_/school/students/{studentId}_** - to delete student by ID<BR>
  calls the`StudentsFacade#delete(id)`<BR>
  no return, input parameter `String studentId`
#### CoursesRestController
* **GET** **_/school/courses/{courseId}_** - to get the course by ID<BR>
  calls the`CoursesFacade#findById(id)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.education.CourseDto**, input parameter `String courseId`
* **GET** **_/school/courses/registered/{studentId}_** - to get courses registered in the student<BR>
  calls the`CoursesFacade#findRegisteredFor(id)`<BR>
  returns **List&lt;oleg.sopilnyak.test.endpoint.dto.education.CourseDto&gt;**, input parameter `String studentId`
* **GET** **_/school/courses/empty_** - to get courses without students<BR>
  calls the`CoursesFacade#findWithoutStudents()`<BR>
  returns **List&lt;oleg.sopilnyak.test.endpoint.dto.education.CourseDto&gt;**, no parameters
* **POST** **_/school/courses_** - to create new instance of the Course<BR>
  calls the`CoursesFacade#createOrUpdate(courseDto)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.education.CourseDto**, input parameter `CourseDto courseDto`
* **PUT** **_/school/courses_** - to update exists instance of the Course<BR>
  calls the`CoursesFacade#createOrUpdate(courseDto)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.education.CourseDto**, input parameter `CourseDto courseDto`
* **DELETE** **_/school/courses/{courseId}_** - to delete the course by ID<BR>
  calls the`CoursesFacade#delete(id)`<BR>
  no return, input parameter `String courseId`
#### RegisterCourseController
* **PUT** **_/school/register/{studentId}/to/{courseId}_** - to register the Student to the Course<BR>
  - see `oleg.sopilnyak.test.school.common.model.Student student`<BR>
  - see `oleg.sopilnyak.test.school.common.model.Course course`<BR>
    calls the`CoursesFacade#register(student, course)`<BR>
  no return, parameters `String studentId`, `String courseId`
* **DELETE** **_/school/register/{studentId}/to/{courseId}_** - to un-register the Student from the Course<BR>
    - see `oleg.sopilnyak.test.school.common.model.Student student`<BR>
    - see `oleg.sopilnyak.test.school.common.model.Course course`<BR>
      calls the`CoursesFacade#unRegister(student, course)`<BR>
  no return, parameters `String studentId`, `String courseId`
* ### organization
#### AuthorityPersonsRestController
* **POST** **_/school/login_** - to log in the authority person<BR>
  calls the`AuthorityPersonFacade#login(username, password)`<BR>
  returns logged in **oleg.sopilnyak.test.endpoint.dto.organization.AuthorityPersonDto**, input parameters `String username`, `String password` 
* **DELETE** **_/school/logout_** - to log out the authority person, uses valid **token** of logged in authority person<BR>
  calls the`AuthorityPersonFacade#logout(token)`<BR>
  no return, input parameters `String token`
* **GET** **_/school/authorities_** - to get all school's authorities<BR>
  calls the`AuthorityPersonFacade#findAllAuthorityPersons()`<BR>
  returns **List&lt;oleg.sopilnyak.test.endpoint.dto.organization.AuthorityPersonDto&gt;**, no input parameters
* **GET** **_/school/authorities/{personId}_** - to get school's authority person by ID<BR>
  calls the`AuthorityPersonFacade#findAuthorityPersonById(id)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.organization.AuthorityPersonDto**, input parameter `String personId`
* **POST** **_/school/authorities_** - to create the authority person instance<BR>
  calls the`AuthorityPersonFacade#create(person)`<BR>
  returns created **oleg.sopilnyak.test.endpoint.dto.organization.AuthorityPersonDto**, input parameter `AuthorityPersonDto person`
* **PUT** **_/school/authorities_** - to update exists authority person
  calls the`AuthorityPersonFacade#createOrUpdateAuthorityPerson(person)`<BR>
  returns updated **oleg.sopilnyak.test.endpoint.dto.organization.AuthorityPersonDto**, input parameter `AuthorityPersonDto person`
* **DELETE** **_/school/authorities/{personId}_** - to delete school's authority person by ID<BR>
  calls the`AuthorityPersonFacade#deleteAuthorityPersonById(id)`<BR>
  no return, parameter `String personId`
#### FacultiesRestController
* **GET** **_/school/faculties_** - to get all school's faculties<BR>
  calls the`FacultyFacade#findAllFaculties()`<BR>
  returns **List&lt;oleg.sopilnyak.test.endpoint.dto.organization.FacultyDto&gt;**, no input parameters
* **GET** **_/school/faculties/{facultyId}_** - to get school's faculty by ID<BR>
  calls the`FacultyFacade#findFacultyById(id)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.organization.FacultyDto**, input parameter `String facultyId`
* **POST** **_/school/faculties_** - to create the faculty<BR>
  calls the`FacultyFacade#createOrUpdateFaculty(facultyDto)`<BR>
  returns created **oleg.sopilnyak.test.endpoint.dto.organization.FacultyDto**, input parameter `FacultyDto facultyDto`
* **PUT** **_/school/faculties_** - to update the faculty<BR>
  calls the`FacultyFacade#createOrUpdateFaculty(facultyDto)`<BR>
  returns updated **oleg.sopilnyak.test.endpoint.dto.organization.FacultyDto**, input parameter `FacultyDto facultyDto`
* **DELETE** **_/school/faculties_** - to delete school's faculty by instance<BR>
  calls the`FacultyFacade#deleteFaculty(facultyDto)`<BR>
  no return, input parameter `oleg.sopilnyak.test.endpoint.dto.organization.FacultyDto facultyDto`
* **DELETE** **_/school/faculties/{facultyId}_** - to delete school's faculty by ID<BR>
  calls the`FacultyFacade#deleteFacultyById(id)`<BR>
  no return, input parameter `String facultyId`
#### StudentsGroupsRestController
* **GET** **_/school/student-groups_** - to get all school's students groups<BR>
  calls the`StudentsGroupFacade#findAllStudentsGroups()`<BR>
  returns **List&lt;oleg.sopilnyak.test.endpoint.dto.organization.StudentsGroupDto&gt;**, no input parameter
* **GET** **_/school/student-groups/{groupId}_** - to get school's students group by ID<BR>
  calls the`StudentsGroupFacade#findStudentsGroupById(id)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.organization.StudentsGroupDto**, input parameter `String groupId`
* **POST** **_/school/student-groups_** - to create the students group<BR>
  calls the`StudentsGroupFacade#createOrUpdateStudentsGroup(studentsGroupDto)`<BR>
  returns created **oleg.sopilnyak.test.endpoint.dto.organization.StudentsGroupDto**, input parameter `StudentsGroupDto studentGroupDto`
* **PUT** **_/school/student-groups_** - to update the students group<BR>
  calls the`StudentsGroupFacade#createOrUpdateStudentsGroup(studentsGroupDto)`<BR>
  returns updated **oleg.sopilnyak.test.endpoint.dto.organization.StudentsGroupDto**, input parameter `StudentsGroupDto studentGroupDto`
* **DELETE** **_/school/student-groups/{groupId}_** - to delete school's students group by ID<BR>
  calls the`StudentsGroupFacade#deleteStudentsGroupById(id)`<BR>
  no return, input parameter `String groupId`
* ### profile
#### PrincipalProfileRestController
* **GET** **_/profiles/principals/{personProfileId}_** - to get school's principal profile by ID<BR>
  calls the`PrincipalProfileFacade#findPrincipalProfileById(id)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.profile.PrincipalProfileDto**, input parameter `String personProfileId`
* **PUT** **_/profiles/principals_** - to update the principal profile<BR>
  calls the`PrincipalProfileFacade#createOrUpdateProfile(profileDto))`<BR>
  returns updated **oleg.sopilnyak.test.endpoint.dto.profile.PrincipalProfileDto**, input parameter `PrincipalProfileDto profileDto`
#### StudentProfileRestController
* **GET** **_/profiles/students/{personProfileId}_** - to get school's student profile by ID<BR>
  calls the`StudentProfileFacade#findStudentProfileById(id)`<BR>
  returns **oleg.sopilnyak.test.endpoint.dto.profile.StudentProfileDto**, input parameter `String personProfileId`
* **PUT** **_/profiles/students_** - to update the student profile<BR>
  calls the`StudentProfileFacade#createOrUpdateProfile(profileDto))`<BR>
  returns updated **oleg.sopilnyak.test.endpoint.dto.profile.StudentProfileDto**, input parameter `StudentProfileDto profileDto`

### Module Data Model
![Endpoint Layer General Diagram](diagrams/Endpoints%20Layer%20Model%20Diagram.png "Endpoint Layer Diagram")
