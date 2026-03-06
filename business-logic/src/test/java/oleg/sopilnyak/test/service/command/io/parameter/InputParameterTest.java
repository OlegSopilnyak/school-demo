package oleg.sopilnyak.test.service.command.io.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;
import oleg.sopilnyak.test.service.command.io.CompositeInput;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.BasePayload;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class InputParameterTest {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void shouldCreateEmptyParameter() {

        Input<Void> parameter = Input.emptyParameter();

        assertThat(parameter.isEmpty()).isTrue();
        assertThat(parameter.value()).isNull();
        assertThat(parameter).isInstanceOf(EmptyParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreEmptyParameter() throws JsonProcessingException {

        Input<Void> parameter = Input.emptyParameter();

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(EmptyParameter.class.getName());
        EmptyParameter restored = objectMapper.readValue(json, EmptyParameter.class);

        assertThat(restored.isEmpty()).isTrue();
        assertThat(restored.value()).isNull();
        assertThat(restored).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateLongIdParameter() {
        long id = 1L;

        Input<Long> parameter = Input.of(id);

        assertThat(parameter.value()).isSameAs(id);
        assertThat(parameter).isInstanceOf(NumberIdParameter.class);
    }

    @Test
    void shouldRestoreLongIdParameter() throws JsonProcessingException {
        long id = 2L;

        Input<Long> parameter = Input.of(id);

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(NumberIdParameter.class.getName());
        NumberIdParameter<?> restored = objectMapper.readValue(json, NumberIdParameter.class);

        assertThat(restored.value()).isEqualTo(id);
        assertThat(restored).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStringIdParameter() {
        long id = 3L;
        String stringId = ":" + id;

        Input<String> parameter = Input.of(stringId);

        assertThat(parameter.value()).isSameAs(stringId);
        assertThat(parameter).isInstanceOf(StringParameter.class);
    }

    @Test
    void shouldRestoreStringIdParameter() throws JsonProcessingException {
        long id = 4L;
        String stringId = ":" + id;

        Input<String> parameter = Input.of(stringId);

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StringParameter.class.getName());
        StringParameter restored = objectMapper.readValue(json, StringParameter.class);

        assertThat(restored.value()).isEqualTo(stringId);
        assertThat(restored).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStudentPayloadParameter() {
        long id = 30L;
        long courseId = 31L;
        CoursePayload course = createCourse(courseId);
        StudentPayload entity = createStudent(id);
        entity.setCourses(List.of(course));

        Input<StudentPayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isSameAs(entity);
        assertThat(parameter.value().getCourses()).contains(course);
        assertThat(parameter).isInstanceOf(PayloadParameter.class);
    }

    @Test
    void shouldRestoreStudentPayloadParameter() throws JsonProcessingException {
        long id = 301L;
        long courseId = 311L;
        CoursePayload course = createCourse(courseId);
        StudentPayload entity = createStudent(id);
        entity.setCourses(List.of(course));
        Input<StudentPayload> parameter = Input.of(entity);

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PayloadParameter.class.getName()).contains(StudentPayload.class.getName());

        TypeReference<PayloadParameter<StudentPayload>> typeReference = new TypeReference<>() {
        };
        PayloadParameter<StudentPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getCourses()).contains(course);
        assertThat(restored).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateCoursePayloadParameter() {
        long id = 32L;
        long courseId = 33L;
        CoursePayload entity = createCourse(courseId);
        StudentPayload student = createStudent(id);
        entity.setStudents(List.of(student));
        Input<CoursePayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isSameAs(entity);
        assertThat(parameter.value().getStudents()).contains(student);
        assertThat(parameter).isInstanceOf(PayloadParameter.class);
    }

    @Test
    void shouldRestoreCoursePayloadParameter() throws JsonProcessingException {
        long id = 321L;
        long courseId = 331L;
        CoursePayload entity = createCourse(courseId);
        StudentPayload student = createStudent(id);
        entity.setStudents(List.of(student));
        Input<CoursePayload> parameter = Input.of(entity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PayloadParameter.class.getName()).contains(CoursePayload.class.getName());

        TypeReference<PayloadParameter<CoursePayload>> typeReference = new TypeReference<>() {
        };
        PayloadParameter<CoursePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored).isInstanceOf(Input.class);
        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getStudents()).contains(student);
    }

    @Test
    void shouldCreateAuthorityPersonPayloadParameter() {
        long id = 34L;
        long anotherId = 35L;
        AuthorityPersonPayload entity = createAuthorityPerson(id);
        FacultyPayload faculty = createFaculty(anotherId);
        entity.setFaculties(List.of(faculty));

        Input<AuthorityPersonPayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isSameAs(entity);
        assertThat(parameter.value().getFaculties()).contains(faculty);
        assertThat(parameter).isInstanceOf(PayloadParameter.class);
    }

    @Test
    void shouldRestoreAuthorityPersonPayloadParameter() throws JsonProcessingException {
        long id = 341L;
        long anotherId = 351L;
        AuthorityPersonPayload entity = createAuthorityPerson(id);
        FacultyPayload faculty = createFaculty(anotherId);
        entity.setFaculties(List.of(faculty));

        Input<AuthorityPersonPayload> parameter = Input.of(entity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PayloadParameter.class.getName()).contains(AuthorityPersonPayload.class.getName());

        TypeReference<PayloadParameter<AuthorityPersonPayload>> typeReference = new TypeReference<>() {
        };
        PayloadParameter<AuthorityPersonPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored).isInstanceOf(Input.class);
        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getFaculties()).contains(faculty);
    }

    @Test
    void shouldCreateFacultyPayloadParameter() {
        long id = 36L;
        long anotherId = 37L;
        FacultyPayload entity = createFaculty(id);
        CoursePayload course = createCourse(anotherId);
        entity.setCourses(List.of(course));

        Input<FacultyPayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isSameAs(entity);
        assertThat(parameter).isInstanceOf(PayloadParameter.class);
    }

    @Test
    void shouldRestoreFacultyPayloadParameter() throws JsonProcessingException {
        long id = 361L;
        long anotherId = 371L;
        FacultyPayload entity = createFaculty(id);
        CoursePayload course = createCourse(anotherId);
        entity.setCourses(List.of(course));

        Input<FacultyPayload> parameter = Input.of(entity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PayloadParameter.class.getName()).contains(FacultyPayload.class.getName());

        TypeReference<PayloadParameter<FacultyPayload>> typeReference = new TypeReference<>() {
        };
        PayloadParameter<FacultyPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored).isInstanceOf(Input.class);
        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getCourses()).contains(course);
    }

    @Test
    void shouldCreateStudentProfilePayloadParameter() {
        long id = 40L;
        StudentProfilePayload entity = createStudentProfile(id);

        Input<StudentProfilePayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isSameAs(entity);
        assertThat(parameter).isInstanceOf(PayloadParameter.class);
    }

    @Test
    void shouldRestoreStudentProfilePayloadParameter() throws JsonProcessingException {
        long id = 401L;
        StudentProfilePayload entity = createStudentProfile(id);

        Input<StudentProfilePayload> parameter = Input.of(entity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PayloadParameter.class.getName()).contains(StudentProfilePayload.class.getName());

        TypeReference<PayloadParameter<StudentProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadParameter<StudentProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored).isInstanceOf(Input.class);
        assertThat(restored.value()).isEqualTo(entity);
    }

    @Test
    void shouldCreatePrincipalProfilePayloadParameter() {
        long id = 41L;
        PrincipalProfilePayload entity = createPrincipalProfile(id);

        Input<PrincipalProfilePayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isSameAs(entity);
        assertThat(parameter).isInstanceOf(PayloadParameter.class);
    }

    @Test
    void shouldRestorePrincipalProfilePayloadParameter() throws JsonProcessingException {
        long id = 411L;
        PrincipalProfilePayload entity = createPrincipalProfile(id);

        Input<PrincipalProfilePayload> parameter = Input.of(entity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PayloadParameter.class.getName()).contains(PrincipalProfilePayload.class.getName());

        TypeReference<PayloadParameter<PrincipalProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadParameter<PrincipalProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored).isInstanceOf(Input.class);
        assertThat(restored.value()).isEqualTo(entity);
    }

    @Test
    void shouldCreateStudentsGroupPayloadParameter() {
        long id = 38L;
        long anotherId = 39L;
        StudentsGroupPayload entity = createStudentsGroup(id);
        StudentPayload student = createStudent(anotherId);
        entity.setStudents(List.of(student));

        Input<StudentsGroupPayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isSameAs(entity);
        assertThat(parameter).isInstanceOf(PayloadParameter.class);
    }

    @Test
    void shouldRestoreStudentsGroupPayloadParameter() throws JsonProcessingException {
        long id = 381L;
        long anotherId = 391L;
        StudentsGroupPayload entity = createStudentsGroup(id);
        StudentPayload student = createStudent(anotherId);
        entity.setStudents(List.of(student));

        Input<StudentsGroupPayload> parameter = Input.of(entity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PayloadParameter.class.getName()).contains(StudentsGroupPayload.class.getName());

        TypeReference<PayloadParameter<StudentsGroupPayload>> typeReference = new TypeReference<>() {
        };
        PayloadParameter<StudentsGroupPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored).isInstanceOf(Input.class);
        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getStudents()).contains(student);
    }

    @Test
    void shouldCreateCompositeInputParameter() {
        long longId = 1000L;
        String stringId = "string-id";

        var parameter = Input.of(Input.of(longId), Input.of(stringId));

        assertThat(parameter.value()[0].value()).isEqualTo(longId);
        assertThat(parameter.value()[1].value()).isEqualTo(stringId);
        assertThat(parameter).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreCompositeInputParameter() throws JsonProcessingException {
        long longId = 1000L;
        String stringId = "string-id";

        var parameter = Input.of(Input.of(longId), Input.of(stringId));
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(CompositeInputParameter.class.getName());
        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(parameter.value()[0].value()).isEqualTo(longId);
        assertThat(parameter.value()[1].value()).isEqualTo(stringId);
        assertThat(restored).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateLongIdPairParameter() {
        long firstId = 5L;
        long secondId = 6L;

        CompositeInput<Long> parameter = Input.of(firstId, secondId);

        assertThat(parameter.value()[0].value()).isSameAs(firstId);
        assertThat(parameter.value()[1].value()).isSameAs(secondId);
        assertThat(parameter).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreLongIdPairParameter() throws JsonProcessingException {
        long firstId = 7L;
        long secondId = 8L;

        var parameter = Input.of(firstId, secondId);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(CompositeInputParameter.class.getName());
        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstId);
        assertThat(restored.value()[1].value()).isEqualTo(secondId);
        assertThat(restored).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStringsPairParameter() {
        String first = "first";
        String second = "second";

        CompositeInput<String> parameter = Input.of(first, second);

        assertThat(parameter.value()[0].value()).isSameAs(first);
        assertThat(parameter.value()[1].value()).isSameAs(second);
        assertThat(parameter).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreStringsPairParameter() throws JsonProcessingException {
        String first = "first";
        String second = "second";

        var parameter = Input.of(first, second);

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(CompositeInputParameter.class.getName());
        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(first);
        assertThat(restored.value()[1].value()).isEqualTo(second);
        assertThat(restored).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStudentPairParameter() {
        long firstId = 11L;
        long secondId = 12L;
        StudentPayload firstEntity = createStudent(firstId);
        CoursePayload firstCourse = createCourse(firstId + 10);
        firstEntity.setCourses(List.of(firstCourse));
        StudentPayload secondEntity = createStudent(secondId);
        CoursePayload secondCourse = createCourse(secondId + 10);
        secondEntity.setCourses(List.of(secondCourse));

        CompositeInput<BasePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value()[0].value()).isSameAs(firstEntity);
        assertThat(((Student) parameter.value()[0].value()).getCourses()).contains(firstCourse);
        assertThat(parameter.value()[1].value()).isSameAs(secondEntity);
        assertThat(((Student) parameter.value()[1].value()).getCourses()).contains(secondCourse);
        assertThat(parameter).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreStudentPairParameter() throws JsonProcessingException {
        long firstId = 111L;
        long secondId = 121L;
        StudentPayload firstEntity = createStudent(firstId);
        CoursePayload firstCourse = createCourse(firstId + 10);
        firstEntity.setCourses(List.of(firstCourse));
        StudentPayload secondEntity = createStudent(secondId);
        CoursePayload secondCourse = createCourse(secondId + 10);
        secondEntity.setCourses(List.of(secondCourse));

        var parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StudentPayload.class.getName());

        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstEntity);
        assertThat(((Student) restored.value()[0].value()).getCourses()).contains(firstCourse);
        assertThat(restored.value()[1].value()).isEqualTo(secondEntity);
        assertThat(((Student) restored.value()[1].value()).getCourses()).contains(secondCourse);
        assertThat(restored).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateCoursePairParameter() {
        long firstId = 13L;
        long secondId = 14L;
        StudentPayload firstEntity = createStudent(firstId);
        CoursePayload firstCourse = createCourse(firstId + 10);
        firstCourse.setStudents(List.of(firstEntity));
        StudentPayload secondEntity = createStudent(secondId);
        CoursePayload secondCourse = createCourse(secondId + 10);
        secondCourse.setStudents(List.of(secondEntity));

        CompositeInput<BasePayload> parameter = Input.of(firstCourse, secondCourse);

        assertThat(parameter.value()[0].value()).isSameAs(firstCourse);
        assertThat(((Course) parameter.value()[0].value()).getStudents()).contains(firstEntity);
        assertThat(parameter.value()[1].value()).isSameAs(secondCourse);
        assertThat(((Course) parameter.value()[1].value()).getStudents()).contains(secondEntity);
        assertThat(parameter).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreCoursePairParameter() throws JsonProcessingException {
        long firstId = 131L;
        long secondId = 141L;
        StudentPayload firstEntity = createStudent(firstId);
        CoursePayload firstCourse = createCourse(firstId + 10);
        firstCourse.setStudents(List.of(firstEntity));
        StudentPayload secondEntity = createStudent(secondId);
        CoursePayload secondCourse = createCourse(secondId + 10);
        secondCourse.setStudents(List.of(secondEntity));

        var parameter = Input.of(firstCourse, secondCourse);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(CoursePayload.class.getName());

        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstCourse);
        assertThat(((Course) restored.value()[0].value()).getStudents()).contains(firstEntity);
        assertThat(restored.value()[1].value()).isEqualTo(secondCourse);
        assertThat(((Course) restored.value()[1].value()).getStudents()).contains(secondEntity);
        assertThat(restored).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateAuthorityPersonPairParameter() {
        long firstId = 15L;
        long secondId = 16L;
        AuthorityPersonPayload firstEntity = createAuthorityPerson(firstId);
        AuthorityPersonPayload secondEntity = createAuthorityPerson(secondId);
        FacultyPayload firstFaculty = createFaculty(firstId + 10);
        FacultyPayload secondFaculty = createFaculty(secondId + 10);
        firstEntity.setFaculties(List.of(firstFaculty));
        secondEntity.setFaculties(List.of(secondFaculty));

        CompositeInput<BasePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value()[0].value()).isSameAs(firstEntity);
        assertThat(((AuthorityPerson) parameter.value()[0].value()).getFaculties()).contains(firstFaculty);
        assertThat(parameter.value()[1].value()).isSameAs(secondEntity);
        assertThat(((AuthorityPerson) parameter.value()[1].value()).getFaculties()).contains(secondFaculty);
        assertThat(parameter).isInstanceOf(CompositeInputParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreAuthorityPersonPairParameter() throws JsonProcessingException {
        long firstId = 151L;
        long secondId = 161L;
        AuthorityPersonPayload firstEntity = createAuthorityPerson(firstId);
        AuthorityPersonPayload secondEntity = createAuthorityPerson(secondId);
        FacultyPayload firstFaculty = createFaculty(firstId + 10);
        FacultyPayload secondFaculty = createFaculty(secondId + 10);
        firstEntity.setFaculties(List.of(firstFaculty));
        secondEntity.setFaculties(List.of(secondFaculty));

        var parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(AuthorityPersonPayload.class.getName());

        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstEntity);
        assertThat(((AuthorityPerson) restored.value()[0].value()).getFaculties()).contains(firstFaculty);
        assertThat(restored.value()[1].value()).isEqualTo(secondEntity);
        assertThat(((AuthorityPerson) restored.value()[1].value()).getFaculties()).contains(secondFaculty);
        assertThat(restored).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateFacultyPairParameter() {
        long firstId = 17L;
        long secondId = 18L;
        FacultyPayload firstEntity = createFaculty(firstId);
        FacultyPayload secondEntity = createFaculty(secondId);
        CoursePayload firstCourse = createCourse(firstId + 10);
        CoursePayload secondCourse = createCourse(secondId + 10);
        AuthorityPersonPayload firstDean = createAuthorityPerson(firstId + 20);
        AuthorityPersonPayload secondDean = createAuthorityPerson(secondId + 20);
        firstEntity.setCourses(List.of(firstCourse));
        secondEntity.setCourses(List.of(secondCourse));
        firstEntity.setDean(firstDean);
        secondEntity.setDean(secondDean);

        CompositeInput<BasePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value()[0].value()).isSameAs(firstEntity);
        assertThat(((Faculty) parameter.value()[0].value()).getDean()).isSameAs(firstDean);
        assertThat(((Faculty) parameter.value()[0].value()).getCourses()).contains(firstCourse);
        assertThat(parameter.value()[1].value()).isSameAs(secondEntity);
        assertThat(((Faculty) parameter.value()[1].value()).getDean()).isSameAs(secondDean);
        assertThat(((Faculty) parameter.value()[1].value()).getCourses()).contains(secondCourse);
        assertThat(parameter).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreFacultyPairParameter() throws JsonProcessingException {
        long firstId = 171L;
        long secondId = 181L;
        FacultyPayload firstEntity = createFaculty(firstId);
        FacultyPayload secondEntity = createFaculty(secondId);
        CoursePayload firstCourse = createCourse(firstId + 10);
        CoursePayload secondCourse = createCourse(secondId + 10);
        AuthorityPersonPayload firstDean = createAuthorityPerson(firstId + 20);
        AuthorityPersonPayload secondDean = createAuthorityPerson(secondId + 20);
        firstEntity.setCourses(List.of(firstCourse));
        secondEntity.setCourses(List.of(secondCourse));
        firstEntity.setDean(firstDean);
        secondEntity.setDean(secondDean);

        var parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(FacultyPayload.class.getName());

        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstEntity);
        assertThat(((Faculty) restored.value()[0].value()).getDean()).isEqualTo(firstDean);
        assertThat(((Faculty) restored.value()[0].value()).getCourses()).contains(firstCourse);
        assertThat(restored.value()[1].value()).isEqualTo(secondEntity);
        assertThat(((Faculty) restored.value()[1].value()).getDean()).isEqualTo(secondDean);
        assertThat(((Faculty) restored.value()[1].value()).getCourses()).contains(secondCourse);
        assertThat(restored).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStudentsGroupPairParameter() {
        long firstId = 19L;
        long secondId = 20L;
        StudentsGroupPayload firstEntity = createStudentsGroup(firstId);
        StudentsGroupPayload secondEntity = createStudentsGroup(secondId);
        StudentPayload firstStudent = createStudent(firstId + 10);
        StudentPayload secondStudent = createStudent(secondId + 10);
        firstEntity.setStudents(List.of(firstStudent));
        firstEntity.setLeader(firstStudent);
        secondEntity.setStudents(List.of(secondStudent));
        secondEntity.setLeader(secondStudent);

        CompositeInput<BasePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value()[0].value()).isSameAs(firstEntity);
        assertThat(((StudentsGroup) parameter.value()[0].value()).getLeader()).isSameAs(firstStudent);
        assertThat(((StudentsGroup) parameter.value()[0].value()).getStudents()).contains(firstStudent);
        assertThat(parameter.value()[1].value()).isSameAs(secondEntity);
        assertThat(((StudentsGroup) parameter.value()[1].value()).getLeader()).isSameAs(secondStudent);
        assertThat(((StudentsGroup) parameter.value()[1].value()).getStudents()).contains(secondStudent);
        assertThat(parameter).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreStudentsGroupPairParameter() throws JsonProcessingException {
        long firstId = 191L;
        long secondId = 201L;
        StudentsGroupPayload firstEntity = createStudentsGroup(firstId);
        StudentsGroupPayload secondEntity = createStudentsGroup(secondId);
        StudentPayload firstStudent = createStudent(firstId + 10);
        StudentPayload secondStudent = createStudent(secondId + 10);
        firstEntity.setStudents(List.of(firstStudent));
        firstEntity.setLeader(firstStudent);
        secondEntity.setStudents(List.of(secondStudent));
        secondEntity.setLeader(secondStudent);

        var parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StudentsGroupPayload.class.getName());

        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstEntity);
        assertThat(((StudentsGroup) restored.value()[0].value()).getLeader()).isEqualTo(firstStudent);
        assertThat(((StudentsGroup) restored.value()[0].value()).getStudents()).contains(firstStudent);
        assertThat(restored.value()[1].value()).isEqualTo(secondEntity);
        assertThat(((StudentsGroup) restored.value()[1].value()).getLeader()).isEqualTo(secondStudent);
        assertThat(((StudentsGroup) restored.value()[1].value()).getStudents()).contains(secondStudent);
        assertThat(restored).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStudentProfilePairParameter() {
        long firstId = 21L;
        long secondId = 22L;
        StudentProfilePayload firstEntity = createStudentProfile(firstId);
        StudentProfilePayload secondEntity = createStudentProfile(secondId);

        CompositeInput<BasePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value()[0].value()).isSameAs(firstEntity);
        assertThat(parameter.value()[1].value()).isSameAs(secondEntity);
        assertThat(parameter).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreStudentProfilePairParameter() throws JsonProcessingException {
        long firstId = 211L;
        long secondId = 221L;
        StudentProfilePayload firstEntity = createStudentProfile(firstId);
        StudentProfilePayload secondEntity = createStudentProfile(secondId);

        var parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StudentProfilePayload.class.getName());

        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstEntity);
        assertThat(restored.value()[1].value()).isEqualTo(secondEntity);
        assertThat(restored).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreatePrincipalProfilePairParameter() {
        long firstId = 23L;
        long secondId = 24L;
        PrincipalProfilePayload firstEntity = createPrincipalProfile(firstId);
        PrincipalProfilePayload secondEntity = createPrincipalProfile(secondId);

        CompositeInput<BasePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value()[0].value()).isSameAs(firstEntity);
        assertThat(parameter.value()[1].value()).isSameAs(secondEntity);
        assertThat(parameter).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestorePrincipalProfilePairParameter() throws JsonProcessingException {
        long firstId = 231L;
        long secondId = 241L;
        PrincipalProfilePayload firstEntity = createPrincipalProfile(firstId);
        PrincipalProfilePayload secondEntity = createPrincipalProfile(secondId);

        var parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PrincipalProfilePayload.class.getName());

        var restored = objectMapper.readValue(json, CompositeInputParameter.class);

        assertThat(restored.value()[0].value()).isEqualTo(firstEntity);
        assertThat(restored.value()[1].value()).isEqualTo(secondEntity);
        assertThat(restored).isInstanceOf(CompositeInput.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStaffRole() {
        Role value = Role.TEACHER;

        var parameter = Input.of(value);

        assertThat(parameter.value()).isEqualTo(value);
        assertThat(parameter).isInstanceOf(StaffRoleParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreStaffRole() throws JsonProcessingException {
        Role value = Role.TEACHER;

        var parameter = Input.of(value);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StaffRoleParameter.class.getName());

        var restored = objectMapper.readValue(json, StaffRoleParameter.class);

        assertThat(restored.value()).isEqualTo(value);
        assertThat(restored).isInstanceOf(StaffRoleParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldMapPrincipalProfileInputToUsername() {
        long id = 1231L;
        PrincipalProfilePayload entity = createPrincipalProfile(id);

        Input<PrincipalProfilePayload> parameter = Input.of(entity);

        assertThat(parameter.value()).isEqualTo(entity);
        assertThat(parameter).isInstanceOf(PayloadParameter.class).isInstanceOf(Input.class);
        assertThat(parameter.map(PrincipalProfilePayload::getUsername).value()).isEqualTo(entity.getUsername());
    }

    @Test
    void shouldFlatMapPrincipalProfileInputToUsernameInput() {
        long id = 1231L;
        PrincipalProfilePayload entity = createPrincipalProfile(id);

        Input<PrincipalProfilePayload> parameter = Input.of(entity);
        Input<String> usernameInput = parameter.flatMap(value -> Input.of(value.getUsername()));

        assertThat(parameter.value()).isEqualTo(entity);
        assertThat(parameter).isInstanceOf(PayloadParameter.class).isInstanceOf(Input.class);
        assertThat(parameter.map(PrincipalProfilePayload::getUsername).value()).isEqualTo(entity.getUsername());
        assertThat(usernameInput.value()).isEqualTo(entity.getUsername());
        assertThat(usernameInput).isInstanceOf(StringParameter.class).isInstanceOf(Input.class);
    }

    // private methods
    private static StudentPayload createStudent(long id) {
        return StudentPayload.builder()
                .id(id)
                .originalType("not-a-student-" + id)
                .profileId(id + 1)
                .firstName("John-" + id)
                .lastName("Smith-" + id)
                .gender("Male")
                .description("student-description-" + id)
                .courses(List.of())
                .build();
    }

    private static CoursePayload createCourse(long id) {
        return CoursePayload.builder()
                .id(id)
                .name("course-name")
                .description("course-description-" + id)
                .originalType("not-a-course" + id)
                .students(List.of())
                .build();
    }

    private static AuthorityPersonPayload createAuthorityPerson(long id) {
        return AuthorityPersonPayload.builder()
                .id(id)
                .originalType("not-a-person-" + id)
                .profileId(id + 1)
                .firstName("John-" + id)
                .lastName("Dow-" + id)
                .gender("Male")
                .title("principal-title-" + id)
                .faculties(List.of())
                .build();
    }

    private static FacultyPayload createFaculty(long id) {
        return FacultyPayload.builder()
                .id(id)
                .originalType("not-a-faculty-" + id)
                .name("faculty-name")
                .courses(List.of())
                .build();
    }

    private static StudentsGroupPayload createStudentsGroup(long id) {
        return StudentsGroupPayload.builder()
                .id(id)
                .originalType("not-a-students-group-" + id)
                .name("students-group-name")
                .students(List.of())
                .build();
    }

    private static StudentProfilePayload createStudentProfile(long id) {
        return StudentProfilePayload.builder()
                .id(id)
                .originalType("not-a-student-profile-" + id)
                .photoUrl("photo-url-" + id)
                .email("email-" + id)
                .phone("phone-" + id)
                .location("location-" + id)
                .build();
    }

    private static PrincipalProfilePayload createPrincipalProfile(long id) {
        return PrincipalProfilePayload.builder()
                .id(id)
                .originalType("not-a-principal-profile-" + id)
                .photoUrl("photo-url-" + id)
                .email("email-" + id)
                .phone("phone-" + id)
                .location("location-" + id)
                .username("login-" + id)
                .role(Role.SUPPORT_STAFF)
                .signature("signature-" + id)
                .build();
    }
}