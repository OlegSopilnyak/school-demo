package oleg.sopilnyak.test.service.command.type.base;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.context.History;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * ObjectMapper:Module: The module to serialize/deserialize The Context
 *
 * @see Context
 */
public class JsonContextModule extends SimpleModule {
    private static final String COMMAND_FIELD_NAME = "command";
    private static final String COMMAND_ID_FIELD_NAME = "id";
    private static final String STARTED_AT_FIELD_NAME = "started-at";
    private static final String DURATION_FIELD_NAME = "duration";
    private static final String STATE_FIELD_NAME = "state";
    private static final String RESULT_FIELD_NAME = "result";
    private static final String REDO_INPUT_FIELD_NAME = "redo-input";
    private static final String UNDO_INPUT_FIELD_NAME = "undo-input";
    private static final String ERROR_FIELD_NAME = "error";
    private static final String HISTORY_FIELD_NAME = "history";

    private final transient CommandsFactoriesFarm<?> farm;

    public JsonContextModule(ApplicationContext context, CommandsFactoriesFarm<?> farm) {
        Assert.notNull(context, "Context must not be null");
        // get the commands farm
        this.farm = farm;
    }

    @Override
    public void setupModule(SetupContext context) {
        final SimpleSerializers serializers = new SimpleSerializers();
        final SimpleDeserializers deserializers = new SimpleDeserializers();
        // add serializer/deserializer for command context
        serializers.addSerializer(Context.class, new CommandContextSerializer<>());
        deserializers.addDeserializer(Context.class, new CommandContextDeserializer<>(farm));
        // apply modified serializer/deserializer
        context.addSerializers(serializers);
        context.addDeserializers(deserializers);

    }

    /**
     * Json Serializer: serializer for Context
     *
     * @param <T> type result for the context
     * @see Context
     * @see RootCommand#executeDo(Context)
     * @see RootCommand#executeUndo(Context)
     */
    static class CommandContextSerializer<T> extends StdSerializer<Context<T>> {
        private final IOBase.ExceptionSerializer<? extends Throwable> exceptionSerializer = new IOBase.ExceptionSerializer<>();

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
            serializeExecutionResult(context.getResult(), generator);
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
            generator.writeStringField(TYPE_FIELD_NAME, command.getClass().getName());
            generator.writeEndObject();
        }

        private void serializeExecutionResult(final Optional<T> executionResult, final JsonGenerator generator) throws IOException {
            generator.writeFieldName(RESULT_FIELD_NAME);
            final Output<?> result = executionResult.isPresent() ? Output.of(executionResult.get()) : Output.empty();
            generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(result));
        }

        private static <R> void serializeRedoParameter(final Input<R> redoInput, final JsonGenerator generator) throws IOException {
            if (!redoInput.isEmpty()) {
                generator.writeFieldName(REDO_INPUT_FIELD_NAME);
                generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(redoInput));
            }
        }

        private static <U> void serializeUndoParameter(final Input<U> undoInput, final JsonGenerator generator) throws IOException {
            if (!undoInput.isEmpty()) {
                generator.writeFieldName(UNDO_INPUT_FIELD_NAME);
                generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(undoInput));
            }
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

    /**
     * Json Deserializer: deserializer for Context
     *
     * @param <T> type result for the context
     * @see Context
     * @see RootCommand#executeDo(Context)
     * @see RootCommand#executeUndo(Context)
     */
    static class CommandContextDeserializer<T> extends StdDeserializer<Context<T>> {
        private final transient CommandsFactoriesFarm<? extends RootCommand<T>> factoriesFarm;
        private final IOBase.ExceptionDeserializer errorDeserializer = new IOBase.ExceptionDeserializer();
        private final Input.ParameterDeserializer<?> parameterDeserializer = new Input.ParameterDeserializer<>();
        private final Output.ResultDeserializer<T> resultDeserializer = new Output.ResultDeserializer<>();

        public CommandContextDeserializer(final CommandsFactoriesFarm<? extends RootCommand<T>> factoriesFarm) {
            this(Context.class, factoriesFarm);
        }

        protected CommandContextDeserializer(Class<?> vc, CommandsFactoriesFarm<? extends RootCommand<T>> factoriesFarm) {
            super(vc);
            this.factoriesFarm = factoriesFarm;
        }

        @Override
        public Context<T> deserialize(final JsonParser jsonParser,
                                      final DeserializationContext deserializationContext) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final CommandContext.CommandContextBuilder<T> contextBuilder = CommandContext.builder();
            deserializeCommand(treeNode.get(COMMAND_FIELD_NAME), contextBuilder);
            deserializeRedoParameter(treeNode.get(REDO_INPUT_FIELD_NAME), contextBuilder, mapper);
            deserializeUndoParameter(treeNode.get(UNDO_INPUT_FIELD_NAME), contextBuilder, mapper);
            deserializeExecutionResult(treeNode.get(RESULT_FIELD_NAME), contextBuilder, mapper);
            deserializeExecutionError(treeNode.get(ERROR_FIELD_NAME), contextBuilder, mapper);
            deserializeStartedAt(treeNode.get(STARTED_AT_FIELD_NAME), contextBuilder, mapper);
            deserializeDuration(treeNode.get(DURATION_FIELD_NAME), contextBuilder, mapper);
            deserializeState(treeNode.get(STATE_FIELD_NAME), contextBuilder);
            deserializeHistory(treeNode.get(HISTORY_FIELD_NAME), contextBuilder, mapper);
            return contextBuilder.build();
        }

        private void deserializeCommand(TreeNode treeNode, CommandContext.CommandContextBuilder<T> contextBuilder) throws IOException {
            final RootCommand<T> command;
            final TreeNode commandIdNode = treeNode.get(COMMAND_ID_FIELD_NAME);
            // deserialize command by command-id
            if (nonNull(commandIdNode) && commandIdNode instanceof TextNode textIdNode) {
                final String commandId = textIdNode.textValue();
                command = factoriesFarm.command(commandId);
            } else {
                throw new IOException("Command ID Node is missing :" + commandIdNode);
            }
            // check command-type
            final TreeNode commandTypeNode = treeNode.get(TYPE_FIELD_NAME);
            if (nonNull(commandTypeNode) && commandTypeNode instanceof TextNode textTypeNode) {
                final String commandType = textTypeNode.textValue();
                try {
                    final Class<?> commandClass = Class.forName(commandType).asSubclass(RootCommand.class);
                    if (commandClass.equals(command.getClass())) {
                        // add valid command to context builder
                        contextBuilder.command(command);
                    } else {
                        throw new IOException("Command Type is missing :" + commandClass);
                    }
                } catch (ClassNotFoundException e) {
                    throw new IOException("Command Type is missing :" + commandType);
                }
            } else {
                throw new IOException("Command Type Node is missing :" + commandTypeNode);
            }
        }

        private <R> void deserializeRedoParameter(final TreeNode treeNode,
                                                  final CommandContext.CommandContextBuilder<T> contextBuilder,
                                                  final ObjectMapper mapper) throws IOException {
            if (nonNull(treeNode)) {
                // restore redo parameter
                final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
                final Input<R> redoParameter = parameterDeserializer.deserialize(parser, null);
                contextBuilder.redoParameter(redoParameter);
            }
        }

        private <U> void deserializeUndoParameter(final TreeNode treeNode,
                                                  final CommandContext.CommandContextBuilder<T> contextBuilder,
                                                  final ObjectMapper mapper) throws IOException {
            if (nonNull(treeNode)) {
                // restore undo parameter
                final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
                final Input<U> undoParameter = parameterDeserializer.deserialize(parser, null);
                contextBuilder.undoParameter(undoParameter);
            }
        }

        private void deserializeExecutionResult(final TreeNode treeNode,
                                                final CommandContext.CommandContextBuilder<T> contextBuilder,
                                                final ObjectMapper mapper) throws IOException {
            if (nonNull(treeNode)) {
                // restore execution result
                final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
                final Output<T> executionResult = resultDeserializer.deserialize(parser, null);
                contextBuilder.resultData(executionResult.value());
            }
        }

        private void deserializeExecutionError(final TreeNode treeNode,
                                               final CommandContext.CommandContextBuilder<T> contextBuilder,
                                               final ObjectMapper mapper) throws IOException {
            if (nonNull(treeNode)) {
                // restore execution error
                final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
                final Throwable error = errorDeserializer.deserialize(parser, null);
                contextBuilder.exception(Exception.class.cast(error));
            }
        }

        private void deserializeStartedAt(final TreeNode treeNode,
                                          final CommandContext.CommandContextBuilder<T> contextBuilder,
                                          final ObjectMapper mapper) throws IOException {
            if (nonNull(treeNode) && treeNode instanceof TextNode textNode) {
                // restore execution error
                final Instant startedAt = mapper.readValue(textNode.textValue(), Instant.class);
                contextBuilder.startedAt(startedAt);
            }
        }

        private void deserializeDuration(final TreeNode treeNode,
                                         final CommandContext.CommandContextBuilder<T> contextBuilder,
                                         final ObjectMapper mapper) throws IOException {
            if (nonNull(treeNode) && treeNode instanceof TextNode textNode) {
                // restore execution error
                final Duration duration = mapper.readValue(textNode.textValue(), Duration.class);
                contextBuilder.duration(duration);
            }
        }

        private void deserializeState(final TreeNode treeNode,
                                      final CommandContext.CommandContextBuilder<T> contextBuilder) {
            if (nonNull(treeNode) && treeNode instanceof TextNode textNode) {
                // restore execution state
                final Context.State state = Context.State.valueOf(textNode.textValue());
                contextBuilder.state(state);
            }
        }

        private void deserializeHistory(final TreeNode treeNode,
                                      final CommandContext.CommandContextBuilder<T> contextBuilder,
                                      final ObjectMapper mapper) throws IOException {
            if (nonNull(treeNode)) {
                // restore execution history
                final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
                final History history = mapper.readValue(parser, History.class);
                contextBuilder.history(history);
            }
        }
    }
}
