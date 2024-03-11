package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.id.set.ProfileCommands.*;

/**
 * Service: To process commands for school's person profiles facade
 */
@Slf4j
@AllArgsConstructor
public class PersonProfileFacadeImpl<T> implements PersonProfileFacade {
    private final CommandsFactory<T> factory;

    /**
     * To get the person's profile by ID
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see PersonProfile#getId()
     * @see ProfileCommands
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> findById(Long id) {
        return executeTheCommand(FIND_BY_ID, id, factory);
    }

    /**
     * To create person-profile
     *
     * @param profile instance to create
     * @return created instance or Optional#empty()
     * @see PersonProfile
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> createOrUpdatePersonProfile(PersonProfile profile) {
        return executeTheCommand(CREATE_OR_UPDATE, profile, factory);
    }

    /**
     * To delete profile by system-id
     *
     * @param id value of system-id
     * @throws ProfileNotExistsException throws if the profile with system-id does not exist in the database
     */
    @Override
    public void deleteProfileById(Long id) throws ProfileNotExistsException {
        final String commandId = DELETE_BY_ID.toString();
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> commandExecutionResult = command.execute(id);
        if (!commandExecutionResult.isSuccess()) {
            final Exception executionException = commandExecutionResult.getException();
            log.warn("Something went wrong", executionException);
            if (executionException instanceof ProfileNotExistsException exception) {
                throw exception;
            } else if (nonNull(executionException)) {
                throwFor(commandId, executionException);
            } else {
                log.error("For command-id:'{}' there is not exception after wrong command execution.", commandId);
                throwFor(commandId, new NullPointerException("Exception is not stored!!!"));
            }
        }
    }

    private static <T> T executeTheCommand(ProfileCommands command, Object option, CommandsFactory<?> factory) {
        return executeSimpleCommand(command.toString(), option, factory);
    }
}
