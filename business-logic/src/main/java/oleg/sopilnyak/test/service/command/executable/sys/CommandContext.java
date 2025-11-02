package oleg.sopilnyak.test.service.command.executable.sys;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.FAIL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.INIT;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.READY;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.WORK;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.executable.sys.context.history.History;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(buildMethodName = "buildInternal")
public class CommandContext<T> implements Context<T> {
    private RootCommand<T> command;
    @JsonDeserialize(using = Input.ParameterDeserializer.class)
    @Builder.Default
    private Input<?> redoParameter = Input.empty();
    @JsonDeserialize(using = Input.ParameterDeserializer.class)
    @Builder.Default
    private Input<?> undoParameter = Input.empty();
    private transient T resultData;
    private Exception exception;
    // the time when execution starts
    private Instant startedAt;
    // the value of command execution duration
    private Duration duration;

    @Setter(AccessLevel.NONE)
    private volatile State state;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final History history = History.builder().build();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final Queue<StateChangedListener> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Add some functionality to the generated builder class
     */
    public static class CommandContextBuilder<T> {
        // to build context with INIT state
        public CommandContext<T> build() {
            final CommandContext<T> context = buildInternal();
            context.listeners.add(new InternalStateChangedListener(context));
            return context;
        }
    }

    /**
     * To set up current state of the context
     *
     * @param currentState new current context's state
     */
    @Override
    public void setState(final State currentState) {
        if (currentState != this.state) {
            final State previousState = this.state;
            listeners.forEach(listener -> listener.stateChanged(this, previousState, currentState));
            this.state = currentState;
        }
    }

    /**
     * To set up input parameter value for command execution
     *
     * @param parameter the value
     * @param <R>       type of do input parameter
     */
    public <R> void setRedoParameter(final Input<R> parameter) {
        this.redoParameter = parameter;
        if (state == INIT) {
            setState(READY);
        }
    }

    /**
     * To set up input parameter value for rollback changes
     *
     * @param parameter the value
     * @param <U>       type of undo input parameter
     */
    public <U> void setUndoParameter(final Input<U> parameter) {
        if (Set.of(DONE, WORK).contains(state)) {
            undoParameter = nonNull(parameter) ? parameter : Input.empty();
        }
    }

    /**
     * To get the result of command execution
     *
     * @return the value of result
     * @see State#DONE
     */
    @Override
    public Optional<T> getResult() {
        return Optional.ofNullable(resultData);
    }

    /**
     * To set up the result of command execution
     *
     * @param result the value of result
     * @see State#DONE
     */
    @Override
    public void setResult(final T result) {
        if (isWorking()) {
            this.resultData = result;
            // null result is going from command undo
            setState(isNull(result) ? WORK : DONE);
        }
    }

    /**
     * Mark context as failed
     *
     * @param exception cause of failure
     * @return failed context instance
     * @see Exception
     * @see this#setState(State)
     * @see Context.State#FAIL
     * @see this#setException(Exception)
     */
    @Override
    public Context<T> failed(Exception exception) {
        setResult(null);
        this.exception = exception;
        setState(State.FAIL);
        return this;
    }

    /**
     * To add change context state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    @Override
    public void addStateListener(final StateChangedListener listener) {
        listeners.offer(listener);
    }

    /**
     * To remove change-context-state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    @Override
    public void removeStateListener(final StateChangedListener listener) {
        listeners.remove(listener);
    }

    // private methods

    // nested classes

    /**
     * Internal listener of context state changes
     * <p>
     * It is used to save context history according to the context's state changes.
     *
     * @see Context.State
     * @see LifeCycleHistory
     */
    private static class InternalStateChangedListener implements StateChangedListener {
        private final Predicate<State> isReady = Set.of(READY, DONE)::contains;
        private final Predicate<State> isWorks = WORK::equals;
        private final Predicate<State> isFinished = Set.of(DONE, UNDONE, FAIL)::contains;
        private final CommandContext<?> commandContext;

        public <T> InternalStateChangedListener(CommandContext<T> context) {
            this.commandContext = context;
        }

        @Override
        public void stateChanged(final Context<?> context, final State previous, final State current) {
            if (!Objects.equals(commandContext, genuine(context))) {
                log.error("State changed for context {}, but not for {}", context, commandContext);
                return;
            }
            if (nonNull(previous) && isReady.test(previous) && isWorks.test(current)) {
                log.debug("Execution of command with id:'{}' is started", context.getCommand().getId());
                commandExecutionStarted(context, justNow(), previous);
            } else if (isFinished.test(current) && isWorks.test(previous)) {
                log.debug("Execution of command with id:'{}' is finished", context.getCommand().getId());
                commandExecutionFinishedBy(context, current);
            }
            log.debug("To save to context-history the current state {} (just changed)", current);
            commandContext.history.add(current);
        }

        /**
         * To get genuine context instance
         * <p>
         * If context is a spy, then it returns the spied instance.
         * If context is a mock, then it returns null.
         * Otherwise, it returns the context itself.
         *
         * @param context the context to check
         * @return genuine context instance or null if context is a mock
         */
        private static Context<?> genuine(final Context<?> context) {
            if (MockUtil.isSpy(context)) {
                log.debug("Context is a spy, returning spied instance");
                // if context is a spy, we can retrieve genuine instance from it
                return (Context<?>) Mockito.mockingDetails(context).getMockCreationSettings().getSpiedInstance();
            } else {
                log.debug("Context is not a spy... returning it as is (if not a mock)");
                // if it is a mock, then we return null
                // if context is not a mock, then we can return it as is
                return MockUtil.isMock(context) ? null : context;
            }
        }

        /**
         * To save to context-history time when command started execution with context
         *
         * @param context      the context of command execution
         * @param startedAt    the time when command execution started
         * @param startedAfter which state was before
         * @see Context#getState()
         * @see Context#getStartedAt()
         * @see LifeCycleHistory
         * @see State#WORK
         * @see RootCommand#executeDo(Context)
         * @see RootCommand#executeUndo(Context)
         */
        private static void commandExecutionStarted(final Context<?> context, final Instant startedAt, final State startedAfter) {
            if (context instanceof CommandContext<?> commandContext) {
                log.debug("Command execution started at {} after state {}", startedAt, startedAfter);
                commandContext.history.add(startedAt, startedAfter);
                commandContext.setStartedAt(startedAt);
            } else {
                log.error("Command execution started, but context is not CommandContext: {}", context);
            }
        }

        /**
         * To save to context-history duration of command execution with context
         *
         * @param context    the context of command execution
         * @param finishedBy the state which finishes command execution
         * @see Context#getState()
         * @see Context#getDuration()
         * @see LifeCycleHistory
         * @see State#DONE
         * @see State#UNDONE
         * @see State#FAIL
         * @see RootCommand#executeDo(Context)
         * @see RootCommand#executeUndo(Context)
         */
        private static void commandExecutionFinishedBy(Context<?> context, final State finishedBy) {
            final Instant startedAt = context.getStartedAt();
            if (isNull(startedAt)) {
                log.warn("Command execution finished by state {}, but started-at is null.\nDuration will not be set ", finishedBy);
                return;
            }

            // saving duration of command execution
            if (context instanceof CommandContext<?> commandContext) {
                // if context is CommandContext, then we can save duration
                log.debug("Command execution finished by state {}, started at {}", finishedBy, startedAt);
                final Duration lastDuration = Duration.between(startedAt, justNow());
                log.debug("Calculate and store command execution duration {} ns", lastDuration.getNano());
                commandContext.setDuration(lastDuration);
                commandContext.history.add(lastDuration, finishedBy);
            } else {
                // otherwise, we just log the information
                log.error("Command execution finished, but context is not CommandContext: {}", context);
            }
        }

        // to get current time mark
        private static Instant justNow() {
            return Instant.now();
        }
    }
}
