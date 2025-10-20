package oleg.sopilnyak.test.service.command.executable.education.student;

import java.util.Deque;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.LegacyParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to delete the student and it's profile
 *
 * @see Student
 * @see StudentProfile
 * @see LegacyParallelMacroCommand
 * @see DeleteStudentCommand
 * @see DeleteStudentProfileCommand
 * @see StudentsPersistenceFacade
 */
@Slf4j
@Component("studentMacroDelete")
public class DeleteStudentMacroCommand extends LegacyParallelMacroCommand<Boolean> implements StudentCommand<Boolean> {
    // executor of parallel nested commands
    private final SchedulingTaskExecutor executor;
    // persistence facade for get instance of student by student-id
    private final transient StudentsPersistenceFacade persistence;

    public DeleteStudentMacroCommand(@Qualifier("studentDelete") StudentCommand<?> personCommand,
                                     @Qualifier("profileStudentDelete") StudentProfileCommand<?> profileCommand,
                                     @Qualifier("parallelCommandNestedCommandsExecutor") SchedulingTaskExecutor executor,
                                     final StudentsPersistenceFacade persistence,
                                     final ActionExecutor actionExecutor) {
        super(actionExecutor);
        this.executor = executor;
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
    @SuppressWarnings("unchecked")
    @Override
    public Boolean finalCommandResult(Deque<Context<?>> contexts) {
        return contexts.stream()
                .map(nested -> ((Context<Boolean>) nested).getResult().orElse(false))
                .reduce(Boolean.TRUE, Boolean::logicalAnd);
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
                createStudentProfileContext(command, studentId) : cannotCreateNestedContextFor(command);
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
     * @see PrepareNestedContextVisitor#prepareContext(LegacyParallelMacroCommand, Input)
     * @see oleg.sopilnyak.test.service.command.type.base.CompositeCommand#createContext(Input)
     */
    @Override
    public Context<Boolean> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return StudentCommand.super.acceptPreparedContext(visitor, macroInputParameter);
    }

    // private methods
    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

}
