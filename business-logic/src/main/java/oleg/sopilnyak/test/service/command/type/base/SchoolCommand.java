package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.CommandResult;

/**
 * Command: entity to execute the business-logic command
 */
public interface SchoolCommand<T> {
    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     */
    CommandResult<T> execute(Object parameter);

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    String getId();

    /**
     * Cast parameter to particular type
     *
     * @param parameter actual parameter
     * @return parameter cast to particular type
     * @param <P> type of the parameter
     */
    @SuppressWarnings("unchecked")
    default <P> P commandParameter(Object parameter){
        return (P)parameter;
    }
}
