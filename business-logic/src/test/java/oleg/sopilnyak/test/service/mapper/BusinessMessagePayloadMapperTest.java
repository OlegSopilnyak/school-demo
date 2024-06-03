package oleg.sopilnyak.test.service.mapper;

import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.message.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Comparator;
import java.util.List;

class BusinessMessagePayloadMapperTest extends TestModelFactory {
    private final BusinessMessagePayloadMapper mapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);

    @Test
    void shouldTransformAuthorityPersonToPayload() {
        Long id = 102L;
        String firstName = "Mary";
        String lastName = "Bell";
        String title = "Professor";
        String gender = "Mrs.";
        List<Faculty> faculties =
                makeFaculties(10).stream().sorted(Comparator.comparing(Faculty::getName)).toList();
        AuthorityPerson person = FakeAuthorityPerson.builder()
                .id(id).title(title).firstName(firstName).lastName(lastName).gender(gender)
                .faculties(faculties).build();

        AuthorityPersonPayload payload = mapper.toPayload(person);

        assertAuthorityPersonEquals(payload, person);
    }

    @Test
    void shouldTransformCourseToPayload() {
        Long id = 101L;
        String name = "courseName";
        String description = "description";
        List<Student> students =
                makeStudents(50).stream().sorted(Comparator.comparing(Student::getFullName)).toList();
        Course course = FakeCourse.builder()
                .id(id).name(name).description(description).students(students)
                .build();

        CoursePayload payload = mapper.toPayload(course);

        assertCourseEquals(payload, course);
    }

    @Test
    void shouldTransformFacultyToPayload() {
        Long id = 103L;
        String name = "English";
        List<Course> courses = makeCourses(5);

        Faculty faculty = FakeFaculty.builder()
                .id(id).name(name).courses(courses)
                .build();

        FacultyPayload payload = mapper.toPayload(faculty);

        assertFacultyEquals(payload, faculty);
    }

    @Test
    void shouldTransformPrincipalProfileToPayload() {
        Long id = 106L;
        PrincipalProfile profile = FakePrincipalProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .login("login-" + id)
                .build();

        PrincipalProfilePayload payload = mapper.toPayload(profile);

        assertProfilesEquals(payload, profile);
    }

    @Test
    void shouldTransformStudentToPayload() {
        Long id = 100L;
        String firstName = "firstName";
        String lastName = "lastName";
        String gender = "gender";
        String description = "description";
        List<Course> courses = makeCourses(5);
        Student student = FakeStudent.builder()
                .id(id).firstName(firstName).lastName(lastName).gender(gender).description(description)
                .courses(courses).build();

        StudentPayload payload = mapper.toPayload(student);

        assertStudentEquals(payload, student);
    }

    @Test
    void shouldTransformStudentProfileToPayload() {
        Long id = 105L;
        StudentProfile profile = FakeStudentsProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .build();

        StudentProfilePayload payload = mapper.toPayload(profile);

        assertProfilesEquals(payload, profile);
    }

    @Test
    void shouldTransformStudentsGroupToPayload() {
        Long id = 104L;
        String name = "Hawks-2020";
        List<Student> students = makeStudents(20);

        StudentsGroup group = FakeStudentsGroup.builder()
                .id(id).name(name).leader(students.get(0)).students(students)
                .build();

        StudentsGroupPayload payload = mapper.toPayload(group);

        assertStudentsGroupEquals(payload, group);
    }


}