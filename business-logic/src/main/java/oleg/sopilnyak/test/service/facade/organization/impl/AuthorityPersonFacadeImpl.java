package oleg.sopilnyak.test.service.facade.organization.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.AuthorityPersonPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
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
public class AuthorityPersonFacadeImpl extends OrganizationFacadeImpl<AuthorityPersonCommand> implements AuthorityPersonFacade {
    private final BusinessMessagePayloadMapper payloadMapper;
    // semantic payload mapper
    private final UnaryOperator<AuthorityPerson> mapToPayload;

    public AuthorityPersonFacadeImpl(final CommandsFactory<AuthorityPersonCommand> factory,
                                     final BusinessMessagePayloadMapper payloadMapper) {
        super(factory);
        this.payloadMapper = payloadMapper;
        mapToPayload = person -> person instanceof AuthorityPersonPayload ? person : this.payloadMapper.toPayload(person);
    }

    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     * @see AuthorityPersonPayload
     * @see AuthorityPersonCommand#FIND_ALL
     */
    @Override
    public Collection<AuthorityPerson> findAllAuthorityPersons() {
        log.debug("Find all authority persons");
        final Collection<AuthorityPerson> result =
                doSimpleCommand(AuthorityPersonCommand.FIND_ALL, null, factory);
        log.debug("Found all authority persons {}", result);
        return result.stream().map(mapToPayload).toList();
    }

    /**
     * To get the authorityPerson by ID
     *
     * @param id system-id of the authorityPerson
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see AuthorityPersonPayload
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> findAuthorityPersonById(Long id) {
        log.debug("Find authority person by ID:{}", id);
        final Optional<AuthorityPerson> result =
                doSimpleCommand(AuthorityPersonCommand.FIND_BY_ID, id, factory);
        log.debug("Found authority person {}", result);
        return result.map(mapToPayload);
    }

    /**
     * To create or update authorityPerson instance
     *
     * @param instance authorityPerson should be created or updated
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see AuthorityPersonPayload
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> createOrUpdateAuthorityPerson(AuthorityPerson instance) {
        log.debug("Create or Update authority person {}", instance);
        final Optional<AuthorityPerson> result =
                doSimpleCommand(AuthorityPersonCommand.CREATE_OR_UPDATE, mapToPayload.apply(instance), factory);
        log.debug("Changed authority person {}", result);
        return result.map(mapToPayload);
    }

    /**
     * To delete authorityPerson from the school
     *
     * @param id system-id of the authorityPerson to delete
     * @throws NotExistAuthorityPersonException      throws when authorityPerson is not exists
     * @throws AuthorityPersonManageFacultyException throws when authorityPerson takes place in a faculty as a dean
     */
    @Override
    public void deleteAuthorityPersonById(Long id) throws NotExistAuthorityPersonException, AuthorityPersonManageFacultyException {
        log.debug("Delete authority person with ID:{}", id);
        final String commandId = AuthorityPersonCommand.DELETE;
        final SchoolCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted authority person with ID:{} successfully.", id);
            return;
        }

        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof NotExistAuthorityPersonException exception) {
            throw exception;
        } else if (doException instanceof AuthorityPersonManageFacultyException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
        } else {
            log.error(WRONG_COMMAND_EXECUTION, commandId);
            throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
        }
    }
}
