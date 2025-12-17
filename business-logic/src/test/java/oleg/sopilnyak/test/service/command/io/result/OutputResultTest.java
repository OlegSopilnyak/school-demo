package oleg.sopilnyak.test.service.command.io.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oleg.sopilnyak.test.service.command.io.Output;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(MockitoExtension.class)
class OutputResultTest {
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void shouldBeReadyEverything() {
        assertThat(objectMapper).isNotNull();
    }

    @Test
    void shouldCreateEmptyResult() {
        Output<Void> result = Output.empty();

        assertThat(result).isNotNull();
        assertThat(result.value()).isNull();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result).isInstanceOf(EmptyResult.class);
    }

    @Test
    void shouldRestoreEmptyResult() throws JsonProcessingException {
        Output<Void> result = Output.empty();
        String json = objectMapper.writeValueAsString(result);
        var restored = objectMapper.readValue(json, EmptyResult.class);

        assertThat(restored).isNotNull();
        assertThat(restored.value()).isNull();
        assertThat(restored.isEmpty()).isTrue();
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateBooleanResult() {
        Output<Boolean> result = Output.of(true);

        assertThat(result).isNotNull();
        assertThat(result.value()).isTrue();
        assertThat(result.isEmpty()).isFalse();
        assertThat(result).isInstanceOf(BooleanResult.class);
    }

    @Test
    void shouldRestoreBooleanResult() throws JsonProcessingException {
        Output<Boolean> result = Output.of(true);
        String json = objectMapper.writeValueAsString(result);
        BooleanResult restored = objectMapper.readValue(json, BooleanResult.class);

        assertThat(restored).isNotNull();
        assertThat(restored.value()).isTrue();
        assertThat(restored.isEmpty()).isFalse();
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateStringResult() {
        String value = "string-value";
        Output<String> result = Output.of(value);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isEqualTo(value);
        assertThat(result).isInstanceOf(StringIdResult.class);
    }

    @Test
    void shouldRestoreStringResult() throws JsonProcessingException {
        String value = "string-value";
        Output<String> result = Output.of(value);
        String json = objectMapper.writeValueAsString(result);
        StringIdResult restored = objectMapper.readValue(json, StringIdResult.class);

        assertThat(restored).isNotNull();
        assertThat(restored.isEmpty()).isFalse();
        assertThat(restored.value()).isEqualTo(value);
        assertThat(restored).isInstanceOf(StringIdResult.class);
    }

    @Test
    void shouldCreateIntegerResult() {
        Output<Number> result = Output.of(1);

        assertThat(result).isInstanceOf(NumberIdResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Integer.class).isEqualTo(1);
    }

    @Test
    void shouldRestoreIntegerResult() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(Output.of(1));
        var result = objectMapper.readValue(json, NumberIdResult.class);

        assertThat(result).isInstanceOf(NumberIdResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Integer.class).isEqualTo(1);
    }

    @Test
    void shouldCreateLongResult() {
        Output<Number> result = Output.of(1L);

        assertThat(result).isInstanceOf(NumberIdResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldRestoreLongResult() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(Output.of(1L));
        var result = objectMapper.readValue(json, NumberIdResult.class);

        assertThat(result).isInstanceOf(NumberIdResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldCreateDoubleResult() {
        Output<Number> result = Output.of(1.0);

        assertThat(result).isInstanceOf(NumberIdResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Double.class).isEqualTo(1.0);
    }

    @Test
    void shouldRestoreDoubleResult() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(Output.of(1.0));
        var result = objectMapper.readValue(json, NumberIdResult.class);

        assertThat(result).isInstanceOf(NumberIdResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Double.class).isEqualTo(1.0);
    }

    @Test
    void shouldCreateOptionalIntegerResult() {
        Output<Optional<Integer>> result = Output.of(Optional.of(1));

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(1);
    }

    @Test
    void shouldRestoreOptionalIntegerResult() throws JsonProcessingException {
        Output<Optional<Integer>> output = Output.of(Optional.of(1));

        String json = objectMapper.writeValueAsString(output);
        Output<Optional<Integer>> result = objectMapper.readValue(json, OptionalValueResult.class);

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(1);
    }

    @Test
    void shouldCreateOptionalLongResult() {
        Output<Optional<Long>> result = Output.of(Optional.of(1L));

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(1L);
    }

    @Test
    void shouldRestoreOptionalLongResult() throws JsonProcessingException {
        Output<Optional<Long>> output = Output.of(Optional.of(1L));

        String json = objectMapper.writeValueAsString(output);
        Output<Optional<Long>> result = objectMapper.readValue(json, OptionalValueResult.class);

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(1L);
    }

    @Test
    void shouldCreateOptionalDoubleResult() {
        Output<Optional<Double>> result = Output.of(Optional.of(1.0));

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(1.0);
    }

    @Test
    void shouldRestoreOptionalDoubleResult() throws JsonProcessingException {
        Output<Optional<Double>> output = Output.of(Optional.of(1.0));

        String json = objectMapper.writeValueAsString(output);
        Output<Optional<Double>> result = objectMapper.readValue(json, OptionalValueResult.class);

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(1.0);
    }

    @Test
    void shouldCreateOptionalEmptyResult() {
        Output<Optional<Object>> result = Output.of(Optional.empty());

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).isEmpty();
    }

    @Test
    void shouldRestoreOptionalEmptyResult() throws JsonProcessingException {
        Output<Optional<Object>> output = Output.of(Optional.empty());

        String json = objectMapper.writeValueAsString(output);
        Output<Optional<Double>> result = objectMapper.readValue(json, OptionalValueResult.class);

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).isEmpty();
    }

    @Test
    void shouldCreateDoubleOptionalLongResult() {
        Output<Optional<Object>> result = Output.of(Optional.of(Optional.of(1L)));

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(Optional.of(1L));
    }

    @Test
    void shouldRestoreDoubleOptionalDoubleResult() throws JsonProcessingException {
        Output<Optional<Object>> output = Output.of(Optional.of(Optional.of(1.0)));

        String json = objectMapper.writeValueAsString(output);
        Output<Optional<Object>> result = objectMapper.readValue(json, OptionalValueResult.class);

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(Optional.of(1.0));
    }

    @Test
    void shouldCreateOptionalStudentResult() {
        long id = 21L;
        StudentPayload entity = createStudent(id);
        CoursePayload extra = createCourse(id + 1);
        entity.setCourses(List.of(extra));

        Output<Optional<StudentPayload>> result = Output.of(Optional.of(entity));

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(entity);
    }

    @Test
    void shouldRestoreOptionalStudentResult() throws JsonProcessingException {
        long id = 23L;
        StudentPayload entity = createStudent(id);
        CoursePayload extra = createCourse(id + 1);
        entity.setCourses(List.of(extra));
        Output<Optional<StudentPayload>> output = Output.of(Optional.of(entity));

        String json = objectMapper.writeValueAsString(output);
        Output<Optional<StudentPayload>> result = objectMapper.readValue(json, OptionalValueResult.class);

        assertThat(result).isInstanceOf(OptionalValueResult.class);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.value()).isInstanceOf(Optional.class).contains(entity);
    }

    @Test
    void shouldCreateStudentPayloadResult() {
        long id = 1L;
        StudentPayload entity = createStudent(id);
        CoursePayload extra = createCourse(id + 1);
        entity.setCourses(List.of(extra));

        Output<StudentPayload> result = Output.of(entity);

        assertThat(result.value()).isSameAs(entity);
        assertThat(result.value().getCourses()).contains(extra);
        assertThat(result).isInstanceOf(PayloadResult.class);
    }

    @Test
    void shouldRestoreStudentPayloadResult() throws JsonProcessingException {
        long id = 101L;
        StudentPayload entity = createStudent(id);
        CoursePayload extra = createCourse(id + 1);
        entity.setCourses(List.of(extra));

        Output<StudentPayload> result = Output.of(entity);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadResult<StudentPayload>> typeReference = new TypeReference<>() {
        };
        PayloadResult<StudentPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getCourses()).contains(extra);
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateCoursePayloadResult() {
        long id = 3L;
        CoursePayload entity = createCourse(id + 1);
        StudentPayload extra = createStudent(id);
        entity.setStudents(List.of(extra));
        Output<CoursePayload> result = Output.of(entity);

        assertThat(result.value()).isSameAs(entity);
        assertThat(result.value().getStudents()).contains(extra);
        assertThat(result).isInstanceOf(PayloadResult.class);
    }

    @Test
    void shouldRestoreCoursePayloadResult() throws JsonProcessingException {
        long id = 103L;
        CoursePayload entity = createCourse(id + 1);
        StudentPayload extra = createStudent(id);
        entity.setStudents(List.of(extra));

        Output<CoursePayload> result = Output.of(entity);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadResult<CoursePayload>> typeReference = new TypeReference<>() {
        };
        PayloadResult<CoursePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getStudents()).contains(extra);
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateAuthorityPersonPayloadResult() {
        long id = 5L;
        AuthorityPersonPayload entity = createAuthorityPerson(id);
        FacultyPayload extra = createFaculty(id + 1);
        entity.setFaculties(List.of(extra));
        PayloadResult<AuthorityPersonPayload> result = new PayloadResult<>(entity);

        assertThat(result.value()).isSameAs(entity);
        assertThat(result.value().getFaculties()).contains(extra);
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreAuthorityPersonPayloadResult() throws JsonProcessingException {
        long id = 105L;
        AuthorityPersonPayload entity = createAuthorityPerson(id);
        FacultyPayload extra = createFaculty(id + 1);
        entity.setFaculties(List.of(extra));

        PayloadResult<AuthorityPersonPayload> result = new PayloadResult<>(entity);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadResult<AuthorityPersonPayload>> typeReference = new TypeReference<>() {
        };
        PayloadResult<AuthorityPersonPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getFaculties()).contains(extra);
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateFacultyPayloadResult() {
        long id = 7L;
        FacultyPayload entity = createFaculty(id);
        CoursePayload extra = createCourse(id + 1);
        AuthorityPersonPayload dean = createAuthorityPerson(id + 2);
        entity.setCourses(List.of(extra));
        entity.setDean(dean);
        PayloadResult<FacultyPayload> result = new PayloadResult<>(entity);

        assertThat(result.value()).isSameAs(entity);
        assertThat(result.value().getDean()).isSameAs(dean);
        assertThat(result.value().getCourses()).contains(extra);
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreFacultyPayloadResult() throws JsonProcessingException {
        long id = 7L;
        FacultyPayload entity = createFaculty(id);
        CoursePayload extra = createCourse(id + 1);
        AuthorityPersonPayload dean = createAuthorityPerson(id + 2);
        entity.setCourses(List.of(extra));
        entity.setDean(dean);

        PayloadResult<FacultyPayload> result = new PayloadResult<>(entity);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadResult<FacultyPayload>> typeReference = new TypeReference<>() {
        };
        PayloadResult<FacultyPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getDean()).isEqualTo(dean);
        assertThat(restored.value().getCourses()).contains(extra);
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateStudentsGroupPayloadResult() {
        long id = 9L;
        StudentsGroupPayload entity = createStudentsGroup(id);
        StudentPayload extra = createStudent(id + 1);
        entity.setStudents(List.of(extra));
        entity.setLeader(extra);

        PayloadResult<StudentsGroupPayload> result = new PayloadResult<>(entity);

        assertThat(result.value()).isSameAs(entity);
        assertThat(result.value().getLeader()).isSameAs(extra);
        assertThat(result.value().getStudents()).contains(extra);
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreStudentsGroupPayloadResult() throws JsonProcessingException {
        long id = 109L;
        StudentsGroupPayload entity = createStudentsGroup(id);
        StudentPayload extra = createStudent(id + 1);
        entity.setStudents(List.of(extra));
        entity.setLeader(extra);

        PayloadResult<StudentsGroupPayload> result = new PayloadResult<>(entity);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadResult<StudentsGroupPayload>> typeReference = new TypeReference<>() {
        };
        PayloadResult<StudentsGroupPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored.value().getLeader()).isEqualTo(extra);
        assertThat(restored.value().getStudents()).contains(extra);
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateStudentProfilePayloadResult() {
        long id = 11L;
        StudentProfilePayload entity = createStudentProfile(id);

        PayloadResult<StudentProfilePayload> result = new PayloadResult<>(entity);

        assertThat(result.value()).isSameAs(entity);
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreStudentProfilePayloadResult() throws JsonProcessingException {
        long id = 111L;
        StudentProfilePayload entity = createStudentProfile(id);

        PayloadResult<StudentProfilePayload> result = new PayloadResult<>(entity);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadResult<StudentProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadResult<StudentProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreatePrincipalProfilePayloadResult() {
        long id = 12L;
        PrincipalProfilePayload entity = createPrincipalProfile(id);

        PayloadResult<PrincipalProfilePayload> result = new PayloadResult<>(entity);

        assertThat(result.value()).isSameAs(entity);
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestorePrincipalProfilePayloadResult() throws JsonProcessingException {
        long id = 112L;
        PrincipalProfilePayload entity = createPrincipalProfile(id);

        PayloadResult<PrincipalProfilePayload> result = new PayloadResult<>(entity);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadResult<PrincipalProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadResult<PrincipalProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).isEqualTo(entity);
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateStudentPayloadSetResult() {
        int size = 1;
        long id = 21L;
        Set<StudentPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            StudentPayload entity = createStudent(id + i);
            CoursePayload extra = createCourse(id + i + 1);
            entity.setCourses(List.of(extra));
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<StudentPayload> result = new PayloadSetResult<>(entitySet);

        assertThat(result.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(result.value()).contains(payload));
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreStudentPayloadSetResult() throws JsonProcessingException {
        int size = 1;
        long id = 121L;
        Set<StudentPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            StudentPayload entity = createStudent(id + i);
            CoursePayload extra = createCourse(id + i + 1);
            entity.setCourses(List.of(extra));
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<StudentPayload> result = new PayloadSetResult<>(entitySet);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadSetResult<StudentPayload>> typeReference = new TypeReference<>() {
        };
        PayloadSetResult<StudentPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(restored.value()).contains(payload));
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldNotRestoreStudentPayloadSetResult() {
        int size = 1;
        long id = 121L;
        Set<BasePayload<?>> entitySet = IntStream.range(0, size).mapToObj(i -> {
            StudentPayload entity = createStudent(id + i);
            CoursePayload extra = createCourse(id + i + 1);
            entity.setCourses(List.of(extra));
            return entity;
        }).collect(Collectors.toSet());
        entitySet.add(createCourse(id + 100));
        PayloadSetResult<? extends BasePayload<?>> result = new PayloadSetResult<>(entitySet);

        var e = assertThrows(IOException.class, () -> objectMapper.writeValueAsString(result));

        assertThat(e.getMessage()).contains("Payload Set parameter elements types mismatch");
    }

    @Test
    void shouldCreateCoursePayloadSetResult() {
        int size = 3;
        long id = 23L;
        Set<CoursePayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            CoursePayload entity = createCourse(id + i + 1);
            StudentPayload extra = createStudent(id + i);
            entity.setStudents(List.of(extra));
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<CoursePayload> result = new PayloadSetResult<>(entitySet);

        assertThat(result.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(result.value()).contains(payload));
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreCoursePayloadSetResult() throws JsonProcessingException {
        int size = 3;
        long id = 123L;
        Set<CoursePayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            CoursePayload entity = createCourse(id + i + 1);
            StudentPayload extra = createStudent(id + i);
            entity.setStudents(List.of(extra));
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<CoursePayload> result = new PayloadSetResult<>(entitySet);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadSetResult<CoursePayload>> typeReference = new TypeReference<>() {
        };
        PayloadSetResult<CoursePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(restored.value()).contains(payload));
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateAuthorityPersonPayloadSetResult() {
        int size = 5;
        long id = 25L;
        Set<AuthorityPersonPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            AuthorityPersonPayload entity = createAuthorityPerson(id + i);
            FacultyPayload extra = createFaculty(id + i + 1);
            entity.setFaculties(List.of(extra));
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<AuthorityPersonPayload> result = new PayloadSetResult<>(entitySet);

        assertThat(result.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(result.value()).contains(payload));
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreAuthorityPersonPayloadSetResult() throws JsonProcessingException {
        int size = 5;
        long id = 125L;
        Set<AuthorityPersonPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            AuthorityPersonPayload entity = createAuthorityPerson(id + i);
            FacultyPayload extra = createFaculty(id + i + 1);
            entity.setFaculties(List.of(extra));
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<AuthorityPersonPayload> result = new PayloadSetResult<>(entitySet);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadSetResult<AuthorityPersonPayload>> typeReference = new TypeReference<>() {
        };
        PayloadSetResult<AuthorityPersonPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(restored.value()).contains(payload));
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateFacultyPayloadSetResult() {
        int size = 7;
        long id = 27L;
        Set<FacultyPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            FacultyPayload entity = createFaculty(id + i);
            CoursePayload extra = createCourse(id + i + 1);
            AuthorityPersonPayload dean = createAuthorityPerson(id + 2);
            entity.setCourses(List.of(extra));
            entity.setDean(dean);
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<FacultyPayload> result = new PayloadSetResult<>(entitySet);

        assertThat(result.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(result.value()).contains(payload));
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreFacultyPayloadSetResult() throws JsonProcessingException {
        int size = 7;
        long id = 127L;
        Set<FacultyPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            FacultyPayload entity = createFaculty(id + i);
            CoursePayload extra = createCourse(id + i + 1);
            AuthorityPersonPayload dean = createAuthorityPerson(id + 2);
            entity.setCourses(List.of(extra));
            entity.setDean(dean);
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<FacultyPayload> result = new PayloadSetResult<>(entitySet);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadSetResult<FacultyPayload>> typeReference = new TypeReference<>() {
        };
        PayloadSetResult<FacultyPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(restored.value()).contains(payload));
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateStudentsGroupPayloadSetResult() {
        int size = 9;
        long id = 29L;
        Set<StudentsGroupPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            StudentsGroupPayload entity = createStudentsGroup(id + i);
            StudentPayload extra = createStudent(id + i + 1);
            entity.setStudents(List.of(extra));
            entity.setLeader(extra);
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<StudentsGroupPayload> result = new PayloadSetResult<>(entitySet);

        assertThat(result.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(result.value()).contains(payload));
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreStudentsGroupPayloadSetResult() throws JsonProcessingException {
        int size = 9;
        long id = 129L;
        Set<StudentsGroupPayload> entitySet = IntStream.range(0, size).mapToObj(i -> {
            StudentsGroupPayload entity = createStudentsGroup(id + i);
            StudentPayload extra = createStudent(id + i + 1);
            entity.setStudents(List.of(extra));
            entity.setLeader(extra);
            return entity;
        }).collect(Collectors.toSet());

        PayloadSetResult<StudentsGroupPayload> result = new PayloadSetResult<>(entitySet);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadSetResult<StudentsGroupPayload>> typeReference = new TypeReference<>() {
        };
        PayloadSetResult<StudentsGroupPayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(restored.value()).contains(payload));
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreateStudentProfilePayloadSetResult() {
        int size = 11;
        long id = 31L;
        Set<StudentProfilePayload> entitySet = IntStream.range(0, size)
                .mapToObj(i -> createStudentProfile(id + i))
                .collect(Collectors.toSet());

        PayloadSetResult<StudentProfilePayload> result = new PayloadSetResult<>(entitySet);

        assertThat(result.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(result.value()).contains(payload));
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestoreStudentProfilePayloadSetResult() throws JsonProcessingException {
        int size = 11;
        long id = 131L;
        Set<StudentProfilePayload> entitySet = IntStream.range(0, size)
                .mapToObj(i -> createStudentProfile(id + i))
                .collect(Collectors.toSet());

        PayloadSetResult<StudentProfilePayload> result = new PayloadSetResult<>(entitySet);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadSetResult<StudentProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadSetResult<StudentProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(restored.value()).contains(payload));
        assertThat(restored).isInstanceOf(Output.class);
    }

    @Test
    void shouldCreatePrincipalProfilePayloadSetResult() {
        int size = 12;
        long id = 32L;
        Set<PrincipalProfilePayload> entitySet = IntStream.range(0, size)
                .mapToObj(i -> createPrincipalProfile(id + i))
                .collect(Collectors.toSet());

        PayloadSetResult<PrincipalProfilePayload> result = new PayloadSetResult<>(entitySet);

        assertThat(result.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(result.value()).contains(payload));
        assertThat(result).isInstanceOf(Output.class);
    }

    @Test
    void shouldRestorePrincipalProfilePayloadSetResult() throws JsonProcessingException {
        int size = 12;
        long id = 132L;
        Set<PrincipalProfilePayload> entitySet = IntStream.range(0, size)
                .mapToObj(i -> createPrincipalProfile(id + i))
                .collect(Collectors.toSet());

        PayloadSetResult<PrincipalProfilePayload> result = new PayloadSetResult<>(entitySet);
        String json = objectMapper.writeValueAsString(result);

        TypeReference<PayloadSetResult<PrincipalProfilePayload>> typeReference = new TypeReference<>() {
        };
        PayloadSetResult<PrincipalProfilePayload> restored = objectMapper.readValue(json, typeReference);

        assertThat(restored.value()).hasSize(size);
        entitySet.forEach(payload -> assertThat(restored.value()).contains(payload));
        assertThat(restored).isInstanceOf(Output.class);
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
                .login("login-" + id)
                .signature("signature-" + id)
                .build();
    }
}
