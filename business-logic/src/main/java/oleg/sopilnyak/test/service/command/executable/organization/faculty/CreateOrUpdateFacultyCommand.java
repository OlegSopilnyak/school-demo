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
 * Command-Implementation: command to update the faculty of the school
 */
@Slf4j
@AllArgsConstructor
@Component
public class CreateOrUpdateFacultyCommand implements FacultyCommand<Optional<Faculty>> {
    private final FacultyPersistenceFacade persistenceFacade;

    /**
     * To create or update faculty instance
     *
     * @param parameter faculty instance to save
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<Faculty>> execute(Object parameter) {
        try {
            log.debug("Trying to create or update faculty {}", parameter);
            Faculty instance = commandParameter(parameter);
            Optional<Faculty> faculty = persistenceFacade.save(instance);
            log.debug("Got stored faculty {} from parameter {}", faculty, instance);
            return CommandResult.<Optional<Faculty>>builder()
                    .result(Optional.ofNullable(faculty)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot create or update faculty {}", parameter, e);
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
        return CREATE_OR_UPDATE;
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
