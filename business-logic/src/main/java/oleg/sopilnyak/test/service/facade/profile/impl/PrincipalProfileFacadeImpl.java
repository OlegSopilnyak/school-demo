package oleg.sopilnyak.test.service.facade.profile.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * Service: To process commands for school's principal profiles facade
 */
@Slf4j
public class PrincipalProfileFacadeImpl extends PersonProfileFacadeImpl<PrincipalProfileCommand<?>>
        implements PrincipalProfileFacade {

    public PrincipalProfileFacadeImpl(final CommandsFactory<PrincipalProfileCommand<?>> factory,
                                      final BusinessMessagePayloadMapper payloadMapper,
                                      final CommandActionExecutor actionExecutor) {
        super(factory, payloadMapper, actionExecutor);
    }

    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }
}
