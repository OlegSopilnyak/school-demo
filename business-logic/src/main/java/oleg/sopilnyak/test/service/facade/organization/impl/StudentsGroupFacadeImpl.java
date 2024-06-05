package oleg.sopilnyak.test.service.facade.organization.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentsGroupException;
import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentsGroupPayload;

import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service-Facade: Service for manage organization in the school (school students groups)
 *
 * @see OrganizationFacade
 * @see StudentsGroup
 */
@Slf4j
public class StudentsGroupFacadeImpl extends OrganizationFacadeImpl implements StudentsGroupFacade {
    private final BusinessMessagePayloadMapper payloadMapper;

    public StudentsGroupFacadeImpl(final CommandsFactory<StudentsGroupCommand> factory,
                                   final BusinessMessagePayloadMapper payloadMapper) {
        super(factory);
        this.payloadMapper = payloadMapper;
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
        final String commandId = StudentsGroupCommand.FIND_ALL;
        final Collection<StudentsGroup> result = doSimpleCommand(commandId, null, factory);
        log.debug("Found all students groups {}", result);
        return result.stream().map(payloadMapper::toPayload).map(StudentsGroup.class::cast).toList();
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
        final String commandId = StudentsGroupCommand.FIND_BY_ID;
        final Optional<StudentsGroup> result = doSimpleCommand(commandId, id, factory);
        log.debug("Found students group {}", result);
        return result.map(payloadMapper::toPayload);
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
        final String commandId = StudentsGroupCommand.CREATE_OR_UPDATE;
        final StudentsGroupPayload payload = payloadMapper.toPayload(instance);
        final Optional<StudentsGroup> result = doSimpleCommand(commandId, payload, factory);
        log.debug("Changed students group {}", result);
        return result.map(payloadMapper::toPayload);
    }

    /**
     * To delete students group instance from the school
     *
     * @param id system-id of the students group to delete
     * @throws NotExistStudentsGroupException    throws when students group is not exists
     * @throws StudentGroupWithStudentsException throws when students group has students
     */
    @Override
    public void deleteStudentsGroupById(Long id) throws NotExistStudentsGroupException, StudentGroupWithStudentsException {
        log.debug("Delete students group with ID:{}", id);
        String commandId = StudentsGroupCommand.DELETE;
        final SchoolCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted authority person with ID:{} successfully.", id);
            return;
        }

        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof NotExistStudentsGroupException exception) {
            throw exception;
        } else if (doException instanceof StudentGroupWithStudentsException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
        } else {
            log.error(WRONG_COMMAND_EXECUTION, commandId);
            throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
        }
    }
}
