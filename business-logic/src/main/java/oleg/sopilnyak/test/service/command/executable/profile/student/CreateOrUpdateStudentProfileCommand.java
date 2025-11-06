package oleg.sopilnyak.test.service.command.executable.profile.student;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.CreateOrUpdateProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;


/**
 * Command-Implementation: command to update student profile instance
 *
 * @see StudentProfileCommand
 * @see StudentProfile
 * @see ProfilePersistenceFacade
 * @see CreateOrUpdateStudentProfileCommand
 */
@Slf4j
@Component("profileStudentUpdate")
public class CreateOrUpdateStudentProfileCommand extends CreateOrUpdateProfileCommand<StudentProfile>
        implements StudentProfileCommand<Optional<StudentProfile>> {
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<StudentProfileCommand<Optional<StudentProfile>>> self = new AtomicReference<>(null);

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    @SuppressWarnings("unchecked")
    public StudentProfileCommand<Optional<StudentProfile>> self() {
        synchronized (StudentProfileCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo method
                self.getAndSet(applicationContext.getBean("profileStudentUpdate", StudentProfileCommand.class));
            }
        }
        return self.get();
    }

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
