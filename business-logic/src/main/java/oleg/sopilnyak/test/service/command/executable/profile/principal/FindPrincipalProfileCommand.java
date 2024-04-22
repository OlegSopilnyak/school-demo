package oleg.sopilnyak.test.service.command.executable.profile.principal;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.type.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.StudentProfileCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Command-Implementation: command to find student profile instance
 *
 * @see PrincipalProfileCommand
 * @see PrincipalProfile
 * @see ProfilePersistenceFacade
 * @see oleg.sopilnyak.test.persistence.sql.entity.PrincipalProfileEntity
 */
@Slf4j
@Component
public class FindPrincipalProfileCommand
        extends FindProfileCommand<Optional<PrincipalProfile>>
        implements PrincipalProfileCommand<Optional<PrincipalProfile>> {

    /**
     * Constructor
     *
     * @param persistenceFacade persistence facade instance
     */
    public FindPrincipalProfileCommand(ProfilePersistenceFacade persistenceFacade) {
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