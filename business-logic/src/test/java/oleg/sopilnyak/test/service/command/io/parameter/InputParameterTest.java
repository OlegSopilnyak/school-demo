package oleg.sopilnyak.test.service.command.io.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.BasePayload;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class InputParameterTest {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void shouldCreateEmptyParameter() {

        Input<Void> parameter = Input.empty();

        assertThat(parameter.isEmpty()).isTrue();
        assertThat(parameter.value()).isNull();
        assertThat(parameter).isInstanceOf(EmptyParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreEmptyParameter() throws JsonProcessingException {

        Input<Void> parameter = Input.empty();

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
        assertThat(parameter).isInstanceOf(StringIdParameter.class);
    }

    @Test
    void shouldRestoreStringIdParameter() throws JsonProcessingException {
        long id = 4L;
        String stringId = ":" + id;

        Input<String> parameter = Input.of(stringId);

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StringIdParameter.class.getName());
        StringIdParameter restored = objectMapper.readValue(json, StringIdParameter.class);

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
    void shouldCreateLongIdPairParameter() {
        long firstId = 5L;
        long secondId = 6L;

        PairParameter<Long> parameter = Input.of(firstId, secondId);

        assertThat(parameter.value().first()).isSameAs(firstId);
        assertThat(parameter.value().second()).isSameAs(secondId);
        assertThat(parameter).isInstanceOf(LongIdPairParameter.class);
    }

    @Test
    void shouldRestoreLongIdPairParameter() throws JsonProcessingException {
        long firstId = 7L;
        long secondId = 8L;

        PairParameter<Long> parameter = Input.of(firstId, secondId);

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(LongIdPairParameter.class.getName());
        LongIdPairParameter restored = objectMapper.readValue(json, LongIdPairParameter.class);

        assertThat(restored.value().first()).isSameAs(firstId);
        assertThat(restored.value().second()).isSameAs(secondId);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStringsPairParameter() {
        String first = "first";
        String second = "second";

        PairParameter<String> parameter = Input.of(first, second);

        assertThat(parameter.value().first()).isSameAs(first);
        assertThat(parameter.value().second()).isSameAs(second);
        assertThat(parameter).isInstanceOf(StringPairParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldRestoreStringsPairParameter() throws JsonProcessingException {
        String first = "first";
        String second = "second";

        PairParameter<String> parameter = Input.of(first, second);

        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StringPairParameter.class.getName());
        StringPairParameter restored = objectMapper.readValue(json, StringPairParameter.class);

        assertThat(restored.value().first()).isEqualTo(first);
        assertThat(restored.value().second()).isEqualTo(second);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
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

        PairParameter<StudentPayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value().first()).isSameAs(firstEntity);
        assertThat(parameter.value().first().getCourses()).contains(firstCourse);
        assertThat(parameter.value().second()).isSameAs(secondEntity);
        assertThat(parameter.value().second().getCourses()).contains(secondCourse);
        assertThat(parameter).isInstanceOf(PayloadPairParameter.class);
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

        PairParameter<StudentPayload> parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StudentPayload.class.getName());

        TypeReference<PayloadPairParameter<StudentPayload>> typeReference = new TypeReference<>() {
        };
        PayloadPairParameter<StudentPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value().first()).isEqualTo(firstEntity);
        assertThat(restored.value().first().getCourses()).contains(firstCourse);
        assertThat(restored.value().second()).isEqualTo(secondEntity);
        assertThat(restored.value().second().getCourses()).contains(secondCourse);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldNotRestoreStudentPairParameter_PairParameterClassesMismatch() {
        long firstId = 112L;
        long secondId = 122L;
        StudentPayload firstEntity = createStudent(firstId);
        CoursePayload secondCourse = createCourse(secondId + 10);

        PayloadPairParameter<? extends BasePayload<? extends BaseType>> parameter = new PayloadPairParameter<>(firstEntity, secondCourse);
        IOException exception = assertThrows(IOException.class, () -> objectMapper.writeValueAsString(parameter));

        assertThat(exception.getMessage()).contains("Pair parameter class mismatch");
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

        PairParameter<CoursePayload> parameter = Input.of(firstCourse, secondCourse);

        assertThat(parameter.value().first()).isSameAs(firstCourse);
        assertThat(parameter.value().first().getStudents()).contains(firstEntity);
        assertThat(parameter.value().second()).isSameAs(secondCourse);
        assertThat(parameter.value().second().getStudents()).contains(secondEntity);
        assertThat(parameter).isInstanceOf(PayloadPairParameter.class);
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

        PairParameter<CoursePayload> parameter = Input.of(firstCourse, secondCourse);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(CoursePayload.class.getName());

        TypeReference<PayloadPairParameter<CoursePayload>> typeReference = new TypeReference<>() {
        };
        PayloadPairParameter<CoursePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value().first()).isEqualTo(firstCourse);
        assertThat(restored.value().first().getStudents()).contains(firstEntity);
        assertThat(restored.value().second()).isEqualTo(secondCourse);
        assertThat(restored.value().second().getStudents()).contains(secondEntity);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
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

        PairParameter<AuthorityPersonPayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value().first()).isSameAs(firstEntity);
        assertThat(parameter.value().first().getFaculties()).contains(firstFaculty);
        assertThat(parameter.value().second()).isSameAs(secondEntity);
        assertThat(parameter.value().second().getFaculties()).contains(secondFaculty);
        assertThat(parameter).isInstanceOf(PayloadPairParameter.class);
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

        PairParameter<AuthorityPersonPayload> parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(AuthorityPersonPayload.class.getName());

        TypeReference<PayloadPairParameter<AuthorityPersonPayload>> typeReference = new TypeReference<>() {
        };
        PayloadPairParameter<AuthorityPersonPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value().first()).isEqualTo(firstEntity);
        assertThat(restored.value().first().getFaculties()).contains(firstFaculty);
        assertThat(restored.value().second()).isEqualTo(secondEntity);
        assertThat(restored.value().second().getFaculties()).contains(secondFaculty);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
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

        PairParameter<FacultyPayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value().first()).isSameAs(firstEntity);
        assertThat(parameter.value().first().getDean()).isSameAs(firstDean);
        assertThat(parameter.value().first().getCourses()).contains(firstCourse);
        assertThat(parameter.value().second()).isSameAs(secondEntity);
        assertThat(parameter.value().second().getDean()).isSameAs(secondDean);
        assertThat(parameter.value().second().getCourses()).contains(secondCourse);
        assertThat(parameter).isInstanceOf(PayloadPairParameter.class);
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

        PairParameter<FacultyPayload> parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(FacultyPayload.class.getName());

        TypeReference<PayloadPairParameter<FacultyPayload>> typeReference = new TypeReference<>() {
        };
        PayloadPairParameter<FacultyPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value().first()).isEqualTo(firstEntity);
        assertThat(restored.value().first().getDean()).isEqualTo(firstDean);
        assertThat(restored.value().first().getCourses()).contains(firstCourse);
        assertThat(restored.value().second()).isEqualTo(secondEntity);
        assertThat(restored.value().second().getDean()).isEqualTo(secondDean);
        assertThat(restored.value().second().getCourses()).contains(secondCourse);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
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

        PairParameter<StudentsGroupPayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value().first()).isSameAs(firstEntity);
        assertThat(parameter.value().first().getLeader()).isSameAs(firstStudent);
        assertThat(parameter.value().first().getStudents()).contains(firstStudent);
        assertThat(parameter.value().second()).isSameAs(secondEntity);
        assertThat(parameter.value().second().getLeader()).isSameAs(secondStudent);
        assertThat(parameter.value().second().getStudents()).contains(secondStudent);
        assertThat(parameter).isInstanceOf(PayloadPairParameter.class);
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

        PairParameter<StudentsGroupPayload> parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StudentsGroupPayload.class.getName());

        TypeReference<PayloadPairParameter<StudentsGroupPayload>> typeReference = new TypeReference<>() {
        };
        PayloadPairParameter<StudentsGroupPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value().first()).isEqualTo(firstEntity);
        assertThat(restored.value().first().getLeader()).isEqualTo(firstStudent);
        assertThat(restored.value().first().getStudents()).contains(firstStudent);
        assertThat(restored.value().second()).isEqualTo(secondEntity);
        assertThat(restored.value().second().getLeader()).isEqualTo(secondStudent);
        assertThat(restored.value().second().getStudents()).contains(secondStudent);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreateStudentProfilePairParameter() {
        long firstId = 21L;
        long secondId = 22L;
        StudentProfilePayload firstEntity = createStudentProfile(firstId);
        StudentProfilePayload secondEntity = createStudentProfile(secondId);

        PairParameter<StudentProfilePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value().first()).isSameAs(firstEntity);
        assertThat(parameter.value().second()).isSameAs(secondEntity);
        assertThat(parameter).isInstanceOf(PayloadPairParameter.class);
    }

    @Test
    void shouldRestoreStudentProfilePairParameter() throws JsonProcessingException {
        long firstId = 211L;
        long secondId = 221L;
        StudentProfilePayload firstEntity = createStudentProfile(firstId);
        StudentProfilePayload secondEntity = createStudentProfile(secondId);

        PairParameter<StudentProfilePayload> parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StudentProfilePayload.class.getName());

        TypeReference<PayloadPairParameter<StudentProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadPairParameter<StudentProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value().first()).isEqualTo(firstEntity);
        assertThat(restored.value().second()).isEqualTo(secondEntity);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
    }

    @Test
    void shouldCreatePrincipalProfilePairParameter() {
        long firstId = 23L;
        long secondId = 24L;
        PrincipalProfilePayload firstEntity = createPrincipalProfile(firstId);
        PrincipalProfilePayload secondEntity = createPrincipalProfile(secondId);

        PairParameter<PrincipalProfilePayload> parameter = Input.of(firstEntity, secondEntity);

        assertThat(parameter.value().first()).isSameAs(firstEntity);
        assertThat(parameter.value().second()).isSameAs(secondEntity);
        assertThat(parameter).isInstanceOf(PayloadPairParameter.class);
    }

    @Test
    void shouldRestorePrincipalProfilePairParameter() throws JsonProcessingException {
        long firstId = 231L;
        long secondId = 241L;
        PrincipalProfilePayload firstEntity = createPrincipalProfile(firstId);
        PrincipalProfilePayload secondEntity = createPrincipalProfile(secondId);

        PairParameter<PrincipalProfilePayload> parameter = Input.of(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(PrincipalProfilePayload.class.getName());

        TypeReference<PayloadPairParameter<PrincipalProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadPairParameter<PrincipalProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value().first()).isEqualTo(firstEntity);
        assertThat(restored.value().second()).isEqualTo(secondEntity);
        assertThat(restored).isInstanceOf(PairParameter.class).isInstanceOf(Input.class);
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