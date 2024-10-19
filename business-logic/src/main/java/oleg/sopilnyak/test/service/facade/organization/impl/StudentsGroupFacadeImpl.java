package oleg.sopilnyak.test.service.facade.organization.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupIsNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentsGroupPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand.*;

/**
 * Service-Facade: Service for manage organization in the school (school students groups)
 *
 * @see OrganizationFacade
 * @see StudentsGroup
 */
@Slf4j
public class StudentsGroupFacadeImpl extends OrganizationFacadeImpl<StudentsGroupCommand> implements StudentsGroupFacade {
    private final BusinessMessagePayloadMapper mapper;
    // semantic data to payload converter
    private final UnaryOperator<StudentsGroup> convert;

    public StudentsGroupFacadeImpl(final CommandsFactory<StudentsGroupCommand> factory,
                                   final BusinessMessagePayloadMapper mapper) {
        super(factory);
        this.mapper = mapper;
        this.convert = group -> group instanceof StudentsGroupPayload ? group : this.mapper.toPayload(group);
    }

    /**
     * To get all students groups
     *
     * @return list of students groups
     * @see StudentsGroup
     */
    @Override
    public Collection<StudentsGroup> findAllStudentsGroups() {
        log.debug("Find all students groups");
        final Collection<StudentsGroup> result = doSimpleCommand(FIND_ALL, null, factory);
        log.debug("Found all students groups {}", result);
        return result.stream().map(convert).toList();
    }

    /**
     * To get the students group by ID
     *
     * @param id system-id of the students group
     * @return StudentsGroup instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> findStudentsGroupById(Long id) {
        log.debug("Find students group by ID:{}", id);
        final Optional<StudentsGroup> result = doSimpleCommand(FIND_BY_ID, id, factory);
        log.debug("Found students group {}", result);
        return result.map(convert);
    }

    /**
     * To create or update students group instance
     *
     * @param instance students group should be created or updated
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> createOrUpdateStudentsGroup(StudentsGroup instance) {
        log.debug("Create or Update students group {}", instance);
        final Optional<StudentsGroup> result = doSimpleCommand(CREATE_OR_UPDATE, convert.apply(instance), factory);
        log.debug("Changed students group {}", result);
        return result.map(convert);
    }

    /**
     * To delete students group instance from the school
     *
     * @param id system-id of the students group to delete
     * @throws StudentsGroupIsNotFoundException    throws when students group is not exists
     * @throws StudentGroupWithStudentsException throws when students group has students
     */
    @Override
    public void deleteStudentsGroupById(Long id) throws StudentsGroupIsNotFoundException, StudentGroupWithStudentsException {
        log.debug("Delete students group with ID:{}", id);
        final String commandId = DELETE;
        final RootCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Deleted authority person with ID:{} successfully.", id);
            return;
        }

        // fail processing
        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof StudentsGroupIsNotFoundException noGroupException) {
            throw noGroupException;
        } else if (doException instanceof StudentGroupWithStudentsException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
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
