package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
import oleg.sopilnyak.test.service.command.id.set.FacultyCommands;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
public class FindAllFacultiesCommand implements OrganizationCommand<Set<Faculty>> {
    private final OrganizationPersistenceFacade persistenceFacade;

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
        return FacultyCommands.FIND_ALL.id();
    }
}
