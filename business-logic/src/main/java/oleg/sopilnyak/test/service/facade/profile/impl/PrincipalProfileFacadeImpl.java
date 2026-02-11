package oleg.sopilnyak.test.service.facade.profile.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * Service: To process commands for school's principal profiles facade
 */
@Slf4j
public class PrincipalProfileFacadeImpl extends PersonProfileFacadeImpl<PrincipalProfileCommand<?>>
        implements PrincipalProfileFacade {
    //
    // semantic data to payload converter
    private UnaryOperator<PersonProfile> toPayloadMapperFunction;

    /**
     * To get the operator to convert entity to payload
     *
     * @return unary operator
     * @see UnaryOperator#apply(Object)
     */
    @Override
    protected UnaryOperator<PersonProfile> toPayload() {
        return toPayloadMapperFunction;
    }

    /**
     * To prepare the operator to convert entity to payload
     *
     * @param mapper layer's data mapper
     * @see this#toPayload()
     * @see BusinessMessagePayloadMapper
     */
    @Override
    protected void prepareToPayloadFunction(final BusinessMessagePayloadMapper mapper) {
        this.toPayloadMapperFunction = profile -> profile instanceof PrincipalProfilePayload ? profile : mapper.toPayload(profile);
    }

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
