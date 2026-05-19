package oleg.sopilnyak.test.service.command.type.core.json;

import static java.util.Objects.nonNull;

import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.executable.core.context.history.History;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Json Deserializer: deserializer for Context
 *
 * @param <T> type result for the context
 * @see Context
 * @see CommandsFactoriesFarm
 */
public class CommandContextDeserializer<T> extends StdDeserializer<Context<T>> implements JsonContextFields {
    private final transient CommandsFactoriesFarm<? extends RootCommand<T>> farm;
    private final IOBase.ExceptionDeserializer errorDeserializer = new IOBase.ExceptionDeserializer();
    private final Input.ParameterDeserializer<?> parameterDeserializer = new Input.ParameterDeserializer<>();
    private final Output.ResultDeserializer<T> resultDeserializer = new Output.ResultDeserializer<>();

    public CommandContextDeserializer(final CommandsFactoriesFarm<? extends RootCommand<T>> farm) {
        this(Context.class, farm);
    }

    protected CommandContextDeserializer(Class<?> vc, CommandsFactoriesFarm<? extends RootCommand<T>> farm) {
        super(vc);
        this.farm = farm;
    }

    @Override
    public Context<T> deserialize(
            final JsonParser jsonParser, final DeserializationContext ignored
    ) throws IOException {
        final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        final TreeNode treeNode = jsonParser.readValueAsTree();
        final CommandContext.CommandContextBuilder<T> contextBuilder = CommandContext.<T>builder();
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
        // restore command instance from factories farm by command-id
        final RootCommand<T> command = restoreCommandFromFactoriesFarm(treeNode.get(COMMAND_ID_FIELD_NAME));
        // check command-family-type and setup context though context builder
        deserializeCommandFamily(contextBuilder, treeNode.get(COMMAND_FAMILY_FIELD_NAME), command);
    }

    // restore command instance from factories farm by command-id
    private RootCommand<T> restoreCommandFromFactoriesFarm(TreeNode commandIdNode) throws IOException {
        if (nonNull(commandIdNode) && commandIdNode instanceof TextNode textIdNode) {
            // getting command instance from commands factories Farm by command-id
            return farm.command(textIdNode.textValue());
        } else {
            throw new IOException("Command ID TreeNode is missing :" + commandIdNode);
        }
    }

    // check command-family-type and setup context though context builder
    private void deserializeCommandFamily(
            CommandContext.CommandContextBuilder<T> contextBuilder, TreeNode commandTypeNode, RootCommand<T> command
    ) throws IOException {
        if (commandTypeNode instanceof TextNode textTypeNode) {
            final String commandFamilyTypeName = textTypeNode.textValue();
            try {
                final Class<?> commandFamilyType = Class.forName(commandFamilyTypeName).asSubclass(RootCommand.class);
                if (commandFamilyType.equals(command.commandFamily())) {
                    // add valid command to context builder
                    contextBuilder.command(command);
                } else {
                    throw new IOException("Command Family Type is missing :" + commandFamilyType);
                }
            } catch (ClassNotFoundException _) {
                throw new IOException("Command Family Type is missing :" + commandFamilyTypeName);
            }
        } else {
            throw new IOException("Command Family Type TreeNode is missing :" + commandTypeNode);
        }
    }

    private void deserializeRedoParameter(final TreeNode treeNode,
                                          final CommandContext.CommandContextBuilder<T> contextBuilder,
                                          final ObjectMapper mapper) throws IOException {
        if (treeNode == null) {
            contextBuilder.redoParameter(Input.emptyParameter());
            return;
        }
        // restore redo parameter
        final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
        final Input<?> redoParameter = parameterDeserializer.deserialize(parser, null);
        contextBuilder.redoParameter(redoParameter);
    }

    private void deserializeUndoParameter(final TreeNode treeNode,
                                          final CommandContext.CommandContextBuilder<T> contextBuilder,
                                          final ObjectMapper mapper) throws IOException {
        if (treeNode == null) {
            contextBuilder.undoParameter(Input.emptyParameter());
            return;
        }
        // restore undo parameter
        final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
        final Input<?> undoParameter = parameterDeserializer.deserialize(parser, null);
        contextBuilder.undoParameter(undoParameter);
    }

    private void deserializeExecutionResult(final TreeNode treeNode,
                                            final CommandContext.CommandContextBuilder<T> contextBuilder,
                                            final ObjectMapper mapper) throws IOException {
        if (treeNode == null) {
            return;
        }
        // restore execution result
        final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
        final Output<T> executionResult = resultDeserializer.deserialize(parser, null);
        contextBuilder.resultData(executionResult.value());
    }

    private void deserializeExecutionError(final TreeNode treeNode,
                                           final CommandContext.CommandContextBuilder<T> contextBuilder,
                                           final ObjectMapper mapper) throws IOException {
        if (treeNode == null) {
            return;
        }
        // restore execution error
        final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
        final Throwable error = errorDeserializer.deserialize(parser, null);
        contextBuilder.exception((Exception) error);
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

