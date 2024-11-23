package oleg.sopilnyak.test.endpoint.mapper;

import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Comparator;
import java.util.List;

class EndpointMapperTest extends TestModelFactory {
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @Test
    void shouldTransformAuthorityPersonToDto() {
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

        AuthorityPersonDto dto = mapper.toDto(person);

        assertAuthorityPersonEquals(dto, person);
    }

    @Test
    void shouldTransformCourseToDto() {
        Long id = 101L;
        String name = "courseName";
        String description = "description";
        List<Student> students =
                makeStudents(50).stream().sorted(Comparator.comparing(Student::getFullName)).toList()
        ;
        Course course = FakeCourse.builder()
                .id(id).name(name).description(description).students(students)
                .build();

        CourseDto dto = mapper.toDto(course);

        assertCourseEquals(dto, course);
    }

    @Test
    void shouldTransformFacultyToDto() {
        Long id = 103L;
        String name = "English";
        List<Course> courses = makeCourses(5);

        Faculty faculty = FakeFaculty.builder()
                .id(id).name(name).courses(courses)
                .build();

        FacultyDto dto = mapper.toDto(faculty);

        assertFacultyEquals(dto, faculty);
    }

    @Test
    void shouldTransformPrincipalProfileToDto() {
        Long id = 106L;
        PrincipalProfile profile = FakePrincipalProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .login("login-" + id)
                .build();

        PrincipalProfileDto dto = mapper.toDto(profile);

        assertProfilesEquals(dto, profile);
    }

    @Test
    void shouldTransformStudentToDto() {
        Long id = 100L;
        String firstName = "firstName";
        String lastName = "lastName";
        String gender = "gender";
        String description = "description";
        List<Course> courses = makeCourses(5);
        Student student = FakeStudent.builder()
                .id(id).firstName(firstName).lastName(lastName).gender(gender).description(description)
                .courses(courses).build();

        StudentDto dto = mapper.toDto(student);

        assertStudentEquals(dto, student);
    }

    @Test
    void shouldTransformStudentProfileToDto() {
        Long id = 105L;
        StudentProfile profile = FakeStudentsProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .build();

        StudentProfileDto dto = mapper.toDto(profile);

        assertProfilesEquals(dto, profile);
    }

    @Test
    void shouldTransformStudentsGroupToDto() {
        Long id = 104L;
        String name = "Hawks-2020";
        Student leader = makeStudent(100);
        List<Student> students = makeStudents(20);

        StudentsGroup group = FakeStudentsGroup.builder()
                .id(id).name(name).leader(leader).students(students)
                .build();

        StudentsGroupDto dto = mapper.toDto(group);

        assertStudentsGroupEquals(dto, group);
    }

}