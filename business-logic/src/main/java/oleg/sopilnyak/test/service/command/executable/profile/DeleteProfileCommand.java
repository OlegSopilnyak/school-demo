package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to delete person profile instance by id
 */
@Slf4j
@AllArgsConstructor
public class DeleteProfileCommand implements ProfileCommand<Boolean> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To delete person's profile by id
     *
     * @param parameter system-id of person-profile to delete
     * @return execution's result
     * @see PersonProfile
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete person profile {}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
            if (profile.isEmpty()) {
                log.debug("Person profile with ID:{} is not exists.", id);
                return CommandResult.<Boolean>builder().result(Optional.of(false))
                        .exception(new ProfileNotExistsException("Profile with ID:" + id + " is not exists."))
                        .success(false).build();
            }
            final boolean deleted = persistenceFacade.deleteProfileById(id);
            log.debug("Person profile with ID:{} is deleted '{}'", id, deleted);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(deleted))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot delete the person profile {}", parameter, e);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(false))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return ProfileCommands.DELETE_BY_ID.id();
    }
}
