package oleg.sopilnyak.test.service.command.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
public class FindAllFacultiesCommand implements SchoolCommand<Set<Faculty>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     */
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
            return CommandResult.<Set<Faculty>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
