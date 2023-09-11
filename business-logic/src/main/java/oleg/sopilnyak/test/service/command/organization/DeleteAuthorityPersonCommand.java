package oleg.sopilnyak.test.service.command.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonIsNotExistsException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.organization.entity.AuthorityPersonCommandFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to delete the authority person by id
 */
@Slf4j
@AllArgsConstructor
public class DeleteAuthorityPersonCommand implements SchoolCommand<Boolean> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To delete authority person by id
     *
     * @param parameter system authority-person-id
     * @return execution's result
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete authority person with ID: {}", parameter);
            Long id = (Long) parameter;
            Optional<AuthorityPerson> person = persistenceFacade.findAuthorityPersonById(id);
            if (person.isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new AuthorityPersonIsNotExistsException("AuthorityPerson with ID:" + id + " is not exists."))
                        .success(false).build();
            } else if (!person.get().getFaculties().isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new AuthorityPersonManageFacultyException("AuthorityPerson with ID:" + id + " is managing faculties."))
                        .success(false).build();
            }

            persistenceFacade.deleteAuthorityPerson(id);

            log.debug("Deleted authority person {} {}", person.get(), true);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(true))
                    .success(true)
                    .build();
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
        return AuthorityPersonCommandFacade.DELETE;
    }
}
