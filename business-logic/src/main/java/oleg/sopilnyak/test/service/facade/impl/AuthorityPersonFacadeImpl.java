package oleg.sopilnyak.test.service.facade.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.school.common.business.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonIsNotExistsException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Collection;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;


/**
 * Service-Facade: Service for manage organization in the school (authority persons)
 *
 * @see OrganizationFacade
 * @see OrganizationFacadeImpl
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 */
@Slf4j
public class AuthorityPersonFacadeImpl extends OrganizationFacadeImpl implements AuthorityPersonFacade {

    public AuthorityPersonFacadeImpl(CommandsFactory<?> factory) {
        super(factory);
    }

    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     */
    @Override
    public Collection<AuthorityPerson> findAllAuthorityPersons() {
        return executeSimpleCommand(AuthorityPersonCommand.FIND_ALL, null, factory);
    }

    /**
     * To get the authorityPerson by ID
     *
     * @param id system-id of the authorityPerson
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> getAuthorityPersonById(Long id) {
        return executeSimpleCommand(AuthorityPersonCommand.FIND_BY_ID, id, factory);
    }

    /**
     * To create or update authorityPerson instance
     *
     * @param instance authorityPerson should be created or updated
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> createOrUpdateAuthorityPerson(AuthorityPerson instance) {
        return executeSimpleCommand(AuthorityPersonCommand.CREATE_OR_UPDATE, instance, factory);
    }

    /**
     * To delete authorityPerson from the school
     *
     * @param id system-id of the authorityPerson to delete
     * @throws AuthorityPersonIsNotExistsException   throws when authorityPerson is not exists
     * @throws AuthorityPersonManageFacultyException throws when authorityPerson takes place in a faculty as a dean
     */
    @Override
    public void deleteAuthorityPersonById(Long id) throws AuthorityPersonIsNotExistsException, AuthorityPersonManageFacultyException {
        String commandId = AuthorityPersonCommand.DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> cmdResult = command.execute(id);
        if (!cmdResult.isSuccess()) {
            final Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof AuthorityPersonIsNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof AuthorityPersonManageFacultyException exception) {
                throw exception;
            } else {
                throwFor(commandId, cmdResult.getException());
            }
        }
    }
}
