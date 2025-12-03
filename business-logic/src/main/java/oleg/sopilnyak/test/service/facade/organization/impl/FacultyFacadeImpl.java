package oleg.sopilnyak.test.service.facade.organization.impl;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.type.organization.FacultyCommand.CommandId;

import oleg.sopilnyak.test.school.common.business.facade.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * Service-Facade: Service for manage organization in the school (groups of courses)
 *
 * @see OrganizationFacade
 * @see OrganizationFacadeImpl
 * @see Faculty
 * @see FacultyCommand
 */
@Slf4j
public class FacultyFacadeImpl extends OrganizationFacadeImpl<FacultyCommand<?>> implements FacultyFacade {
    // semantic data to payload converter
    private final UnaryOperator<Faculty> toPayload;

    public FacultyFacadeImpl(
            CommandsFactory<FacultyCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            ActionExecutor actionExecutor
    ) {
        super(factory, actionExecutor);
        this.toPayload = faculty -> faculty instanceof FacultyPayload ? faculty : mapper.toPayload(faculty);
    }

    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see Faculty
     */
    @Override
    public Collection<Faculty> findAllFaculties() {
        log.debug("Finding all faculties");
        final Optional<Set<Faculty>> result;
        result = actCommand(CommandId.FIND_ALL, factory, Input.empty());
        if (result.isPresent()) {
            final Set<Faculty> facultySet = result.get();
            log.debug("Found all faculties {}", facultySet);
            return facultySet.stream().map(toPayload).collect(Collectors.toSet());
        }
        return Set.of();
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
        log.debug("Finding faculty by ID:{}", id);
        final Optional<Optional<Faculty>> result;
        result = actCommand(CommandId.FIND_BY_ID, factory, Input.of(id));
        if (result.isPresent()) {
            final Optional<Faculty> faculty = result.get();
            log.debug("Found faculty {}", faculty);
            return faculty.map(toPayload);
        }
        return Optional.empty();
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
        log.debug("Creating or Updating faculty {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Faculty>> result;
        result = actCommand(CommandId.CREATE_OR_UPDATE, factory, input);
        if (result.isPresent()) {
            final Optional<Faculty> faculty = result.get();
            log.debug("Changed faculty {}", faculty);
            return faculty.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To delete faculty from the school
     *
     * @param id system-id of the faculty to delete
     * @throws FacultyNotFoundException   throws when faculty is not exists
     * @throws FacultyIsNotEmptyException throws when faculty has courses
     */
    @Override
    public void deleteFacultyById(Long id) throws FacultyNotFoundException, FacultyIsNotEmptyException {
        final String commandId = CommandId.DELETE;
        final Consumer<Exception> doThisOnError = exception -> {
            logSomethingWentWrong(exception, commandId);
            if (exception instanceof FacultyNotFoundException noFacultyException) {
                throw noFacultyException;
            } else if (exception instanceof FacultyIsNotEmptyException notEmptyException) {
                throw notEmptyException;
            } else if (nonNull(exception)) {
                ActionFacade.throwFor(commandId, exception);
            } else {
                failedButNoExceptionStored(commandId);
            }
        };
        log.debug("Deleting faculty with ID:{}", id);
        final Optional<Boolean> result = actCommand(commandId, factory, Input.of(id), doThisOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted faculty with ID:{} successfully:{} .", id, executionResult)
        );
    }

    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }

// private methods
}
