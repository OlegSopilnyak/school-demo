package oleg.sopilnyak.test.service.command.executable.education.student;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
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

import java.util.Deque;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Component(StudentCommand.Component.DELETE_ALL)
public class DeleteStudentMacroCommand extends ParallelMacroCommand<Boolean> implements MacroDeleteStudent<Boolean> {
    // persistence facade for get instance of student by student-id
    private final transient StudentsPersistenceFacade persistence;

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command
     * @see RootCommand#self()
     * @see this#prepareContext(StudentProfileCommand, Input)
     * @see MacroDeleteStudent#createStudentProfileContext(StudentProfileCommand, Long)
     */
    @Override
    public MacroDeleteStudent<Boolean> self() {
        return (MacroDeleteStudent<Boolean>) super.self();
    }

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.DELETE_ALL;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CommandId.DELETE_ALL;
    }

    public DeleteStudentMacroCommand(
            @Qualifier(StudentCommand.Component.DELETE) StudentCommand<?> personCommand,
            @Qualifier(StudentProfileCommand.Component.DELETE_BY_ID) StudentProfileCommand<?> profileCommand,
            @Qualifier("parallelCommandNestedCommandsExecutor") SchedulingTaskExecutor executor,
            final StudentsPersistenceFacade persistence,
            final ActionExecutor actionExecutor
    ) {
        super(actionExecutor, executor);
        this.persistence = persistence;
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
    @Override
    public Boolean finalCommandResult(Deque<Context<?>> contexts) {
        return contexts.stream()
                .map(context -> context.getResult().map(Boolean.class::cast).orElse(false))
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
     * To prepare context for particular type of the nested command
     *
     * @param command             nested command instance
     * @param mainInput macro-command input parameter
     * @param <N>                 type of delete student profile command result
     * @return built context of the command for input parameter
     * @see Input
     * @see Student
     * @see StudentProfileCommand
     * @see DeleteStudentMacroCommand#createStudentProfileContext(StudentProfileCommand, Long)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final StudentProfileCommand<N> command, final Input<?> mainInput) {
        return mainInput.value() instanceof Long studentId
                &&
                StudentProfileCommand.DELETE_BY_ID.equals(command.getId())
                ?
                self().createStudentProfileContext(command, studentId)
                :
                cannotCreateNestedContextFor(command);
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
    public <N> Context<N> createStudentProfileContext(final StudentProfileCommand<N> command, final Long studentId) {
        final Long profileId = persistence.findStudentById(studentId).map(Student::getProfileId)
                .orElseThrow(() -> exceptionFor(studentId));
        return command.createContext(Input.of(profileId));
    }

    //
    // for command activities as nested command
    //

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
    private static <T> Context<T> cannotCreateNestedContextFor(final RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

    private EntityNotFoundException exceptionFor(final Long id) {
        return new StudentNotFoundException(STUDENT_WITH_ID_PREFIX + id + " is not exists.");
    }

}
