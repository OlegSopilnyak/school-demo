package oleg.sopilnyak.test.service.command.executable.profile.student;

import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to delete student profile instance by id
 *
 * @see StudentProfileCommand
 * @see oleg.sopilnyak.test.school.common.model.StudentProfile
 * @see ProfilePersistenceFacade
 */
@Slf4j
@Component(StudentProfileCommand.Component.DELETE_BY_ID)
public class DeleteStudentProfileCommand extends DeleteProfileCommand<StudentProfile>
        implements StudentProfileCommand<Boolean> {

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.DELETE_BY_ID;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return StudentProfileFacade.DELETE_BY_ID;
    }

    /**
     * Constructor
     *
     * @param persistence persistence facade instance
     */
    public DeleteStudentProfileCommand(final ProfilePersistenceFacade persistence,
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
