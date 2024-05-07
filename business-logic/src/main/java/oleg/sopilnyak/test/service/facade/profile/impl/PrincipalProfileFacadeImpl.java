package oleg.sopilnyak.test.service.facade.profile.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;

import static oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand.*;

/**
 * Service: To process commands for school's principal profiles facade
 */
@Slf4j
public class PrincipalProfileFacadeImpl
        extends PersonProfileFacadeImpl<PrincipalProfileCommand>
        implements PrincipalProfileFacade {

    public PrincipalProfileFacadeImpl(CommandsFactory<PrincipalProfileCommand> factory) {
        super(factory);
    }

    @Override
    protected final String findByIdCommandId() {
        return FIND_BY_ID;
    }

    @Override
    protected final String createOrUpdateCommandId() {
        return CREATE_OR_UPDATE;
    }

    @Override
    protected final String deleteByIdCommandId() {
        return DELETE_BY_ID;
    }
}
