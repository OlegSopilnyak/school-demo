package oleg.sopilnyak.test.service.command.executable.profile.student;

import static java.util.Objects.isNull;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.CreateOrUpdateProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;


/**
 * Command-Implementation: command to update student profile instance
 *
 * @see StudentProfileCommand
 * @see StudentProfile
 * @see ProfilePersistenceFacade
 * @see CreateOrUpdateStudentProfileCommand
 */
@Slf4j
@Component
public class CreateOrUpdateStudentProfileCommand extends CreateOrUpdateProfileCommand<StudentProfile>
        implements StudentProfileCommand<Optional<StudentProfile>> {

    /**
     * Constructor
     *
     * @param persistence facade of persistence layer
     */
    public CreateOrUpdateStudentProfileCommand(final ProfilePersistenceFacade persistence,
                                               final BusinessMessagePayloadMapper payloadMapper) {
        super(StudentProfile.class, persistence, payloadMapper);
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CREATE_OR_UPDATE;
    }

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#detachResultData(Context)
     */
    @Override
    public Optional<StudentProfile> detachedResult(Optional<StudentProfile> result) {
        return isNull(result) || result.isEmpty() ? Optional.empty() : Optional.of(payloadMapper.toPayload(result.get()));
    }

    /**
     * To get mapper for business-message-payload
     *
     * @return mapper instance
     * @see BusinessMessagePayloadMapper
     */
    @Override
    public BusinessMessagePayloadMapper getPayloadMapper() {
        return payloadMapper;
    }

    /**
     * to get function to find entity by id
     *
     * @return function implementation
     */
    @Override
    protected LongFunction<Optional<StudentProfile>> functionFindById() {
        return persistence::findStudentProfileById;
    }

    /**
     * to get function to copy the entity
     *
     * @return function implementation
     */
    @Override
    protected UnaryOperator<StudentProfile> functionAdoptEntity() {
        final UnaryOperator<StudentProfile> persistenceAdoption = persistence::toEntity;
        return profile -> payloadMapper.toPayload(persistenceAdoption.apply(profile));
    }

    /**
     * to get function to persist the entity
     *
     * @return function implementation
     */
    @Override
    protected Function<StudentProfile, Optional<StudentProfile>> functionSave() {
        return persistence::save;
    }
}
