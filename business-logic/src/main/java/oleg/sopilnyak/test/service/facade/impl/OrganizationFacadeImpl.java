package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;

/**
 * Base Service: To process commands for school's organization structure
 */
@AllArgsConstructor
public class OrganizationFacadeImpl implements OrganizationFacade {
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    protected final CommandsFactory<?> factory;
}
