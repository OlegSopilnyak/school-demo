package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.facade.organization.entity.StudentsGroupCommandFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to get students group by id
 */
@Slf4j
@AllArgsConstructor
public class FindStudentsGroupCommand implements SchoolCommand<Optional<StudentsGroup>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To find students group by id
     *
     * @param parameter system students-group-id
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<StudentsGroup>> execute(Object parameter) {
        try {
            log.debug("Trying to find students group by ID:{}", parameter);
            Long id = (Long) parameter;
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
        return StudentsGroupCommandFacade.FIND_BY_ID;
    }
}
