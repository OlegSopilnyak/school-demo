package oleg.sopilnyak.test.service.command.io.result;

import oleg.sopilnyak.test.service.command.io.Output;

/**
 * Type: I/O school-command empty command execution result (no results)
 */
public class EmptyResult implements Output<Boolean> {
    /**
     * To get the value of command execution result
     *
     * @return value of the result
     */
    @Override
    public Boolean value() {
        return null;
    }
}
