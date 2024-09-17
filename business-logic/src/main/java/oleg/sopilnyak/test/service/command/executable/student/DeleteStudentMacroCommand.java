package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Command-Implementation: command to delete the student and it's profile
 *
 * @see Student
 * @see StudentProfile
 * @see ParallelMacroCommand
 * @see DeleteStudentCommand
 * @see DeleteStudentProfileCommand
 * @see StudentsPersistenceFacade
 */
@Slf4j
@Component
public class DeleteStudentMacroCommand extends ParallelMacroCommand implements StudentCommand {
    // executor of parallel nested commands
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    private final int maxPoolSize;
    // persistence facade for get instance of student by student-id
    private final StudentsPersistenceFacade persistence;

    public DeleteStudentMacroCommand(
            final DeleteStudentCommand deleteStudentCommand,
            final DeleteStudentProfileCommand deleteStudentProfileCommand,
            final StudentsPersistenceFacade persistence,
            @Value("${school.parallel.max.pool.size:100}") final int maxPoolSize
    ) {
        this.maxPoolSize = maxPoolSize;
        this.persistence = persistence;
        addToNest(deleteStudentCommand);
        addToNest(deleteStudentProfileCommand);
    }

    /**
     * To get access to command's command-context executor
     *
     * @return instance of executor
     */
    @Override
    public SchedulingTaskExecutor getExecutor() {
        return executor;
    }

    /**
     * To prepare and run nested commands runner executor
     *
     * @see ThreadPoolTaskExecutor#initialize()
     * @see MacroCommand#fromNest()
     */
    @PostConstruct
    public void runThreadPoolExecutor() {
        executor.setCorePoolSize(fromNest().size());
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(maxPoolSize);
        executor.initialize();
    }

    /**
     * To shut down nested commands runner executor
     *
     * @see ThreadPoolTaskExecutor#shutdown()
     */
    @PreDestroy
    public void stopThreadPoolExecutor() {
        executor.shutdown();
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
        return DELETE_ALL;
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of delete student profile command result
     * @return built context of the command for input parameter
     * @see Student
     * @see StudentProfileCommand
     * @see DeleteStudentMacroCommand#createStudentProfileContext(StudentProfileCommand, Long)
     * @see Context
     */
    @Override
    public <T> Context<T> prepareContext(@NonNull final StudentProfileCommand command, final Object mainInput) {
        return mainInput instanceof Long studentId && StudentProfileCommand.DELETE_BY_ID.equals(command.getId()) ?
                createStudentProfileContext(command, studentId) : cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for delete student profile command
     *
     * @param command   delete student profile command instance
     * @param studentId related student-id value
     * @param <T>       type of delete student profile command result
     * @return built context of the command for input parameter
     * @see StudentsPersistenceFacade#findStudentById(Long)
     */
    public <T> Context<T> createStudentProfileContext(StudentProfileCommand command, Long studentId) {
        final Long profileId = persistence.findStudentById(studentId)
                .orElseThrow(() -> new NotExistStudentException("Not exists student with ID: " + studentId))
                .getProfileId();
        return command.createContext(profileId);
    }

// for command activities as nested command

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Object)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    public <T> Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Object input) {
        return super.acceptPreparedContext(visitor, input);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see CompositeCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    public <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                      final Context<T> context, final Context.StateChangedListener<T> stateListener) {
        super.doAsNestedCommand(visitor, context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see CompositeCommand#undoCommand(Context)
     */
    @Override
    public <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                              final Context<T> context) {
        return super.undoAsNestedCommand(visitor, context);
    }

    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

}
