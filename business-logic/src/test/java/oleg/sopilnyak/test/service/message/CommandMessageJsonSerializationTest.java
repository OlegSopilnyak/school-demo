package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.io.parameter.*;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.io.result.EmptyResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadSetResult;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommandMessageJsonSerializationTest {
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void shouldDeserializeActionContextUsingActionContextDeserializer() throws IOException {
        String facadeName = "contextNodeTree.get(\"facadeName\")";
        String actionName = "contextNodeTree.get(\"actionName\"))";
        ActionContext context = ActionContext.builder().facadeName(facadeName).actionName(actionName).build();
        CommandMessage.ActionContextDeserializer actionContextDeserializer = new CommandMessage.ActionContextDeserializer();
        String json = objectMapper.writeValueAsString(context);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        ActionContext restored = actionContextDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(ActionContext.class).isEqualTo(context);
        assertThat(restored.getFacadeName()).isEqualTo(facadeName);
        assertThat(restored.getActionName()).isEqualTo(actionName);
    }

    @Test
    void shouldSerializeExceptionUsingExceptionSerializer_SimpleException() throws IOException {
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
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
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
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
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
        String message = "Simple IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(ex, generator, null);
        generator.close();
        String json = writer.toString();
        CommandMessage.ExceptionDeserializer deserializer = new CommandMessage.ExceptionDeserializer();
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Throwable restored = deserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getMessage()).isEqualTo(message);
        assertThat(restored.getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getCause()).isNull();
    }

    @Test
    void shouldDeserializeExceptionUsingExceptionDeserializer_CannotProcessActionException() throws IOException {
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
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
        CommandMessage.ExceptionDeserializer deserializer = new CommandMessage.ExceptionDeserializer();
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
        CommandMessage.InputParameterDeserializer inputParameterDeserializer = new CommandMessage.InputParameterDeserializer();
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
        CommandMessage.InputParameterDeserializer inputParameterDeserializer = new CommandMessage.InputParameterDeserializer();
        StringIdParameter input = new StringIdParameter(id);
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Input<?> restored = inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(StringIdParameter.class);
        assertThat(restored.value()).isEqualTo(id);
    }

    @Test
    void shouldDeserializeInputUsingInputParameterDeserializer_LongIdPairParameter() throws IOException {
        long id = 103L;
        CommandMessage.InputParameterDeserializer inputParameterDeserializer = new CommandMessage.InputParameterDeserializer();
        LongIdPairParameter input = new LongIdPairParameter(id, id + 1);
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        PairParameter<?> restored = (PairParameter<?>) inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(LongIdPairParameter.class);
        assertThat(restored.value().first()).isEqualTo(id);
        assertThat(restored.value().second()).isEqualTo(id + 1);
    }

    @Test
    void shouldDeserializeInputUsingInputParameterDeserializer_PayloadParameter() throws IOException {
        long id = 105L;
        CommandMessage.InputParameterDeserializer inputParameterDeserializer = new CommandMessage.InputParameterDeserializer();
        StudentPayload entity = createStudent(id);
        PayloadParameter<StudentPayload> input = new PayloadParameter<>(entity);
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        PayloadParameter<?> restored = (PayloadParameter<?>) inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(PayloadParameter.class);
        assertThat(restored.value()).isEqualTo(entity);
    }

    @Test
    void shouldDeserializeInputUsingInputParameterDeserializer_PayloadPairParameter() throws IOException {
        long id = 106L;
        CommandMessage.InputParameterDeserializer inputParameterDeserializer = new CommandMessage.InputParameterDeserializer();
        StudentPayload firstEntity = createStudent(id);
        StudentPayload secondEntity = createStudent(id + 1);
        PayloadPairParameter<StudentPayload> input = new PayloadPairParameter<>(firstEntity, secondEntity);
        String json = objectMapper.writeValueAsString(input);
        JsonParser parser = objectMapper.getFactory().createParser(json);

        PayloadPairParameter<StudentPayload> restored = (PayloadPairParameter<StudentPayload>) inputParameterDeserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(PayloadPairParameter.class);
        assertThat(restored.value().first()).isEqualTo(firstEntity);
        assertThat(restored.value().second()).isEqualTo(secondEntity);
    }

    @Test
    void shouldDeserializeOutputUsingOutputResultDeserializer_BooleanResult() throws IOException {
        boolean resultValue = true;
        CommandMessage.OutputResultDeserializer outputResultDeserializer = new CommandMessage.OutputResultDeserializer();
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
        CommandMessage.OutputResultDeserializer outputResultDeserializer = new CommandMessage.OutputResultDeserializer();
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
        CommandMessage.OutputResultDeserializer outputResultDeserializer = new CommandMessage.OutputResultDeserializer();
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
        CommandMessage.OutputResultDeserializer outputResultDeserializer = new CommandMessage.OutputResultDeserializer();
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