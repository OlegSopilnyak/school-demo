package oleg.sopilnyak.test.service.facade.profile.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.base.impl.PersonProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * Service: To process commands for school's student profiles facade
 */
@Slf4j
public class StudentProfileFacadeImpl extends PersonProfileFacadeImpl<StudentProfileCommand<?>>
        implements StudentProfileFacade {

    public StudentProfileFacadeImpl(final CommandsFactory<StudentProfileCommand<?>> factory,
                                    final BusinessMessagePayloadMapper payloadMapper,
                                    final CommandActionExecutor actionExecutor) {
        super(factory, payloadMapper, actionExecutor);
    }
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
        this.toPayloadMapperFunction = profile -> profile instanceof StudentProfilePayload ? profile : mapper.toPayload(profile);
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
