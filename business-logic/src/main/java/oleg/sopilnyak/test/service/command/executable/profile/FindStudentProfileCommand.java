package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to get student profile by id
 */
@Slf4j
@AllArgsConstructor
public class FindStudentProfileCommand implements ProfileCommand<Optional<StudentProfile>> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To find student's profile by id
     *
     * @param parameter system profile-id
     * @return execution's result
     * @see Optional
     * @see StudentProfile
     */
    @Override
    public CommandResult<Optional<StudentProfile>> execute(Object parameter) {
        try {
            log.debug("Trying to find student profile by ID:{}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<StudentProfile> profile = persistenceFacade.findStudentProfileById(id);
            log.debug("Got student profile {} by ID:{}", profile, id);
            return CommandResult.<Optional<StudentProfile>>builder()
                    .result(Optional.of(profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the profile by ID:{}", parameter, e);
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
        return ProfileCommands.FIND_STUDENT_BY_ID.toString();
    }
}
