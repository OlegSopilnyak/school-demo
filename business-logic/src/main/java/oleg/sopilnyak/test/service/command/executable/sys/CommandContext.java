package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.*;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandContext<T> implements Context<T> {
    private SchoolCommand<T> command;
    @Setter(AccessLevel.NONE)
    private State state;
    private Object doParameter;
    private Object undoParameter;
    private T resultData;
    private Exception exception;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<State> states;

    /**
     * To get the command associated with the context
     *
     * @return command instance
     * @see SchoolCommand
     */
    @Override
    public SchoolCommand<T> getCommand() {
        return command;
    }

    /**
     * To set up current state of the context
     *
     * @param state new current context's state
     */
    @Override
    public void setState(State state) {
        states.add(state);
        this.state = state;
    }

    /**
     * To set up parameter value for command execution
     *
     * @param parameter the value
     */
    @Override
    public void setDoParameter(Object parameter) {
        this.doParameter = parameter;
        if (state == State.INIT) {
            setState(State.READY);
        }
    }

    /**
     * To set up parameter value for rollback changes
     *
     * @param parameter the value
     */
    @Override
    public void setUndoParameter(Object parameter) {
        if (state == State.DONE || state == State.WORK) {
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
        if (state == State.WORK) {
            this.resultData = result;
            setState(State.DONE);
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
}
