package oleg.sopilnyak.test.service.facade.profile.impl;

import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;
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
    //
    // semantic data to payload converter operator
    private final UnaryOperator<PersonProfile> payloadOperator;

    public StudentProfileFacadeImpl(final CommandsFactory<StudentProfileCommand<?>> factory,
                                    final BusinessMessagePayloadMapper payloadMapper,
                                    final CommandActionExecutor actionExecutor) {
        super(factory, actionExecutor);
        this.payloadOperator = profile -> profile instanceof StudentProfilePayload ? profile : payloadMapper.toPayload(profile);
    }

    /**
     * Concrete profile action processing facade method
     *
     * @param actionId the id of the action
     * @param argument the parameters of action to execute
     * @return action execution result value
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T profileAction(final String actionId, final Object argument) {
        return (T) switch (actionId) {
            case StudentProfileFacade.FIND_BY_ID -> super.internalFindById(actionId, argument);
            case StudentProfileFacade.CREATE_OR_UPDATE -> super.internalCreateOrUpdate(actionId, argument);
            case StudentProfileFacade.DELETE_BY_ID -> super.internalDelete(actionId, argument);
            default -> unknownActionId(actionId);
        };
    }

    /**
     * To get the operator to convert entity to payload
     *
     * @return unary operator
     * @see UnaryOperator#apply(Object)
     */
    @Override
    protected final UnaryOperator<PersonProfile> toPayload() {
        return payloadOperator;
    }

    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public final Logger getLogger() {
        return log;
    }
}
