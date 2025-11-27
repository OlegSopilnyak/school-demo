package oleg.sopilnyak.test.service.facade.profile.impl;

import static oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand.CommandId;

import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service: To process commands for school's principal profiles facade
 */
@Slf4j
public class PrincipalProfileFacadeImpl
        extends PersonProfileFacadeImpl<PrincipalProfileCommand<?>>
        implements PrincipalProfileFacade {

    public PrincipalProfileFacadeImpl(final CommandsFactory<PrincipalProfileCommand<?>> factory,
                                      final BusinessMessagePayloadMapper payloadMapper) {
        super(factory, payloadMapper);
    }

    @Override
    protected final String findByIdCommandId() {
        return CommandId.FIND_BY_ID;
    }

    @Override
    protected final String createOrUpdateCommandId() {
        return CommandId.CREATE_OR_UPDATE;
    }

    @Override
    protected final String deleteByIdCommandId() {
        return CommandId.DELETE_BY_ID;
    }
}
