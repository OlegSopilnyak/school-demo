package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.FacultyNotExistsException;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.FacultyCommand;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to delete the faculty of the school by id
 */
@Slf4j
@AllArgsConstructor
public class DeleteFacultyCommand implements FacultyCommand<Boolean> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To delete faculty by id
     *
     * @param parameter system faculty-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete faculty with ID: {}", parameter);
            Long id = commandParameter(parameter);
            Optional<Faculty> person = persistenceFacade.findFacultyById(id);
            if (person.isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new FacultyNotExistsException("Faculty with ID:" + id + " is not exists."))
                        .success(false).build();
            } else if (!person.get().getCourses().isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new FacultyIsNotEmptyException("Faculty with ID:" + id + " has courses."))
                        .success(false).build();
            }

            persistenceFacade.deleteFaculty(id);

            log.debug("Deleted faculty {} {}", person.get(), true);
            return CommandResult.<Boolean>builder().result(Optional.of(true)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot delete the authority person by ID:{}", parameter, e);
            return CommandResult.<Boolean>builder().result(Optional.empty())
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
        return DELETE;
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
