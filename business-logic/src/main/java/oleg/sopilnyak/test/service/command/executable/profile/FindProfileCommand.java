package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.function.LongFunction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;

/**
 * Command-Base-Implementation: command to get profile by id
 *
 * @param <E> command's working Entity type
 * @see PersonProfile
 * @see ProfileCommand
 * @see ProfilePersistenceFacade
 */
@Getter
public abstract class FindProfileCommand<E extends PersonProfile> extends BasicCommand<Optional<E>>
        implements ProfileCommand<Optional<E>> {
    protected final transient ProfilePersistenceFacade persistence;
    protected final transient BusinessMessagePayloadMapper payloadMapper;

    protected FindProfileCommand(ProfilePersistenceFacade persistence, BusinessMessagePayloadMapper payloadMapper) {
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * to get function to find entity by id
     *
     * @return function implementation
     */
    protected abstract LongFunction<Optional<E>> functionFindById();

    /**
     * DO: To find profile (no matter type) by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#functionFindById()
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Optional<E>> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            getLog().debug("Trying to find profile by ID:{}", parameter);
            final Long id = parameter.value();

            final Optional<E> profile = functionFindById().apply(id).map(this::adoptEntity);

            getLog().debug("Got profile {} by ID:{}", profile, id);
            context.setResult(profile);
        } catch (Exception e) {
            getLog().error("Cannot find the profile by ID:{}", parameter, e);
            context.failed(e);
        }
    }
}
