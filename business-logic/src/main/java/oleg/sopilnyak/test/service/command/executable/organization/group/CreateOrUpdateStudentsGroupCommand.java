package oleg.sopilnyak.test.service.command.executable.organization.group;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentsGroupCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to update the students group of the school
 */
@Slf4j
@AllArgsConstructor
@Component
public class CreateOrUpdateStudentsGroupCommand implements StudentsGroupCommand<Optional<StudentsGroup>> {
    private final StudentsGroupPersistenceFacade persistenceFacade;

    /**
     * To create or update students group instance
     *
     * @param parameter students group instance to save
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
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
