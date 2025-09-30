package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAuthorityPersonMacroCommandTest extends TestModelFactory {
    @Mock
    PersistenceFacade persistence;
    @Spy
    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);
    @Spy
    @InjectMocks
    CreateOrUpdatePrincipalProfileCommand profileCommand;
    @Spy
    @InjectMocks
    CreateOrUpdateAuthorityPersonCommand personCommand;
    @Mock
    ActionExecutor actionExecutor;

    CreateAuthorityPersonMacroCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new CreateAuthorityPersonMacroCommand(personCommand, profileCommand, payloadMapper, actionExecutor) {
            @Override
            public NestedCommand<?> wrap(NestedCommand<?> command) {
                return spy(super.wrap(command));
            }
        });
    }

    @AfterEach
    void tearDown() {
        reset(payloadMapper);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        NestedCommand<?> nestedProfileCommand = nested.pop();
        if (nestedProfileCommand instanceof SequentialMacroCommand.Chained<?> chained) {
            assertThat(chained.unWrap()).isSameAs(profileCommand);
        } else {
            fail("nested profile command is not a chained command");
        }
        NestedCommand<?> nestedStudentCommand = nested.pop();
        if (nestedStudentCommand instanceof SequentialMacroCommand.Chained<?> chained) {
            assertThat(chained.unWrap()).isSameAs(personCommand);
        } else {
            fail("nested person command is not a chained command");
        }
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        AuthorityPersonPayload newPerson = payloadMapper.toPayload(makeCleanAuthorityPerson(1));
        reset(payloadMapper);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nestedCommands.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nestedCommands.pop();

        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));

        assertThat(context.isReady()).isTrue();
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        assertThat(parameter).isNotNull();
        assertThat(parameter.getRootInput().value()).isSameAs(newPerson);
        Deque<Context<?>> nested = parameter.getNestedContexts();
        assertThat(nested).hasSameSizeAs(command.fromNest());
        Context<?> profileContext = nested.pop();
        Context<?> personContext = nested.pop();

        assertThat(personContext).isNotNull();
        assertThat(personContext.isReady()).isTrue();
        assertThat(personContext.getCommand()).isSameAs(nestedPersonCommand);
        AuthorityPerson person = personContext.<AuthorityPerson>getRedoParameter().value();
        assertThat(person).isNotNull();
        assertThat(person.getId()).isNull();
        assertAuthorityPersonEquals(newPerson, person);
        String emailPrefix = person.getFirstName().toLowerCase() + "." + person.getLastName().toLowerCase();

        assertThat(profileContext).isNotNull();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(profileContext.getCommand()).isSameAs(nestedProfileCommand);
        PrincipalProfile profile = profileContext.<PrincipalProfile>getRedoParameter().value();
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNull();
        assertThat(profile.getEmail()).startsWith(emailPrefix);
        assertThat(profile.getPhone()).isNotEmpty();

        verifyProfileCommandContext(newPerson, nestedProfileCommand);

        verifyPersonCommandContext(newPerson, nestedPersonCommand);
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nested.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nested.pop();

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Optional<AuthorityPerson>> context = command.createContext(wrongInput);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.CREATE_OR_UPDATE);
        assertThat(context.getUndoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(nestedProfileCommand, wrongInput);
        verify(command, never()).createProfileContext(eq(nestedProfileCommand), any());

        verify(nestedPersonCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(nestedPersonCommand, wrongInput);
        verify(command, never()).createPersonContext(eq(nestedPersonCommand), any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateProfileContextThrows() {
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        AuthorityPerson newPerson = makeCleanAuthorityPerson(2);
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nested.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nested.pop();
        when(nestedProfileCommand.createContext(any(Input.class))).thenThrow(exception);

        Input<?> input = Input.of(newPerson);
        AuthorityPerson inputNewPerson = (AuthorityPerson) input.value();
        Context<Optional<AuthorityPerson>> context = command.createContext(input);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getUndoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(nestedProfileCommand, input);
        verify(command).createProfileContext(nestedProfileCommand, inputNewPerson);
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedPersonCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(nestedPersonCommand, input);
        verify(command).createPersonContext(nestedPersonCommand, inputNewPerson);
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        String errorMessage = "Cannot create nested student context";
        AuthorityPerson newPerson = makeCleanAuthorityPerson(3);
        RuntimeException exception = new RuntimeException(errorMessage);
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nested.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nested.pop();

        Input<?> input = Input.of(newPerson);
        AuthorityPerson inputNewPerson = (AuthorityPerson) input.value();
        doThrow(exception).when(nestedProfileCommand).createContext(any(Input.class));

        Context<Optional<AuthorityPerson>> context = command.createContext(input);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getUndoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(nestedProfileCommand, input);
        verify(command).createProfileContext(nestedProfileCommand, inputNewPerson);
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedPersonCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(nestedPersonCommand, input);
        verify(command).createPersonContext(nestedPersonCommand, inputNewPerson);
        verify(nestedPersonCommand).createContext(any(Input.class));
    }

    @Test
    void shouldExecuteDoCommand_CreateEntity() {
        Long profileId = 1L;
        Long personId = 2L;
        AuthorityPerson newPerson = makeCleanAuthorityPerson(4);
        adjustProfileSaving(profileId);
        adjustPersonSaving(personId);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));

        command.doCommand(context);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(context);
        Optional<AuthorityPerson> savedPerson = context.getResult().orElseThrow();
        assertThat(savedPerson.orElseThrow().getId()).isEqualTo(personId);
        assertThat(savedPerson.orElseThrow().getProfileId()).isEqualTo(profileId);
        assertAuthorityPersonEquals(savedPerson.orElseThrow(), newPerson, false);

        checkContextAfterDoCommand(profileContext);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        assertThat(profileResult.orElseThrow().getId()).isEqualTo(profileId);
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(personContext);
        Optional<AuthorityPerson> studentResult = personContext.getResult().orElseThrow();
        final AuthorityPerson person = studentResult.orElseThrow();
        assertAuthorityPersonEquals(person, newPerson, false);
        assertThat(person.getId()).isEqualTo(personId);
        assertThat(person.getProfileId()).isEqualTo(profileId);
        assertThat(personContext.<Long>getUndoParameter().value()).isEqualTo(personId);
        assertThat(savedPerson.orElseThrow()).isSameAs(person);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), personContext);
        verify(command).transferProfileIdToAuthorityPersonInput(profileId, personContext);

        verifyPersonDoCommand(personContext);
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(5);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        RuntimeException exception = new RuntimeException("Cannot process nested commands");
        doThrow(exception).when(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
    }

    @Test
    void shouldNotExecuteDoCommand_getCommandResultThrows() {
        Long profileId = 21L;
        Long personId = 32L;
        adjustProfileSaving(profileId);
        adjustPersonSaving(personId);
        AuthorityPerson newPerson = makeCleanAuthorityPerson(15);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        RuntimeException exception = new RuntimeException("Cannot get command result");
        doThrow(exception).when(command).finalCommandResult(any(Deque.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(profileContext);
        Optional<PrincipalProfile> profileResult = profileContext.getResult().orElseThrow();
        assertThat(profileResult.orElseThrow().getId()).isEqualTo(profileId);
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(personContext);
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        final AuthorityPerson student = personResult.orElseThrow();
        assertAuthorityPersonEquals(student, newPerson, false);
        assertThat(student.getId()).isEqualTo(personId);
        assertThat(student.getProfileId()).isEqualTo(profileId);
        assertThat(personContext.<Long>getUndoParameter().value()).isEqualTo(personId);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), personContext);
        verify(command).transferProfileIdToAuthorityPersonInput(profileId, personContext);

        verifyPersonDoCommand(personContext);
    }

    @Test
    void shouldNotExecuteDoCommand_CreateProfileDoNestedCommandsThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(6);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        RuntimeException exception = new RuntimeException("Cannot process profile nested command");
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) parameter.getNestedContexts().pop();
        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isEqualTo(exception);

        assertThat(personContext.getState()).isEqualTo(CANCEL);

//        verify(profileCommand).doAsNestedCommand(eq(command), eq(profileContext), any(Context.StateChangedListener.class));
//        verify(command).doNestedCommand(any(RootCommand.class), eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(PrincipalProfile.class));

        verify(command, never()).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any(Context.class));

//        verify(personCommand, never()).doAsNestedCommand(any(NestedCommandExecutionVisitor.class), any(Context.class), any(Context.StateChangedListener.class));
//        verify(command, never()).doNestedCommand(any(RootCommand.class), eq(personContext), any(Context.StateChangedListener.class));
    }

    @Test
    void shouldNotExecuteDoCommand_CreatePersonDoNestedCommandsThrows() {
        Long profileId = 10L;
        adjustProfileSaving(profileId);
        RuntimeException exception = new RuntimeException("Cannot process create person nested command");
        AuthorityPerson newPerson = makeCleanAuthorityPerson(7);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));

        doThrow(exception).when(persistence).save(any(AuthorityPerson.class));
        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> profileContext = parameter.getNestedContexts().pop();
        Context<?> personContext = parameter.getNestedContexts().pop();

        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        AuthorityPerson person = personContext.<AuthorityPerson>getRedoParameter().value();
        assertAuthorityPersonEquals(person, newPerson, false);
        assertThat(person.getProfileId()).isEqualTo(profileId);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(eq(profileCommand), any(Optional.class), eq(personContext));
        verify(command).transferProfileIdToAuthorityPersonInput(profileId, personContext);

        verifyPersonDoCommand(personContext);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileContext, profileId);
    }

    @Test
    void shouldExecuteUndoCommand() {
        Long profileId = 11L;
        Long personId = 21L;
        AuthorityPerson newPerson = makeCleanAuthorityPerson(8);
        adjustProfileSaving(profileId);
        adjustPersonSaving(personId);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));

        command.doCommand(context);
        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> profileContext = parameter.getNestedContexts().pop();
        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(personContext.isUndone()).isTrue();

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyPersonUndoCommand(personContext, personId);

        // nested commands order
        checkUndoNestedCommandsOrder(profileContext, personContext, personId, profileId);
    }

    @Test
    void shouldNotExecuteUndoCommand_UndoNestedCommandsThrowsException() {
        Long profileId = 5L;
        Long personId = 6L;
        AuthorityPerson newPerson = makeCleanAuthorityPerson(11);
        adjustProfileSaving(profileId);
        adjustPersonSaving(personId);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(command).rollbackNestedDone(any(Input.class));

        command.doCommand(context);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNestedDone(any(Input.class));
//        verify(personCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
//        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldNotExecuteUndoCommand_StudentUndoThrowsException() {
        Long profileId = 12L;
        Long personId = 22L;
        AuthorityPerson newPerson = makeCleanAuthorityPerson(9);
        adjustProfileSaving(profileId);
        adjustPersonSaving(personId);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        RuntimeException exception = new RuntimeException("Cannot process person undo command");
        doThrow(exception).when(persistence).deleteAuthorityPerson(personId);

        command.doCommand(context);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> profileContext = parameter.getNestedContexts().pop();
        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verifyPersonUndoCommand(personContext, personId);
    }

    @Test
    void shouldNotExecuteUndoCommand_ProfileUndoThrowsException() {
        Long profileId = 3L;
        Long personId = 4L;
        AuthorityPerson newPerson = makeCleanAuthorityPerson(10);
        adjustProfileSaving(profileId);
        adjustPersonSaving(personId);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        command.doCommand(context);
        reset(persistence, command, personCommand);
        RuntimeException exception = new RuntimeException("Cannot process profile undo command");

        doThrow(exception).when(persistence).deleteProfileById(profileId);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isEqualTo(exception);

        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyPersonUndoCommand(personContext, personId);
        verifyPersonDoCommand(personContext);
    }

    // private methods
    private void adjustPersonSaving(Long studentId) {
        doAnswer(invocation -> {
            AuthorityPerson person = invocation.getArgument(0, AuthorityPerson.class);
            AuthorityPersonPayload result = person instanceof AuthorityPersonPayload payload ? payload : payloadMapper.toPayload(person);
            result.setId(studentId);
            return Optional.of(result);
        }).when(persistence).save(any(AuthorityPerson.class));
    }

    private void adjustProfileSaving(Long profileId) {
        doAnswer(invocation -> {
            PrincipalProfile profile = invocation.getArgument(0, PrincipalProfile.class);
            PrincipalProfilePayload result = profile instanceof PrincipalProfilePayload payload ? payload: payloadMapper.toPayload(profile);
            result.setId(profileId);
            return Optional.of(result);
        }).when(persistence).save(any(PrincipalProfile.class));
    }

    private void checkUndoNestedCommandsOrder(Context<?> profileContext, Context<?> personContext, Long personId, Long profileId) {
        // nested commands order
        InOrder inOrder = Mockito.inOrder(command);
        // creating profile and student (profile is first) avers commands order
        Context<Optional<PrincipalProfile>> nestedProfileContext = (Context<Optional<PrincipalProfile>>) profileContext;
//        inOrder.verify(command).doNestedCommand(any(RootCommand.class), eq(nestedProfileContext), any(Context.StateChangedListener.class));
        Context<Optional<AuthorityPerson>> nestedPersonContext = (Context<Optional<AuthorityPerson>>) personContext;
//        inOrder.verify(command).doNestedCommand(any(RootCommand.class), eq(nestedPersonContext), any(Context.StateChangedListener.class));
        // undo creating profile and student (student is first) revers commands order
//        inOrder.verify(command).undoNestedCommand(any(RootCommand.class), eq(personContext));
//        inOrder.verify(command).undoNestedCommand(any(RootCommand.class), eq(profileContext));

        // persistence operations order
        inOrder = Mockito.inOrder(persistence);
        // creating profile and student (profile is first) avers operations order
        inOrder.verify(persistence).save(any(PrincipalProfile.class));
        inOrder.verify(persistence).save(any(AuthorityPerson.class));
        // undo creating profile and student (student is first) revers operations order
        inOrder.verify(persistence).deleteAuthorityPerson(personId);
        inOrder.verify(persistence).deleteProfileById(profileId);
    }

    private void verifyProfileUndoCommand(Context<?> profileContext, Long id) {
//        verify(profileCommand).undoAsNestedCommand(command, profileContext);
//        verify(command).undoNestedCommand(any(RootCommand.class), eq(profileContext));
        verify(profileCommand).undoCommand(profileContext);
        verify(profileCommand).executeUndo(profileContext);
        verify(persistence).deleteProfileById(id);
    }

    private void verifyPersonUndoCommand(Context<?> personContext, Long id) {
//        verify(personCommand).undoAsNestedCommand(command, personContext);
//        verify(command).undoNestedCommand(any(RootCommand.class), eq(personContext));
        verify(personCommand).undoCommand(personContext);
        verify(personCommand).executeUndo(personContext);
        verify(persistence).deleteAuthorityPerson(id);
    }

    private void verifyProfileDoCommand(Context<?> nestedContext) {
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContext;
//        verify(profileCommand).doAsNestedCommand(eq(command), eq(profileContext), any(Context.StateChangedListener.class));
//        verify(command).doNestedCommand(any(RootCommand.class), eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(PrincipalProfile.class));
    }

    private void verifyPersonDoCommand(Context<?> nestedContext) {
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContext;
//        verify(personCommand).doAsNestedCommand(eq(command), eq(personContext), any(Context.StateChangedListener.class));
//        verify(command).doNestedCommand(any(RootCommand.class), eq(personContext), any(Context.StateChangedListener.class));
        verify(personCommand).doCommand(personContext);
        verify(personCommand).executeDo(personContext);
        verify(persistence).save(any(AuthorityPerson.class));
    }

    private static <T> void checkContextAfterDoCommand(Context<Optional<T>> context) {
        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().get()).isPresent();
    }

    private void verifyPersonCommandContext(AuthorityPerson person, AuthorityPersonCommand<?> personCommand) {
        Input<?> inputPerson = Input.of(person);
        verify(personCommand).acceptPreparedContext(command, inputPerson);
        verify(command).prepareContext(personCommand, inputPerson);
        verify(command).createPersonContext(personCommand, person);
        verify(personCommand).createContext(any(Input.class));
    }

    private void verifyProfileCommandContext(AuthorityPerson person, PrincipalProfileCommand<?> profileCommand) {
        Input<?> inputPerson = Input.of(person);
        verify(profileCommand).acceptPreparedContext(command, inputPerson);
        verify(command).prepareContext(profileCommand, inputPerson);
        verify(command).createProfileContext(profileCommand, person);
        verify(profileCommand).createContext(any(Input.class));
    }
}