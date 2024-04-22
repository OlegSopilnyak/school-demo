package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.FacultyCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to get faculty by id
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindFacultyCommand implements FacultyCommand<Optional<Faculty>> {
    private final FacultyPersistenceFacade persistenceFacade;

    /**
     * To find faculty by id
     *
     * @param parameter system faculty-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<Faculty>> execute(Object parameter) {
        try {
            log.debug("Trying to find faculty by ID:{}", parameter);
            Long id = commandParameter(parameter);
            Optional<Faculty> faculty = persistenceFacade.findFacultyById(id);
            log.debug("Got faculty {} by ID:{}", faculty, id);
            return CommandResult.<Optional<Faculty>>builder()
                    .result(Optional.ofNullable(faculty))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the faculty by ID:{}", parameter, e);
            return CommandResult.<Optional<Faculty>>builder()
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
        return FIND_BY_ID;
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
