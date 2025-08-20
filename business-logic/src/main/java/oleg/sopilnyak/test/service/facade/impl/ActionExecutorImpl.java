package oleg.sopilnyak.test.service.facade.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import org.slf4j.Logger;

@Slf4j
public class ActionExecutorImpl implements ActionExecutor {
    /**
     * To get the logger of the executor implementation
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }
}
