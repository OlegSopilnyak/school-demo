package oleg.sopilnyak.test.service.facade.organization.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.FacultyPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.organization.FacultyCommand.*;

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
    private final BusinessMessagePayloadMapper mapper;
    // semantic data to payload converter
    private final UnaryOperator<Faculty> convert;

    public FacultyFacadeImpl(final CommandsFactory<FacultyCommand> factory,
                             final BusinessMessagePayloadMapper mapper) {
        super(factory);
        this.mapper = mapper;
        this.convert = faculty -> faculty instanceof FacultyPayload ? faculty : this.mapper.toPayload(faculty);
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
        final Collection<Faculty> result = doSimpleCommand(FIND_ALL, null, factory);
        log.debug("Found all faculties {}", result);
        return result.stream().map(convert).toList();
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
        final Optional<Faculty> result = doSimpleCommand(FIND_BY_ID, id, factory);
        log.debug("Found faculty {}", result);
        return result.map(convert);
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
        final Optional<Faculty> result = doSimpleCommand(CREATE_OR_UPDATE, convert.apply(instance), factory);
        log.debug("Changed faculty {}", result);
        return result.map(convert);
    }

    /**
     * To delete faculty from the school
     *
     * @param id system-id of the faculty to delete
     * @throws FacultyIsNotFoundException   throws when faculty is not exists
     * @throws FacultyIsNotEmptyException throws when faculty has courses
     */
    @Override
    public void deleteFacultyById(Long id) throws FacultyIsNotFoundException, FacultyIsNotEmptyException {
        log.debug("Delete faculty with ID:{}", id);
        final String commandId = DELETE;
        final RootCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Deleted faculty with ID:{} successfully.", id);
            return;
        }

        // fail processing
        final Exception deleteException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, deleteException);
        if (deleteException instanceof FacultyIsNotFoundException noFacultyException) {
            throw noFacultyException;
        } else if (deleteException instanceof FacultyIsNotEmptyException exception) {
            throw exception;
        } else if (nonNull(deleteException)) {
            throwFor(commandId, deleteException);
        } else {
            wrongCommandExecution();
        }
    }

    // private methods
    private static void wrongCommandExecution() {
        log.error(WRONG_COMMAND_EXECUTION, DELETE);
        throwFor(DELETE, new NullPointerException(EXCEPTION_IS_NOT_STORED));
    }
}
