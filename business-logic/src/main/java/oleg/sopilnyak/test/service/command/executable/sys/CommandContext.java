package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandContext<T> implements Context<T> {
    private SchoolCommand<T> command;
    private State state = State.INIT;
    private Object doParameter;
    private Object undoParameter;
    private Optional<T> result = Optional.empty();
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
        if(state == State.WORK && value != null) {
            result = Optional.of(value);
            state = State.DONE;
        }
    }
}
