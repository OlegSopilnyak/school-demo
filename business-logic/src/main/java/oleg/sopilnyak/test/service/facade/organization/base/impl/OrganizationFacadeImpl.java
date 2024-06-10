package oleg.sopilnyak.test.service.facade.organization.base.impl;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.business.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

/**
 * Base Service: To process commands for school's organization structure
 */
@AllArgsConstructor
public class OrganizationFacadeImpl<T extends OrganizationCommand> implements OrganizationFacade {
    public static final String WRONG_COMMAND_EXECUTION = "For command-id:'{}' there is not exception after wrong command execution.";
    public static final String EXCEPTION_IS_NOT_STORED = "Exception is not stored!!!";
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    protected final CommandsFactory<T> factory;
}
