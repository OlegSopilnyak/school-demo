package oleg.sopilnyak.test.service.command.executable.profile.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
import oleg.sopilnyak.test.service.command.type.StudentProfileCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to delete student profile instance by id
 *
 * @see StudentProfileCommand
 * @see oleg.sopilnyak.test.school.common.model.StudentProfile
 * @see ProfilePersistenceFacade
 * @see oleg.sopilnyak.test.persistence.sql.entity.StudentProfileEntity
 */
@Slf4j
@Component
public class DeleteStudentProfileCommand
        extends DeleteProfileCommand<Boolean>
        implements StudentProfileCommand<Boolean> {
    /**
     * Constructor
     *
     * @param persistenceFacade persistence facade instance
     */
    public DeleteStudentProfileCommand(ProfilePersistenceFacade persistenceFacade) {
        super(persistenceFacade);
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
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return DELETE_BY_ID_COMMAND_ID;
    }
}
