package oleg.sopilnyak.test.service.command.executable.education.student;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

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
@Component("studentMacroDelete")
public class DeleteStudentMacroCommand extends ParallelMacroCommand<Boolean> implements MacroDeleteStudent<Boolean> {
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // persistence facade for get instance of student by student-id
    private final transient StudentsPersistenceFacade persistence;
    // reference to current command for transactional operations
    private final AtomicReference<MacroDeleteStudent<Boolean>> self;

    /**
     * Reference to the current command for transactional operations create context processing
     *
     * @return the reference to the current command from spring beans factory
     * @see org.springframework.context.ApplicationContext
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     * @see this#createStudentProfileContext(StudentProfileCommand, Long)
     */
    @Override
    @SuppressWarnings("unchecked")
    public MacroDeleteStudent<Boolean> self() {
        synchronized (StudentCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo/executeUndo methods
                self.getAndSet(applicationContext.getBean("studentMacroDelete", MacroDeleteStudent.class));
            }
        }
        return self.get();
    }

    public DeleteStudentMacroCommand(@Qualifier("studentDelete") StudentCommand<?> personCommand,
                                     @Qualifier("profileStudentDelete") StudentProfileCommand<?> profileCommand,
                                     @Qualifier("parallelCommandNestedCommandsExecutor") SchedulingTaskExecutor executor,
                                     final StudentsPersistenceFacade persistence,
                                     final ActionExecutor actionExecutor) {
        super(actionExecutor, executor);
        this.persistence = persistence;
        this.self = new AtomicReference<>(null);
        super.putToNest(personCommand);
        super.putToNest(profileCommand);
    }

    /**
     * To get final main do-command result from nested command-contexts
     *
     * @param contexts nested command-contexts
     * @return the command result's value
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#afterExecutionProcessing(Context, Deque, Deque, Deque)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Boolean finalCommandResult(Deque<Context<?>> contexts) {
        return contexts.stream()
                .map(nested -> ((Context<Boolean>) nested).getResult().orElse(false))
                .reduce(Boolean.TRUE, Boolean::logicalAnd);
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
     * To get mapper for business-message-payload
     *
     * @return mapper instance
     * @see BusinessMessagePayloadMapper
     */
    @Override
    public BusinessMessagePayloadMapper getPayloadMapper() {
        return null;
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <N>                 type of delete student profile command result
     * @return built context of the command for input parameter
     * @see Input
     * @see Student
     * @see StudentProfileCommand
     * @see DeleteStudentMacroCommand#createStudentProfileContext(StudentProfileCommand, Long)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final StudentProfileCommand<N> command, final Input<?> macroInputParameter) {
        return macroInputParameter.value() instanceof Long studentId && StudentProfileCommand.DELETE_BY_ID.equals(command.getId()) ?
                self().createStudentProfileContext(command, studentId) : cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for delete student profile command
     *
     * @param command   delete student profile command instance
     * @param studentId related student-id value
     * @param <N>       type of delete student profile command result
     * @return built context of the command for input parameter
     * @see StudentsPersistenceFacade#findStudentById(Long)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public <N> Context<N> createStudentProfileContext(StudentProfileCommand<N> command, Long studentId) {
        final Long profileId = persistence.findStudentById(studentId).map(Student::getProfileId)
                .orElseThrow(() -> new StudentNotFoundException(STUDENT_WITH_ID_PREFIX + studentId + " is not exists."));
        return command.createContext(Input.of(profileId));
    }

    // for command activities as nested command

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command execute input parameter
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareNestedContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see oleg.sopilnyak.test.service.command.type.base.CompositeCommand#createContext(Input)
     */
    @Override
    public Context<Boolean> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return MacroDeleteStudent.super.acceptPreparedContext(visitor, macroInputParameter);
    }

    // private methods
    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

}
