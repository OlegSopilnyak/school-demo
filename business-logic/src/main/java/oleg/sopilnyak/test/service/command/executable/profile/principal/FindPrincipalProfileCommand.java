package oleg.sopilnyak.test.service.command.executable.profile.principal;

import static java.util.Objects.isNull;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.LongFunction;


/**
 * Command-Implementation: command to find student profile instance
 *
 * @see PrincipalProfileCommand
 * @see PrincipalProfile
 * @see ProfilePersistenceFacade
 */
@Slf4j
@Component
public class FindPrincipalProfileCommand extends FindProfileCommand<PrincipalProfile>
        implements PrincipalProfileCommand<Optional<PrincipalProfile>> {

    /**
     * Constructor
     *
     * @param persistence persistence facade instance
     */
    public FindPrincipalProfileCommand(ProfilePersistenceFacade persistence, BusinessMessagePayloadMapper payloadMapper) {
        super(persistence, payloadMapper);
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return FIND_BY_ID;
    }

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#detachResultData(Context)
     */
    @Override
    public Optional<PrincipalProfile> detachedResult(Optional<PrincipalProfile> result) {
        return isNull(result) || result.isEmpty() ? Optional.empty() : Optional.of(payloadMapper.toPayload(result.get()));
    }

    /**
     * To get mapper for business-message-payload
     *
     * @return mapper instance
     * @see BusinessMessagePayloadMapper
     */
    @Override
    public BusinessMessagePayloadMapper getPayloadMapper() {
        return payloadMapper;
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * to get function to find entity by id
     *
     * @return function implementation
     */
    @Override
    protected LongFunction<Optional<PrincipalProfile>> functionFindById() {
        return persistence::findPrincipalProfileById;
    }
}
