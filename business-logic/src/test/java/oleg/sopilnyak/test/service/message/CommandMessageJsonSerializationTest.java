package oleg.sopilnyak.test.service.message;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.service.command.io.CompositeOutput;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.io.parameter.CompositeParameter;
import oleg.sopilnyak.test.service.command.io.parameter.NumberIdParameter;
import oleg.sopilnyak.test.service.command.io.parameter.PayloadParameter;
import oleg.sopilnyak.test.service.command.io.parameter.StaffRoleParameter;
import oleg.sopilnyak.test.service.command.io.parameter.StringParameter;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.io.result.CompositeResult;
import oleg.sopilnyak.test.service.command.io.result.EmptyResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadSetResult;
import oleg.sopilnyak.test.service.command.io.result.ResultsContainer;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class CommandMessageJsonSerializationTest {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void shouldDeserializeActionContextUsingActionContextDeserializer() throws IOException {
        String facadeName = "contextNodeTree.get(\"facadeName\")";
        String actionName = "contextNodeTree.get(\"actionName\"))";
        ActionContext context = ActionContext.builder().actionProcessorFacade(facadeName).entryPointMethod(actionName).build();
        context.finish();
        String json = objectMapper.writeValueAsString(context);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        ActionContext restored = new IOBase.ActionContextDeserializer().deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(ActionContext.class).isEqualTo(context);
        assertThat(restored.getActionProcessorFacade()).isEqualTo(facadeName);
        assertThat(restored.getEntryPointMethod()).isEqualTo(actionName);
    }

    @Test
    void shouldSerializeExceptionUsingExceptionSerializer_SimpleException() throws IOException {
        IOBase.ExceptionSerializer<Exception> exceptionSerializer = new IOBase.ExceptionSerializer<>();
        String message = "Simple exception message";
        Exception ex = new Exception(message);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();

        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(ex, generator, null);
        generator.close();

        String json = writer.toString();

        Map<String, Object> restored = objectMapper.readValue(json, Map.class);
        assertThat(restored).isNotNull().containsEntry("type", Exception.class.getName());
        Map<String, Object> value = (Map<String, Object>) restored.get("value");
        assertThat(value).isNotNull().containsEntry("message", message);
        List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) value.get("stackTrace");
        assertThat(stackTrace).isNotNull().hasSize(stackTraceDepth);

        assertThat(value.get("cause")).isNull();
    }

    @Test
    void shouldSerializeExceptionUsingExceptionSerializer_CannotProcessActionException() throws IOException {
        IOBase.ExceptionSerializer<Exception> exceptionSerializer = new IOBase.ExceptionSerializer<>();
        String message = "Embedded IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        ActionContext.setup("test-facade", "test-action");
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String exceptionMessage = exception.getMessage();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();

        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(exception, generator, null);
        generator.close();

        String json = writer.toString();

        Map<String, Object> restored = objectMapper.readValue(json, Map.class);
        assertThat(restored).isNotNull().containsEntry("type", CannotProcessActionException.class.getName());
        Map<String, Object> value = (Map<String, Object>) restored.get("value");
        assertThat(value).isNotNull().containsEntry("message", exceptionMessage);
        List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) value.get("stackTrace");
        assertThat(stackTrace).isNotNull().hasSize(stackTraceDepth);

        Map<String, Object> restoredCause = (Map<String, Object>) value.get("cause");
        assertThat(restoredCause).isNotNull().containsEntry("type", IOException.class.getName());
        Map<String, Object> causeValue = (Map<String, Object>) restoredCause.get("value");
        assertThat(causeValue).isNotNull().containsEntry("message", message);
        List<Map<String, Object>> causeStackTrace = (List<Map<String, Object>>) causeValue.get("stackTrace");
        assertThat(causeStackTrace).isNotNull().hasSize(stackTraceDepth);
        assertThat(causeValue.get("cause")).isNull();
    }

    @Test
    void shouldDeserializeExceptionUsingExceptionDeserializer_SimpleException() throws IOException {
        IOBase.ExceptionSerializer<Exception> exceptionSerializer = new IOBase.ExceptionSerializer<>();
        String message = "Simple IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(ex, generator, null);
        generator.close();
        String json = writer.toString();
        IOBase.ExceptionDeserializer deserializer = new IOBase.ExceptionDeserializer();
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Throwable restored = deserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getMessage()).isEqualTo(message);
        assertThat(restored.getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getCause()).isNull();
    }

    @Test
    void shouldDeserializeExceptionUsingExceptionDeserializer_CannotProcessActionException() throws IOException {
        IOBase.ExceptionSerializer<Exception> exceptionSerializer = new IOBase.ExceptionSerializer<>();
        String message = "Embedded IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();
        ActionContext.setup("test-facade", "test-action");
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String exceptionMessage = exception.getMessage();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(exception, generator, null);
        generator.close();
        String json = writer.toString();
        IOBase.ExceptionDeserializer deserializer = new IOBase.ExceptionDeserializer();
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Throwable restored = deserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(CannotProcessActionException.class);
        assertThat(restored.getMessage()).isEqualTo(exceptionMessage);
        assertThat(restored.getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getCause()).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getCause().getMessage()).isEqualTo(message);
    }

    @Test
    void shouldDeserializeInputUsingInputParameterDeserializer_LongIdParameter() throws IOException {
        long id = 102;
        Input.ParameterDeserializer<Long> inputParameterDeserializer = new Input.ParameterDeserializer<>();
        NumberIdParameter<Long> input = new NumberIdParameter<>(id);
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Input<?> restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(NumberIdParameter.class);
        assertThat(restored.value()).isEqualTo(id);
    }

    @Test
    void shouldDeserializeInputUsingInputParameterDeserializer_StringIdParameter() throws IOException {
        String id = UUID.randomUUID().toString();
        Input.ParameterDeserializer<String> inputParameterDeserializer = new Input.ParameterDeserializer<>();
        StringParameter input = new StringParameter(id);
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Input<?> restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(StringParameter.class);
        assertThat(restored.value()).isEqualTo(id);
    }

    @Test
    void shouldDeserializeInputUsingInputParameterDeserializer_PayloadParameter() throws IOException {
        long id = 105L;
        Input.ParameterDeserializer inputParameterDeserializer = new Input.ParameterDeserializer();
        StudentPayload entity = createStudent(id);
        PayloadParameter<StudentPayload> input = new PayloadParameter<>(entity);
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        PayloadParameter<?> restored = (PayloadParameter<?>) inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(PayloadParameter.class);
        assertThat(restored.value()).isEqualTo(entity);
    }

    @Test
    void shouldDeserializeCompositeInputParameter_TheSameTypes_Long() throws IOException {
        Long id1 = 102L;
        Long id2 = 103L;
        Long id3 = 104L;
        Input.ParameterDeserializer<Input<?>[]> inputParameterDeserializer = new Input.ParameterDeserializer<>();
        CompositeParameter<Long> input = new CompositeParameter<>(Input.of(id1), Input.of(id2), Input.of(id3));
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        var restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isInstanceOf(CompositeParameter.class).isInstanceOf(Input.class);
        assertThat(restored.value()[0].value()).isEqualTo(id1);
        assertThat(restored.value()[1].value()).isEqualTo(id2);
        assertThat(restored.value()[2].value()).isEqualTo(id3);
    }

    @Test
    void shouldDeserializeCompositeInputParameter_TheSameTypes_String() throws IOException {
        String id1 = "102L";
        String id2 = "103L";
        String id3 = "104L";
        Input.ParameterDeserializer<Input<?>[]> inputParameterDeserializer = new Input.ParameterDeserializer<>();
        CompositeParameter<String> input = new CompositeParameter<>(Input.of(id1), Input.of(id2), Input.of(id3));
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        var restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isInstanceOf(CompositeParameter.class).isInstanceOf(Input.class);
        assertThat(restored.value()[0].value()).isEqualTo(id1);
        assertThat(restored.value()[1].value()).isEqualTo(id2);
        assertThat(restored.value()[2].value()).isEqualTo(id3);
    }

    @Test
    void shouldDeserializeCompositeInputParameter_TheSameTypes_Payload() throws IOException {
        long id = 116L;
        StudentPayload id1 = createStudent(id);
        StudentPayload id2 = createStudent(id + 1);
        StudentPayload id3 = createStudent(id + 2);
        Input.ParameterDeserializer<Input<?>[]> inputParameterDeserializer = new Input.ParameterDeserializer<>();
        CompositeParameter<Student> input = new CompositeParameter<>(Input.of(id1), Input.of(id2), Input.of(id3));
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        var restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isInstanceOf(CompositeParameter.class).isInstanceOf(Input.class);
        assertThat(restored.value()[0].value()).isEqualTo(id1);
        assertThat(restored.value()[1].value()).isEqualTo(id2);
        assertThat(restored.value()[2].value()).isEqualTo(id3);
    }

    @Test
    void shouldDeserializeCompositeInputParameter_DifferentTypes() throws IOException {
        long id = 126L;
        StudentPayload id1 = createStudent(id);
        Long id2 = id + 1;
        String id3 = "id + 2";
        Input.ParameterDeserializer<Input<?>[]> inputParameterDeserializer = new Input.ParameterDeserializer<>();
        CompositeParameter<Object> input = new CompositeParameter<>(Input.of(id1), Input.of(id2), Input.of(id3));
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        var restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isInstanceOf(CompositeParameter.class).isInstanceOf(Input.class);
        assertThat(restored.value()[0].value()).isEqualTo(id1);
        assertThat(restored.value()[1].value()).isEqualTo(id2);
        assertThat(restored.value()[2].value()).isEqualTo(id3);
    }

    @Test
    void shouldDeserializeCompositeInputParameter_UsingInputOf() throws IOException {
        long id = 126L;
        StudentPayload id1 = createStudent(id);
        Long id2 = id + 1;
        String id3 = "id + 2";
        Input.ParameterDeserializer<Input<?>[]> inputParameterDeserializer = new Input.ParameterDeserializer<>();
        var input = Input.of(Input.of(id1), Input.of(id2), Input.of(id3));
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        var restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isInstanceOf(CompositeParameter.class).isInstanceOf(Input.class);
        assertThat(restored.value()[0].value()).isEqualTo(id1);
        assertThat(restored.value()[1].value()).isEqualTo(id2);
        assertThat(restored.value()[2].value()).isEqualTo(id3);
    }

    @Test
    void shouldDeserializeCompositeOutputResult_Boolean() throws IOException {
        boolean boolTrueValue = true;
        boolean boolFalseValue = false;

        Output.ResultDeserializer<Boolean> outputResultDeserializer = new Output.ResultDeserializer<>();
        CompositeOutput<Boolean> result = Output.of(Output.of(boolTrueValue), Output.of(boolFalseValue));
        String json = objectMapper.writeValueAsString(result);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        CompositeOutput<Boolean> restored = (CompositeOutput) outputResultDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(CompositeResult.class).isInstanceOf(ResultsContainer.class)
                .isInstanceOf(CompositeOutput.class).isInstanceOf(Output.class);
        assertThat(restored.isEmpty()).isFalse();
        assertThat(restored.value()[0].value()).isEqualTo(boolTrueValue);
        assertThat(restored.value()[1].value()).isEqualTo(boolFalseValue);
    }

    @Test
    void shouldDeserializeCompositeOutputResult_StudentPayload() throws IOException {
        long id = 211L;
        StudentPayload firstStudentValue = createStudent(id);
        StudentPayload secondStudentValue = createStudent(id + 10);

        Output.ResultDeserializer<Boolean> outputResultDeserializer = new Output.ResultDeserializer<>();
        CompositeOutput<Student> result = Output.of(Output.of(firstStudentValue), Output.of(secondStudentValue));
        String json = objectMapper.writeValueAsString(result);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        CompositeOutput<Student> restored = (CompositeOutput) outputResultDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(CompositeResult.class)
                .isInstanceOf(CompositeOutput.class)
                .isInstanceOf(Output.class);
        assertThat(restored.isEmpty()).isFalse();
        assertThat(restored.value()[0].value()).isEqualTo(firstStudentValue);
        assertThat(restored.value()[1].value()).isEqualTo(secondStudentValue);
    }

    @Test
    void shouldDeserializeCompositeOutputResult_TypesMix() throws IOException {
        long id = 221L;
        StudentPayload studentValue = createStudent(id);
        boolean boolTrueValue = true;

        Output.ResultDeserializer<Boolean> outputResultDeserializer = new Output.ResultDeserializer<>();
        CompositeOutput<Student> result = Output.of(Output.of(studentValue), Output.of(boolTrueValue));
        String json = objectMapper.writeValueAsString(result);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        CompositeOutput<Object> restored = (CompositeOutput) outputResultDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(CompositeResult.class)
                .isInstanceOf(CompositeOutput.class)
                .isInstanceOf(Output.class);
        assertThat(restored.isEmpty()).isFalse();
        assertThat(restored.value()[0].value()).isEqualTo(studentValue);
        assertThat(restored.value()[1].value()).isEqualTo(boolTrueValue);
    }

    @Test
    void shouldDeserializeOutputUsingOutputResultDeserializer_BooleanResult() throws IOException {
        boolean resultValue = true;
        Output.ResultDeserializer outputResultDeserializer = new Output.ResultDeserializer();
        BooleanResult result = new BooleanResult(resultValue);
        String json = objectMapper.writeValueAsString(result);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Output restored = outputResultDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(BooleanResult.class);
        assertThat(restored.value()).isEqualTo(resultValue);
        assertThat(restored.isEmpty()).isFalse();
    }

    @Test
    void shouldDeserializeOutputUsingOutputResultDeserializer_EmptyResult() throws IOException {
        Output.ResultDeserializer outputResultDeserializer = new Output.ResultDeserializer();
        EmptyResult result = new EmptyResult();
        String json = objectMapper.writeValueAsString(result);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Output restored = outputResultDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(EmptyResult.class);
        assertThat(restored.value()).isNull();
        assertThat(restored.isEmpty()).isTrue();
    }

    @Test
    void shouldDeserializeOutputUsingOutputResultDeserializer_StudentPayloadResult() throws IOException {
        long id = 201L;
        Output.ResultDeserializer outputResultDeserializer = new Output.ResultDeserializer();
        StudentPayload entity = createStudent(id);
        PayloadResult<StudentPayload> result = new PayloadResult<>(entity);
        String json = objectMapper.writeValueAsString(result);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Output restored = outputResultDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(PayloadResult.class);
        assertThat(restored.value()).isNotNull().isInstanceOf(StudentPayload.class).isEqualTo(entity);
        assertThat(restored.isEmpty()).isFalse();
    }

    @Test
    void shouldDeserializeOutputUsingOutputResultDeserializer_StudentPayloadSetResult() throws IOException {
        long id = 202L;
        Output.ResultDeserializer outputResultDeserializer = new Output.ResultDeserializer();
        StudentPayload entity1 = createStudent(id);
        StudentPayload entity2 = createStudent(id + 1);
        StudentPayload entity3 = createStudent(id + 2);
        PayloadSetResult<StudentPayload> result = new PayloadSetResult<>(Set.of(entity1, entity2, entity3));
        String json = objectMapper.writeValueAsString(result);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        PayloadSetResult<StudentPayload> restored = (PayloadSetResult<StudentPayload>) outputResultDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(PayloadSetResult.class);
        assertThat(restored.value()).isNotNull().isInstanceOf(Set.class);
        assertThat(restored.value()).contains(entity1, entity2, entity3);
        assertThat(restored.isEmpty()).isFalse();
    }

    @Test
    void shouldDeserializeInputUsingInputParameterDeserializer_StaffRole() throws IOException {
        Role value = Role.TEACHER;
        Input.ParameterDeserializer<Role> inputParameterDeserializer = new Input.ParameterDeserializer<>();

        var parameter = Input.of(value);
        String json = objectMapper.writeValueAsString(parameter);
        assertThat(json).contains(StaffRoleParameter.class.getName());
        JsonParser parser = objectMapper.getFactory().createParser(json);

        var restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored.value()).isEqualTo(value);
        assertThat(restored).isInstanceOf(StaffRoleParameter.class).isInstanceOf(Input.class);
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
}