package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to get principal's profile by id
 */
@Slf4j
@AllArgsConstructor
public class FindPrincipalProfileCommand implements ProfileCommand<Optional<PrincipalProfile>> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To find principal's profile by id
     *
     * @param parameter system profile-id
     * @return execution's result
     * @see Optional
     * @see PrincipalProfile
     */
    @Override
    public CommandResult<Optional<PrincipalProfile>> execute(Object parameter) {
        try {
            log.debug("Trying to find principal profile by ID:{}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<PrincipalProfile> profile = persistenceFacade.findPrincipalProfileById(id);
            log.debug("Got principal profile {} by ID:{}", profile, id);
            return CommandResult.<Optional<PrincipalProfile>>builder()
                    .result(Optional.of(profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the profile by ID:{}", parameter, e);
            return CommandResult.<Optional<PrincipalProfile>>builder()
                    .result(Optional.of(Optional.empty()))
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
        return ProfileCommands.FIND_PRINCIPAL_BY_ID.toString();
    }
}
