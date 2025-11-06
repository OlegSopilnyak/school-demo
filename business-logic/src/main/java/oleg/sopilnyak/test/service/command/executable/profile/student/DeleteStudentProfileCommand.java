package oleg.sopilnyak.test.service.command.executable.profile.student;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.DeleteProfileCommand;
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
 * Command-Implementation: command to delete student profile instance by id
 *
 * @see StudentProfileCommand
 * @see oleg.sopilnyak.test.school.common.model.StudentProfile
 * @see ProfilePersistenceFacade
 */
@Slf4j
@Component("profileStudentDelete")
public class DeleteStudentProfileCommand extends DeleteProfileCommand<StudentProfile>
        implements StudentProfileCommand<Boolean> {
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<StudentProfileCommand<Boolean>> self = new AtomicReference<>(null);

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
    public StudentProfileCommand<Boolean> self() {
        synchronized (StudentProfileCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo method
                self.getAndSet(applicationContext.getBean("profileStudentDelete", StudentProfileCommand.class));
            }
        }
        return self.get();
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
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return DELETE_BY_ID;
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
