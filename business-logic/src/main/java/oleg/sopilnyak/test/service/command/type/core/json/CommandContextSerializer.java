package oleg.sopilnyak.test.service.command.type.core.json;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Json Serializer: serializer for Context
 *
 * @param <T> type result for the context
 * @see Context
 */
public class CommandContextSerializer<T> extends StdSerializer<Context<T>> implements JsonContextFields {
    private final IOBase.ExceptionSerializer<Throwable> exceptionSerializer = new IOBase.ExceptionSerializer<>();

    public CommandContextSerializer() {
        this(null);
    }

    protected CommandContextSerializer(Class<Context<T>> t) {
        super(t);
    }

    @Override
    public void serialize(final Context<T> context, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
        final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
        generator.writeStartObject();
        serializeCommand(context.getCommand(), generator);
        serializeRedoParameter(context.getRedoParameter(), generator);
        serializeUndoParameter(context.getUndoParameter(), generator);
        serializeExecutionResult(context.getResult().orElse(null), generator);
        serializeExecutionError(context.getException(), generator);
        generator.writeStringField(STARTED_AT_FIELD_NAME, mapper.writeValueAsString(context.getStartedAt()));
        generator.writeStringField(DURATION_FIELD_NAME, mapper.writeValueAsString(context.getDuration()));
        generator.writeStringField(STATE_FIELD_NAME, String.valueOf(context.getState()));
        serializeHistory(context.getHistory(), generator);
        generator.writeEndObject();
    }

    private void serializeCommand(final RootCommand<T> command, final JsonGenerator generator) throws IOException {
        generator.writeFieldName(COMMAND_FIELD_NAME);
        generator.writeStartObject();
        generator.writeStringField(COMMAND_ID_FIELD_NAME, command.getId());
        generator.writeStringField(COMMAND_FAMILY_FIELD_NAME, command.commandFamily().getName());
        generator.writeEndObject();
    }

    private void serializeExecutionResult(final T executionResult, final JsonGenerator generator) throws IOException {
        generator.writeFieldName(RESULT_FIELD_NAME);
        final Output<?> result = isNull(executionResult) ?  Output.emptyResult() : Output.of(executionResult);
        generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(result));
    }

    // serialize input parameter
    private static <I> void serializeParameter(final Input<I> input,
                                               final JsonGenerator generator,
                                               final String fieldName) throws IOException {
        if (isNull(input) || input.isEmpty()) {
            // if input is null or empty, do not serialize
            return;
        }
        generator.writeFieldName(fieldName);
        generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(input));
    }

    // serialize redo parameter
    private static <R> void serializeRedoParameter(final Input<R> input, final JsonGenerator generator) throws IOException {
        serializeParameter(input, generator, REDO_INPUT_FIELD_NAME);
    }

    // serialize undo parameter
    private static <U> void serializeUndoParameter(final Input<U> undoInput, final JsonGenerator generator) throws IOException {
        serializeParameter(undoInput, generator, UNDO_INPUT_FIELD_NAME);
    }

    private void serializeExecutionError(final Throwable error, final JsonGenerator generator) throws IOException {
        if (nonNull(error)) {
            generator.writeFieldName(ERROR_FIELD_NAME);
            exceptionSerializer.serialize(error, generator, null);
        }
    }

    private void serializeHistory(final Context.LifeCycleHistory history, final JsonGenerator generator) throws IOException {
        generator.writeFieldName(HISTORY_FIELD_NAME);
        generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(history));
    }
}
