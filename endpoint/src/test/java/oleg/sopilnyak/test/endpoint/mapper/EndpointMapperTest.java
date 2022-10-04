package oleg.sopilnyak.test.endpoint.mapper;

import lombok.Builder;
import lombok.Data;
import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointMapperTest {

    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @Test
    void shouldTransferStudentToDto() {
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
    void shouldTransferCourseToDto() {
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

    private void assertStudentLists(List<Student> expected, List<Student> result) {
        if (ObjectUtils.isEmpty(expected)) {
            assertThat(ObjectUtils.isEmpty(result)).isTrue();
            return;
        }
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertStudentEquals(expected.get(i), result.get(i)));
    }

    private void assertCourseLists(List<Course> expected, List<Course> result) {
        if (ObjectUtils.isEmpty(expected)) {
            assertThat(ObjectUtils.isEmpty(result)).isTrue();
            return;
        }
        assertThat(expected.size()).isEqualTo(result.size());
        IntStream.range(0, expected.size()).forEach(i -> assertCourseEquals(expected.get(i), result.get(i)));
    }

    private void assertCourseEquals(Course expected, Course result) {
        assertThat(expected.getId()).isEqualTo(result.getId());
        assertThat(expected.getName()).isEqualTo(result.getName());
        assertThat(expected.getDescription()).isEqualTo(result.getDescription());
    }

    private void assertStudentEquals(Student expected, Student result) {
        assertThat(expected.getId()).isEqualTo(result.getId());
        assertThat(expected.getFirstName()).isEqualTo(result.getFirstName());
        assertThat(expected.getLastName()).isEqualTo(result.getLastName());
        assertThat(expected.getGender()).isEqualTo(result.getGender());
        assertThat(expected.getDescription()).isEqualTo(result.getDescription());
    }

    private List<Course> makeCourses(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeCourse(i + 1)).toList();
    }

    private Course makeCourse(int i) {
        return FakeCourse.builder()
                .id(i + 200L)
                .name("name-" + i)
                .description("description-" + i)
                .students(Collections.emptyList())
                .build();
    }

    private List<Student> makeStudents(int count) {
        return IntStream.range(0, count).mapToObj(i -> makeStudent(i + 1)).toList();
    }

    private Student makeStudent(int i) {
        return FakeStudent.builder()
                .id(i + 200L).firstName("firstName-" + i).lastName("lastName-" + i)
                .gender("gender-" + i).description("description-" + i)
                .courses(Collections.emptyList())
                .build();
    }


    @Data
    @Builder
    private static class FakeStudent implements Student {
        private Long id;
        private String firstName;
        private String lastName;
        private String gender;
        private String description;
        private List<Course> courses;
    }

    @Data
    @Builder
    private static class FakeCourse implements Course {
        private Long id;
        private String name;
        private String description;
        private List<Student> students;
    }
}