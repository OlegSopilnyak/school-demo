package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
import oleg.sopilnyak.test.service.command.id.set.FacultyCommands;

import java.util.Optional;

/**
 * Command-Implementation: command to update the faculty of the school
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateFacultyCommand implements OrganizationCommand<Optional<Faculty>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To create or update faculty instance
     *
     * @param parameter faculty instance to save
     * @return execution's result
     */
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
        return FacultyCommands.CREATE_OR_UPDATE;
    }
}
