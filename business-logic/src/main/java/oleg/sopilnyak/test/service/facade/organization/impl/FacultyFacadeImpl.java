package oleg.sopilnyak.test.service.facade.organization.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.FacultyPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service-Facade: Service for manage organization in the school (groups of courses)
 *
 * @see OrganizationFacade
 * @see OrganizationFacadeImpl
 * @see Faculty
 * @see FacultyCommand
 */
@Slf4j
public class FacultyFacadeImpl extends OrganizationFacadeImpl<FacultyCommand> implements FacultyFacade {
    private final BusinessMessagePayloadMapper payloadMapper;
    // semantic payload mapper
    private final UnaryOperator<Faculty> mapToPayload;

    public FacultyFacadeImpl(final CommandsFactory<FacultyCommand> factory,
                             final BusinessMessagePayloadMapper payloadMapper) {
        super(factory);
        this.payloadMapper = payloadMapper;
        mapToPayload = faculty -> faculty instanceof FacultyPayload ? faculty : this.payloadMapper.toPayload(faculty);
    }

    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see Faculty
     */
    @Override
    public Collection<Faculty> findAllFaculties() {
        log.debug("Find all faculties");
        final Collection<Faculty> result = doSimpleCommand(FacultyCommand.FIND_ALL, null, factory);
        log.debug("Found all faculties {}", result);
        return result.stream().map(mapToPayload).toList();
    }

    /**
     * To get the faculty by ID
     *
     * @param id system-id of the faculty
     * @return Faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Faculty> findFacultyById(Long id) {
        log.debug("Find faculty by ID:{}", id);
        final Optional<Faculty> result = doSimpleCommand(FacultyCommand.FIND_BY_ID, id, factory);
        log.debug("Found faculty {}", result);
        return result.map(mapToPayload);
    }

    /**
     * To create or update faculty instance
     *
     * @param instance faculty should be created or updated
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Faculty> createOrUpdateFaculty(Faculty instance) {
        log.debug("Create or Update faculty {}", instance);
        final Optional<Faculty> result =
                doSimpleCommand(FacultyCommand.CREATE_OR_UPDATE, mapToPayload.apply(instance), factory);
        log.debug("Changed faculty {}", result);
        return result.map(payloadMapper::toPayload);
    }

    /**
     * To delete faculty from the school
     *
     * @param id system-id of the faculty to delete
     * @throws NotExistFacultyException   throws when faculty is not exists
     * @throws FacultyIsNotEmptyException throws when faculty has courses
     */
    @Override
    public void deleteFacultyById(Long id) throws NotExistFacultyException, FacultyIsNotEmptyException {
        log.debug("Delete faculty with ID:{}", id);
        final String commandId = FacultyCommand.DELETE;
        final SchoolCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted faculty with ID:{} successfully.", id);
            return;
        }

        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof NotExistFacultyException exception) {
            throw exception;
        } else if (doException instanceof FacultyIsNotEmptyException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
        } else {
            log.error(WRONG_COMMAND_EXECUTION, commandId);
            throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
        }
    }
}
