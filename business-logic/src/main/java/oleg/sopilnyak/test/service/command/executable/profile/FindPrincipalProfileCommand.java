package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.ProfileCommandFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to get faculty by id
 */
@Slf4j
@AllArgsConstructor
public class FindPrincipalProfileCommand implements ProfileCommand<Optional<PrincipalProfile>> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To find student's profile by id
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
            final Long id = (Long) parameter;
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
            final Optional<PrincipalProfile> concreteProfile;
            concreteProfile = profile.isPresent() && profile.get() instanceof PrincipalProfile entity ?
                    Optional.of(entity) : Optional.empty();

            log.debug("Got principal profile {} by ID:{}", concreteProfile, id);
            return CommandResult.<Optional<PrincipalProfile>>builder()
                    .result(Optional.of(concreteProfile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot student find the profile by ID:{}", parameter, e);
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
        return ProfileCommandFacade.FIND_PRINCIPAL_BY_ID;
    }
}
