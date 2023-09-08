package oleg.sopilnyak.test.endpoint.mapper;

import oleg.sopilnyak.test.endpoint.dto.*;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointMapperTest extends TestModelFactory {
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

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
                .courses(courses)
                .build();

        StudentDto dto = mapper.toDto(student);

        assertThat(id).isEqualTo(dto.getId());
        assertThat(firstName).isEqualTo(dto.getFirstName());
        assertThat(lastName).isEqualTo(dto.getLastName());
        assertThat(gender).isEqualTo(dto.getGender());
        assertThat(description).isEqualTo(dto.getDescription());
        assertCourseLists(courses, dto.getCourses());
    }

    @Test
    void shouldTransformCourseToDto() {
        Long id = 101L;
        String name = "courseName";
        String description = "description";
        List<Student> students = makeStudents(50);
        Course course = FakeCourse.builder()
                .id(id).name(name).description(description).students(students)
                .build();

        CourseDto dto = mapper.toDto(course);
        assertThat(id).isEqualTo(dto.getId());
        assertThat(name).isEqualTo(dto.getName());
        assertThat(description).isEqualTo(dto.getDescription());
        assertStudentLists(students, dto.getStudents());
    }

    @Test
    void shouldTransformAuthorityPersonToDto() {
        Long id = 102L;
        String firstName = "Mary";
        String lastName = "Bell";
        String title = "Professor";
        String gender = "Mrs.";
        AuthorityPerson person = FakeAuthorityPerson.builder()
                .id(id)
                .title(title)
                .firstName(firstName)
                .lastName(lastName)
                .gender(gender)
                .build();

        AuthorityPersonDto dto = mapper.toDto(person);

        assertThat(id).isEqualTo(dto.getId());
        assertThat(title).isEqualTo(dto.getTitle());
        assertThat(firstName).isEqualTo(dto.getFirstName());
        assertThat(lastName).isEqualTo(dto.getLastName());
        assertThat(gender).isEqualTo(dto.getGender());
    }

    @Test
    void shouldTransformFacultyToDto() {
        Long id = 103L;
        String name = "English";
        AuthorityPerson dean = FakeAuthorityPerson.builder()
                .id(-101L).title("title").firstName("firstName").lastName("lastName").gender("gender")
                .build();
        List<Course> courses = makeCourses(5);

        Faculty faculty = FakeFaculty.builder()
                .id(id)
                .name(name)
                .dean(dean)
                .courses(courses)
                .build();

        FacultyDto dto = mapper.toDto(faculty);

        assertThat(id).isEqualTo(dto.getId());
        assertThat(name).isEqualTo(dto.getName());
        assertAuthorityPersonEquals(dean, dto.getDean());
        assertCourseLists(courses, dto.getCourses());
    }

    @Test
    void shouldTransformStudentsGroupToDto() {
        Long id = 104L;
        String name = "Hawks-2020";
        Student leader = makeStudent(100);
        List<Student> students = makeStudents(20);

        StudentsGroup group = FakeStudentsGroup.builder()
                .id(id)
                .name(name)
                .leader(leader)
                .students(students)
                .build();

        StudentsGroupDto dto = mapper.toDto(group);

        assertThat(id).isEqualTo(dto.getId());
        assertThat(name).isEqualTo(dto.getName());
        assertStudentEquals(leader, dto.getLeader());
        assertStudentLists(students, dto.getStudents());
    }
}