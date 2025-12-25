package oleg.sopilnyak.test.service.command.executable.education.student;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.Deque;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to create the student-person instance with related profile
 *
 * @see Student
 * @see StudentProfile
 * @see SequentialMacroCommand
 * @see StudentCommand
 */
@Slf4j
@Component(StudentCommand.Component.CREATE_NEW)
public class CreateStudentMacroCommand extends SequentialMacroCommand<Optional<Student>>
        implements StudentCommand<Optional<Student>> {
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;
    @Value("${school.mail.basic.domain:gmail.com}")
    private String emailDomain;

    /**
     * Reference to the current command for operations with the command's entities in transaction possibility<BR/>
     * Not needed transaction for this command
     *
     * @return the reference to the current command from spring beans factory
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    public StudentCommand<Optional<Student>> self() {
        return this;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CommandId.CREATE_NEW;
    }

    public CreateStudentMacroCommand(
            @Qualifier(Component.CREATE_OR_UPDATE) StudentCommand<?> personCommand,
            @Qualifier(StudentProfileCommand.Component.CREATE_OR_UPDATE) StudentProfileCommand<?> profileCommand,
            final BusinessMessagePayloadMapper payloadMapper,
            final ActionExecutor actionExecutor
    ) {
        super(actionExecutor);
        this.payloadMapper = payloadMapper;
        putToNest(profileCommand);
        putToNest(personCommand);
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
    public Optional<Student> finalCommandResult(final Deque<Context<?>> contexts) {
        return contexts.stream().filter(CreateStudentMacroCommand::hasPerson)
                .map(context -> (Context<Optional<Student>>) context).findFirst()
                .flatMap(context -> context.getResult().orElseGet(Optional::empty));
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
     * @param macroInputParameter macro-command input parameter
     * @param <N>                 type of create-or-update student nested command result
     * @return built context of the command for input parameter
     * @see Student
     * @see StudentCommand
     * @see CreateStudentMacroCommand#createPersonContext(StudentCommand, Student)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final StudentCommand<N> command, final Input<?> macroInputParameter) {
        return macroInputParameter.value() instanceof Student person
                &&
                StudentCommand.CommandId.CREATE_OR_UPDATE.equals(command.getId())
                ?
                createPersonContext(command, person)
                :
                cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for create-or-update person command
     *
     * @param command create-or-update person command instance
     * @param person  input parameter of person to create
     * @param <N>     type of create-or-update person command result
     * @return built context of the command for input person
     * @see BusinessMessagePayloadMapper#toPayload(Student)
     * @see StudentPayload
     */
    public <N> Context<N> createPersonContext(final StudentCommand<N> command, final Student person) {
        final StudentPayload payload = adoptEntity(person);
        // prepare entity for create person sequence
        payload.setId(null);
        // create command-context with parameter by default
        return command.createContext(Input.of(payload));
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <N>       type of create-or-update student profile command result
     * @return built context of the command for input parameter
     * @see Student
     * @see StudentProfileCommand
     * @see CreateStudentMacroCommand#createProfileContext(StudentProfileCommand, Student)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(@NonNull final StudentProfileCommand<N> command, final Input<?> mainInput) {
        return mainInput.value() instanceof Student person
                &&
                StudentProfileCommand.CommandId.CREATE_OR_UPDATE.equals(command.getId())
                ?
                createProfileContext(command, person)
                :
                cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for create-or-update person profile command
     *
     * @param command create-or-update person profile command instance
     * @param person  input parameter of person to create
     * @param <N>     type of create-or-update student-profile nested command result
     * @return built context of the command for input parameter
     * @see StudentProfilePayload
     */
    public <N> Context<N> createProfileContext(final StudentProfileCommand<N> command, final Student person) {
        final String emailPrefix = person.getFirstName().trim().toLowerCase() + "." + person.getLastName().trim().toLowerCase();
        final StudentProfilePayload payload = StudentProfilePayload.builder()
                .id(null).phone("Not-Exists-Yet").email(emailPrefix + "@" + emailDomain)
                .build();
        // create command-context with created parameter by default
        return command.createContext(Input.of(payload));
    }

// for command do activities as nested command

    /**
     * To transfer result of the previous command execution to the next command context
     *
     * @param result  result of previous command execution value
     * @param context the command context of the next command
     */
    @Override
    @SuppressWarnings("unchecked")
    public void transferResultForward(final Object result, final Context<?> context) {
        final String commandId = context.getCommand().getId();
        if (isOptionalProfile(result) && hasPerson(context)) {
            log.debug("Transferring to context of '{}' the result {}", commandId, result);
            final Long profileId = ((Optional<StudentProfile>) result)
                    .orElseThrow(() -> new CannotTransferCommandResultException(commandId)).getId();
            log.debug("Transferring profile-id:{} to context of '{}'", profileId, commandId);
            transferProfileIdToStudentInput(profileId, context);
        } else {
            log.error("Cannot transfer to context of '{}' the result {}", commandId, result);
            throw new CannotTransferCommandResultException(commandId);
        }
    }

    /**
     * To transfer profile-id to create-person command input
     *
     * @param profileId the id of profile created before person
     * @param target    create-person command context
     * @see Context#getRedoParameter()
     * @see CommandContext#setRedoParameter(Input)
     * @see StudentPayload#setProfileId(Long)
     */
    public void transferProfileIdToStudentInput(final Long profileId, @NonNull final Context<?> target) {
        final StudentPayload personPayload = target.<StudentPayload>getRedoParameter().value();
        log.debug("Transferring profile id: {} to person: {}", profileId, personPayload);
        personPayload.setProfileId(profileId);
        if (target instanceof CommandContext<?> commandContext) {
            commandContext.setRedoParameter(Input.of(personPayload));
            log.debug("Transferred to student changed input parameter: {}", personPayload);
        }
    }

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareNestedContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    public Context<Optional<Student>> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> input) {
        return StudentCommand.super.acceptPreparedContext(visitor, input);
    }

    // private methods
    // to check is result to transfer has necessary type
    private static boolean isOptionalProfile(final Object result) {
        return result instanceof Optional<?> optional
                && optional.isPresent() && optional.get() instanceof StudentProfile;
    }

    // to check is context for create or update the person
    private static boolean hasPerson(Context<?> context) {
        final RootCommand<?> command = context.getCommand();
        return command instanceof StudentCommand<?> && CommandId.CREATE_OR_UPDATE.equals(command.getId());
    }

    // to throw CannotCreateCommandContextException exception
    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<?> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }
}
