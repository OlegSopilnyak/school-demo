package oleg.sopilnyak.test.service.command.executable.organization.group;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentsGroupCommand;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to get students group by id
 */
@Slf4j
@AllArgsConstructor
public class FindStudentsGroupCommand implements StudentsGroupCommand<Optional<StudentsGroup>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To find students group by id
     *
     * @param parameter system students-group-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<StudentsGroup>> execute(Object parameter) {
        try {
            log.debug("Trying to find students group by ID:{}", parameter);
            Long id = commandParameter(parameter);
            Optional<StudentsGroup> studentsGroup = persistenceFacade.findStudentsGroupById(id);
            log.debug("Got students group {} by ID:{}", studentsGroup, id);
            return CommandResult.<Optional<StudentsGroup>>builder()
                    .result(Optional.ofNullable(studentsGroup))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the students group by ID:{}", parameter, e);
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
