package oleg.sopilnyak.test.service.command.executable.profile.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.CreateOrUpdateProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.type.StudentProfileCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Command-Implementation: command to find student profile instance
 *
 * @see StudentProfileCommand
 * @see StudentProfile
 * @see ProfilePersistenceFacade
 * @see oleg.sopilnyak.test.persistence.sql.entity.StudentProfileEntity
 */
@Slf4j
@Component
public class FindStudentProfileCommand
        extends FindProfileCommand<Optional<StudentProfile>>
        implements StudentProfileCommand<Optional<StudentProfile>> {

    /**
     * Constructor
     *
     * @param persistenceFacade persistence facade instance
     */
    public FindStudentProfileCommand(ProfilePersistenceFacade persistenceFacade) {
        super(persistenceFacade);
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return FIND_BY_ID_COMMAND_ID;
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
}
