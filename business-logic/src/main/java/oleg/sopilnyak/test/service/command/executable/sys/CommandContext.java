package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.*;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandContext<T> implements Context<T> {
    private RootCommand<T> command;
    private Object redoParameter;
    private Object undoParameter;
    private T resultData;
    private Exception exception;

    @Setter(AccessLevel.NONE)
    private volatile State state;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final List<State> states = Collections.synchronizedList(new LinkedList<>());

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final List<StateChangedListener> listeners = Collections.synchronizedList(new LinkedList<>());

    /**
     * To set up current state of the context
     *
     * @param newState new current context's state
     */
    @Override
    public void setState(final State newState) {
        if (newState != state) {
            final State oldState = this.state;
            this.states.add(newState);
            this.state = newState;
            notifyStateChangedListeners(oldState, newState);
        }
    }

    /**
     * To set up parameter value for command execution
     *
     * @param parameter the value
     */
    @Override
    public void setRedoParameter(Object parameter) {
        this.redoParameter = parameter;
        if (state == INIT) {
            setState(READY);
        }
    }

    /**
     * To set up parameter value for rollback changes
     *
     * @param parameter the value
     */
    @Override
    public void setUndoParameter(Object parameter) {
        if (isDone() || isWorking()) {
            undoParameter = parameter;
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
    public void setResult(T result) {
        if (isWorking()) {
            this.resultData = result;
            setState(DONE);
        }
    }

    /**
     * To get states of context during context's life-cycle
     *
     * @return list of states
     */
    public List<State> getStates() {
        return List.copyOf(states);
    }

    /**
     * To add change context state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    @Override
    public void addStateListener(final StateChangedListener listener) {
        listeners.add(listener);
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
    private void notifyStateChangedListeners(final State previous, final State last) {
        if (ObjectUtils.isEmpty(listeners)) return;
        // delivery state-changed notification
        final Consumer<StateChangedListener> doStateChangedNotification =
                listener -> listener.stateChanged(this, previous, last);
        listeners.forEach(doStateChangedNotification);
    }
}
