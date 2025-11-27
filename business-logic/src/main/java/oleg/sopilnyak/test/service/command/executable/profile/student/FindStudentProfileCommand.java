package oleg.sopilnyak.test.service.command.executable.profile.student;

import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.function.LongFunction;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;


/**
 * Command-Implementation: command to find student profile instance
 *
 * @see StudentProfileCommand
 * @see StudentProfile
 * @see ProfilePersistenceFacade
 */
@Slf4j
@Component(StudentProfileCommand.Component.FIND_BY_ID)
public class FindStudentProfileCommand extends FindProfileCommand<StudentProfile>
        implements StudentProfileCommand<Optional<StudentProfile>> {

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.FIND_BY_ID;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CommandId.FIND_BY_ID;
    }

    public FindStudentProfileCommand(ProfilePersistenceFacade persistence, BusinessMessagePayloadMapper payloadMapper) {
        super(persistence, payloadMapper);
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
    protected LongFunction<Optional<StudentProfile>> functionFindById() {
        return persistence::findStudentProfileById;
    }
}
