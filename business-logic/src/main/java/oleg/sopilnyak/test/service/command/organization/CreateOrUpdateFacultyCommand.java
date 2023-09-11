package oleg.sopilnyak.test.service.command.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.organization.entity.FacultyCommandFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to update the faculty of the school
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateFacultyCommand implements SchoolCommand<Optional<Faculty>> {
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
            Faculty instance = (Faculty) parameter;
            Optional<Faculty> faculty = persistenceFacade.saveFaculty(instance);
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
        return FacultyCommandFacade.CREATE_OR_UPDATE;
    }
}
