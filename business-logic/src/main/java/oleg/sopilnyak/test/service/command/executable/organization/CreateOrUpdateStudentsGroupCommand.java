package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
import oleg.sopilnyak.test.service.command.id.set.StudentsGroupCommands;

import java.util.Optional;

/**
 * Command-Implementation: command to update the students group of the school
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateStudentsGroupCommand implements OrganizationCommand<Optional<StudentsGroup>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To create or update students group instance
     *
     * @param parameter students group instance to save
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<StudentsGroup>> execute(Object parameter) {
        try {
            log.debug("Trying to create or update students group {}", parameter);
            StudentsGroup instance = commandParameter(parameter);
            Optional<StudentsGroup> studentsGroup = persistenceFacade.save(instance);
            log.debug("Got stored students group {} from parameter {}", studentsGroup, instance);
            return CommandResult.<Optional<StudentsGroup>>builder()
                    .result(Optional.ofNullable(studentsGroup)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot create or update students group {}", parameter, e);
            return CommandResult.<Optional<StudentsGroup>>builder()
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
        return StudentsGroupCommands.CREATE_OR_UPDATE.id();
    }
}
