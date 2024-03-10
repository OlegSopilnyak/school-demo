package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to update student profile instance
 */
@Slf4j
@AllArgsConstructor
public class CreateStudentProfileCommand implements ProfileCommand<Optional<StudentProfile>> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To update principal's profile
     *
     * @param parameter system principal-profile instance
     * @return execution's result
     * @see Optional
     * @see PrincipalProfile
     */
    @Override
    public CommandResult<Optional<StudentProfile>> execute(Object parameter) {
        try {
            log.debug("Trying to update student profile {}", parameter);
            final StudentProfile input = commandParameter(parameter);
            final Optional<StudentProfile> profile = persistenceFacade.save(input);
            log.debug("Got saved \nstudent profile {}\n for input {}", profile, input);
            return CommandResult.<Optional<StudentProfile>>builder()
                    .result(Optional.of(profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot save find the profile {}", parameter, e);
            return CommandResult.<Optional<StudentProfile>>builder()
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
        return ProfileCommands.CREATE_OR_UPDATE_STUDENT;
    }
}
