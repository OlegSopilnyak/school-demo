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
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindAllFacultiesCommand implements FacultyCommand<Set<Faculty>> {
    private final FacultyPersistenceFacade persistenceFacade;

    /**
     * To execute command's business-logic
     *
     * @param parameter not used
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Set<Faculty>> execute(Object parameter) {
        try {
            log.debug("Trying to get all faculties");
            final Set<Faculty> faculties = persistenceFacade.findAllFaculties();
            log.debug("Got faculties {}", faculties);
            return CommandResult.<Set<Faculty>>builder()
                    .result(Optional.ofNullable(faculties))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find any faculty", e);
            return CommandResult.<Set<Faculty>>builder()
                    .result(Optional.of(Set.of()))
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
        return FIND_ALL;
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
