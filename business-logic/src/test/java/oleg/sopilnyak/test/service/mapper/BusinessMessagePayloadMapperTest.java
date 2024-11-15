package oleg.sopilnyak.test.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.message.payload.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessMessagePayloadMapperTest extends TestModelFactory {
    private final BusinessMessagePayloadMapper mapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    void shouldTransformAuthorityPersonToPayload_Directly() {
        Long id = 102L;
        List<Faculty> faculties = new ArrayList<>(makeFaculties(10));
        AuthorityPerson source = FakeAuthorityPerson.builder()
                .id(id).title("Professor").firstName("Mary").lastName("Bell").gender("Mrs.")
                .faculties(faculties).build();

        AuthorityPersonPayload payload = mapper.toPayload(source);

        assertAuthorityPersonEquals(payload, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformAuthorityPersonToPayload_UsingJson() throws JsonProcessingException {
        Long id = 112L;
        List<Faculty> faculties = new ArrayList<>(makeFaculties(10));
        AuthorityPerson source = FakeAuthorityPerson.builder()
                .id(id).title("Professor").firstName("Mary").lastName("Bell").gender("Mrs.")
                .faculties(faculties).build();

        AuthorityPersonPayload payload = mapper.toPayload(source);
        String jsonString = jsonMapper.writeValueAsString(payload);
        AuthorityPersonPayload restored = jsonMapper.readValue(jsonString, AuthorityPersonPayload.class);

        assertAuthorityPersonEquals(restored, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(restored.getOriginal()).isNull();

        AuthorityPersonPayload triple = mapper.toPayload(mapper.toPayload(mapper.toPayload(source)));
        assertThat(triple.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformCourseToPayload_Directly() {
        Long id = 101L;
        List<Student> students = makeStudents(30);
        Course source = FakeCourse.builder()
                .id(id).name("courseName").description("description").students(students)
                .build();

        CoursePayload payload = mapper.toPayload(source);

        assertCourseEquals(payload, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformCourseToPayload_UsingJson() throws JsonProcessingException {
        Long id = 111L;
        List<Student> students = makeStudents(30);
        Course source = FakeCourse.builder()
                .id(id).name("courseName").description("description").students(students)
                .build();

        CoursePayload payload = mapper.toPayload(source);
        String jsonString = jsonMapper.writeValueAsString(payload);
        CoursePayload restored = jsonMapper.readValue(jsonString, CoursePayload.class);

        assertCourseEquals(restored, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(restored.getOriginal()).isNull();

        CoursePayload triple = mapper.toPayload(mapper.toPayload(mapper.toPayload(source)));
        assertThat(triple.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformFacultyToPayload_Directly() {
        Long id = 103L;
        List<Course> courses = makeCourses(5);
        Faculty source = FakeFaculty.builder().id(id).name("English").courses(courses).build();

        FacultyPayload payload = mapper.toPayload(source);

        assertFacultyEquals(payload, source);
        assertThat(payload.getOriginal()).isSameAs(source);
    }

    @Test
    void shouldTransformFacultyToPayload_UsingJson() throws JsonProcessingException {
        Long id = 113L;
        List<Course> courses = makeCourses(5);
        Faculty source = FakeFaculty.builder().id(id).name("English").courses(courses).build();

        FacultyPayload payload = mapper.toPayload(source);
        String jsonString = jsonMapper.writeValueAsString(payload);
        FacultyPayload restored = jsonMapper.readValue(jsonString, FacultyPayload.class);

        assertFacultyEquals(restored, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(restored.getOriginal()).isNull();

        FacultyPayload triple = mapper.toPayload(mapper.toPayload(mapper.toPayload(source)));
        assertThat(triple.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformPrincipalProfileToPayload_Directly() {
        Long id = 106L;
        PrincipalProfile source = FakePrincipalProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .login("login-" + id)
                .build();

        PrincipalProfilePayload payload = mapper.toPayload(source);

        assertProfilesEquals(payload, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformPrincipalProfileToPayload_UsingJson() throws JsonProcessingException {
        Long id = 116L;
        PrincipalProfile source = FakePrincipalProfile.builder()
                .id(id).email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .login("login-" + id)
                .build();

        PrincipalProfilePayload payload = mapper.toPayload(source);
        String jsonString = jsonMapper.writeValueAsString(payload);
        PrincipalProfilePayload restored = jsonMapper.readValue(jsonString, PrincipalProfilePayload.class);

        assertProfilesEquals(restored, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(restored.getOriginal()).isNull();

        PrincipalProfilePayload triple = mapper.toPayload(mapper.toPayload(mapper.toPayload(source)));
        assertThat(triple.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformStudentToPayload_Directly() {
        Long id = 100L;
        List<Course> courses = makeCourses(5);
        Student source = FakeStudent.builder().id(id).courses(courses)
                .firstName("firstName").lastName("lastName").gender("gender").description("description")
                .build();

        StudentPayload payload = mapper.toPayload(source);

        assertStudentEquals(payload, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformStudentToPayload_UsingJson() throws JsonProcessingException {
        Long id = 110L;
        List<Course> courses = makeCourses(5);
        Student source = FakeStudent.builder().id(id).courses(courses)
                .firstName("firstName").lastName("lastName").gender("gender").description("description")
                .build();

        StudentPayload payload = mapper.toPayload(source);
        String jsonString = jsonMapper.writeValueAsString(payload);
        StudentPayload restored = jsonMapper.readValue(jsonString, StudentPayload.class);

        assertStudentEquals(restored, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(restored.getOriginal()).isNull();

        StudentPayload triple = mapper.toPayload(mapper.toPayload(mapper.toPayload(source)));
        assertThat(triple.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformStudentProfileToPayload_Directly() {
        Long id = 105L;
        StudentProfile source = FakeStudentsProfile.builder().id(id)
                .email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .build();

        StudentProfilePayload payload = mapper.toPayload(source);

        assertProfilesEquals(payload, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformStudentProfileToPayload_UsingJson() throws JsonProcessingException {
        Long id = 115L;
        StudentProfile source = FakeStudentsProfile.builder().id(id)
                .email("email@email").phone("phone").location("location").photoUrl("photo-url")
                .build();

        StudentProfilePayload payload = mapper.toPayload(source);
        String jsonString = jsonMapper.writeValueAsString(payload);
        StudentProfilePayload restored = jsonMapper.readValue(jsonString, StudentProfilePayload.class);

        assertProfilesEquals(restored, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(restored.getOriginal()).isNull();

        StudentProfilePayload triple = mapper.toPayload(mapper.toPayload(mapper.toPayload(source)));
        assertThat(triple.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformStudentsGroupToPayload_Directly() {
        Long id = 104L;
        List<Student> students = makeStudents(20);
        StudentsGroup source = FakeStudentsGroup.builder()
                .id(id).name("Hawks-2020").leader(students.get(0)).students(students)
                .build();

        StudentsGroupPayload payload = mapper.toPayload(source);

        assertStudentsGroupEquals(payload, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }

    @Test
    void shouldTransformStudentsGroupToPayload_UsingJson() throws JsonProcessingException {
        Long id = 114L;
        List<Student> students = makeStudents(10);
        StudentsGroup source = FakeStudentsGroup.builder()
                .id(id).name("Hawks-2020").leader(students.get(0)).students(students)
                .build();

        StudentsGroupPayload payload = mapper.toPayload(source);
        String jsonString = jsonMapper.writeValueAsString(payload);
        StudentsGroupPayload restored = jsonMapper.readValue(jsonString, StudentsGroupPayload.class);

        assertStudentsGroupEquals(restored, source);
        assertThat(payload.getOriginal()).isSameAs(source);
        assertThat(restored.getOriginal()).isNull();

        StudentsGroupPayload triple = mapper.toPayload(mapper.toPayload(mapper.toPayload(source)));
        assertThat(triple.getOriginal()).isSameAs(source);
        assertThat(payload.getOriginalType()).isEqualTo(source.getClass().getName());
    }
}