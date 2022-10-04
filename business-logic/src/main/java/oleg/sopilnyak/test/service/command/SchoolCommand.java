package oleg.sopilnyak.test.service.command;

/**
 * Command: entity to execute command
 */
public interface SchoolCommand<T> {
    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     */
    CommandResult<T> execute(Object parameter);
}
