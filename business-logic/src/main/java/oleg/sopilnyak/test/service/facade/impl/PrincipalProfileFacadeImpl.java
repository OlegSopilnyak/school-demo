package oleg.sopilnyak.test.service.facade.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.PrincipalProfileCommand;

/**
 * Service: To process commands for school's principal profiles facade
 */
@Slf4j
public class PrincipalProfileFacadeImpl<T>
        extends PersonProfileFacadeImpl<T>
        implements PrincipalProfileFacade {

    public PrincipalProfileFacadeImpl(CommandsFactory<T> factory) {
        super(factory);
    }

    @Override
    protected String findByIdCommandId() {
        return PrincipalProfileCommand.FIND_BY_ID_COMMAND_ID;
    }

    @Override
    protected String createOrUpdateCommandId() {
        return PrincipalProfileCommand.CREATE_OR_UPDATE_COMMAND_ID;
    }

    @Override
    protected String deleteByIdCommandId() {
        return PrincipalProfileCommand.DELETE_BY_ID_COMMAND_ID;
    }
}
