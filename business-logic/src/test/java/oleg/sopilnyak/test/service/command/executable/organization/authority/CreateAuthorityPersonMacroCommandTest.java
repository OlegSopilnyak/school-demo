package oleg.sopilnyak.test.service.command.executable.organization.authority;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.mapstruct.factory.Mappers;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
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
    CommandActionExecutor actionExecutor;

    CreateAuthorityPersonMacroCommand command;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        command = spy(new CreateAuthorityPersonMacroCommand(personCommand, profileCommand, payloadMapper, actionExecutor));
        ReflectionTestUtils.setField(personCommand, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(profileCommand, "applicationContext", applicationContext);
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonUpdate", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalUpdate", PrincipalProfileCommand.class);
        doCallRealMethod().when(actionExecutor).commitAction(any(ActionContext.class), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        ActionContext.setup("test-facade", "test-doingMainLoop");
    }

    @AfterEach
    void tearDown() {
        reset(payloadMapper, applicationContext);
    }

    @Test
    void shouldBeValidCommand() {
        reset(actionExecutor);
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        reset(actionExecutor);
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
        reset(actionExecutor);
        Object wrongTypeInput = "something";
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        PrincipalProfileCommand<?> nestedProfileCommand = (PrincipalProfileCommand<?>) nested.pop();
        AuthorityPersonCommand<?> nestedPersonCommand = (AuthorityPersonCommand<?>) nested.pop();

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Optional<AuthorityPerson>> context = command.createContext(wrongInput);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.CommandId.CREATE_OR_UPDATE);
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
        reset(actionExecutor);
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
        reset(actionExecutor);
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
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();

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
        Optional<AuthorityPerson> personResult = personContext.getResult().orElseThrow();
        final AuthorityPerson person = personResult.orElseThrow();
        assertAuthorityPersonEquals(person, newPerson, false);
        assertThat(person.getId()).isEqualTo(personId);
        assertThat(person.getProfileId()).isEqualTo(profileId);
        assertThat(personContext.<Long>getUndoParameter().value()).isEqualTo(personId);
        assertThat(savedPerson.orElseThrow()).isSameAs(person);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferResult(profileCommand, profileContext.getResult().get(), personContext);
        verify(command).transferProfileIdToAuthorityPersonUpdateInput(profileId, personContext);

        verifyPersonDoCommand(personContext);
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        reset(actionExecutor);
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
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContexts.pop();

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


        verify(command).transferResult(profileCommand, profileContext.getResult().get(), personContext);
        verify(command).transferProfileIdToAuthorityPersonUpdateInput(profileId, personContext);

        verifyPersonDoCommand(personContext);
    }

    @Test
    void shouldNotExecuteDoCommand_CreateProfileDoNestedCommandsThrows() {
        AuthorityPerson newPerson = makeCleanAuthorityPerson(6);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(newPerson));
        RuntimeException exception = new RuntimeException("Don't want to process profile nested command. Bad guy!");
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContexts.pop();
        Context<?> personContext = nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isEqualTo(exception);

        assertThat(personContext.getState()).isEqualTo(CANCEL);

        verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(PrincipalProfile.class));

        verify(command, never()).transferResult(eq(profileCommand), any(), eq(personContext));
        verify(command, never()).transferProfileIdToAuthorityPersonUpdateInput(anyLong(), any(Context.class));

        verify(personCommand, never()).doCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_CreatePersonDoNestedCommandsThrows() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
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

        verify(command).transferResult(eq(profileCommand), any(), eq(personContext));
        verify(command).transferProfileIdToAuthorityPersonUpdateInput(profileId, personContext);

        verifyPersonDoCommand(personContext);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileContext, profileId);
    }

    @Test
    void shouldExecuteUndoCommand() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
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
        command.doCommand(context);
        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(command).rollbackNested(any(Deque.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(command, never()).executeUndoNested(any(Context.class));
        verify(personCommand, never()).undoCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteUndoCommand_StudentUndoThrowsException() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
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
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
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
        inOrder.verify(command).executeDoNested(eq(nestedProfileContext), any(Context.StateChangedListener.class));
        Context<Optional<AuthorityPerson>> nestedPersonContext = (Context<Optional<AuthorityPerson>>) personContext;
        inOrder.verify(command).executeDoNested(eq(nestedPersonContext), any(Context.StateChangedListener.class));
        // undo creating profile and student (student is first) revers commands order
        inOrder.verify(command).executeUndoNested(personContext);
        inOrder.verify(command).executeUndoNested(profileContext);

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
        verify(command).executeUndoNested(profileContext);
        verify(profileCommand).undoCommand(profileContext);
        verify(profileCommand).executeUndo(profileContext);
        verify(persistence).deleteProfileById(id);
    }

    private void verifyPersonUndoCommand(Context<?> personContext, Long id) {
        verify(command).executeUndoNested(personContext);
        verify(personCommand).undoCommand(personContext);
        verify(personCommand).executeUndo(personContext);
        verify(persistence).deleteAuthorityPerson(id);
    }

    private void verifyProfileDoCommand(Context<?> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        Context<Optional<PrincipalProfile>> profileContext = (Context<Optional<PrincipalProfile>>) nestedContext;
        verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(PrincipalProfile.class));
    }

    private void verifyPersonDoCommand(Context<?> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any());
        Context<Optional<AuthorityPerson>> personContext = (Context<Optional<AuthorityPerson>>) nestedContext;
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