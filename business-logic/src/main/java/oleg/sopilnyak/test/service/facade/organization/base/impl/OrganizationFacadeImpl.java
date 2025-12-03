package oleg.sopilnyak.test.service.facade.organization.base.impl;

import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;

import org.slf4j.Logger;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Base Service: To process commands for school's organization structure
 */
@AllArgsConstructor
public class OrganizationFacadeImpl<T extends OrganizationCommand<?>> implements OrganizationFacade, ActionFacade {
    protected static final String WRONG_COMMAND_EXECUTION = "For command-id:'{}' there is not exception after wrong command execution.";
    protected static final String EXCEPTION_IS_NOT_STORED = "Exception is not stored!!!";
    protected static final String SOMETHING_WENT_WRONG = "Something went wrong";
    protected final CommandsFactory<T> factory;
    @Getter
    protected final ActionExecutor actionExecutor;

    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        throw new UnsupportedOperationException("Please override this method.");
    }
}
