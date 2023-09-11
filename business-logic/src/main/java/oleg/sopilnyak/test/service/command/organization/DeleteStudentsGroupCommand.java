package oleg.sopilnyak.test.service.command.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.StudentsGroupNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.organization.entity.StudentsGroupCommandFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to delete the students group of the school by id
 */
@Slf4j
@AllArgsConstructor
public class DeleteStudentsGroupCommand implements SchoolCommand<Boolean> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To delete students group by id
     *
     * @param parameter system students-group-id
     * @return execution's result
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete students group with ID: {}", parameter);
            Long id = (Long) parameter;
            Optional<StudentsGroup> group = persistenceFacade.findStudentsGroupById(id);
            if (group.isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new StudentsGroupNotExistsException("StudentsGroup with ID:" + id + " is not exists."))
                        .success(false).build();
            } else if (!group.get().getStudents().isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new StudentGroupWithStudentsException("StudentsGroup with ID:" + id + " has students."))
                        .success(false).build();
            }

            persistenceFacade.deleteStudentsGroup(id);

            log.debug("Deleted students group {} {}", group.get(), true);
            return CommandResult.<Boolean>builder().result(Optional.of(true)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot delete the students group by ID:{}", parameter, e);
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
        return StudentsGroupCommandFacade.DELETE;
    }
}
