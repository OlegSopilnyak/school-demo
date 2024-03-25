package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;

import static java.util.Objects.nonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandContext<T> implements Context<T> {
    private SchoolCommand<T> command;
    private State state;
    private Object doParameter;
    private Object undoParameter;
    private Optional<T> result;
    private Exception exception;
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
     * To set up parameter value for command execution
     *
     * @param parameter the value
     */
    @Override
    public void setDoParameter(Object parameter) {
        this.doParameter = parameter;
        if (state == State.INIT) {
            this.state = State.READY;
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
     * To set up the result of command execution
     *
     * @param value the value of result
     * @see State#DONE
     */
    @Override
    public void setResult(T value) {
        setResult(nonNull(value) ? Optional.of(value) : Optional.empty());
    }

    public void setResult(Optional<T> result) {
        if(state == State.WORK) {
            this.result = result;
            state = State.DONE;
        }
    }
}
