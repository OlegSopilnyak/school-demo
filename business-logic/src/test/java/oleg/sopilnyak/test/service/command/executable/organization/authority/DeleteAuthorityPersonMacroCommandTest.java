package oleg.sopilnyak.test.service.command.executable.organization.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.mapstruct.factory.Mappers;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class DeleteAuthorityPersonMacroCommandTest {
    @Mock
    PersistenceFacade persistence;
    @Spy
    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);
    @Spy
    @InjectMocks
    DeletePrincipalProfileCommand profileCommand;
    @Mock
    CommandActionExecutor actionExecutor;
    @Mock
    SchedulingTaskExecutor schedulingTaskExecutor;
    @Spy
    @InjectMocks
    DeleteAuthorityPersonCommand personCommand;
    @Mock
    ApplicationContext applicationContext;
    DeleteAuthorityPersonMacroCommand command;

    @Mock
    AuthorityPerson person;
    @Mock
    PrincipalProfile profile;
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @BeforeEach
    void setUp() {
        command = spy(new DeleteAuthorityPersonMacroCommand(
                personCommand, profileCommand, schedulingTaskExecutor, persistence, actionExecutor
        ));
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(profileCommand, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(personCommand, "applicationContext", applicationContext);
        threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("DeleteAuthorityPersonMacroCommand-");
        threadPoolTaskExecutor.initialize();
        doAnswer((Answer<Void>) invocationOnMock -> {
            threadPoolTaskExecutor.execute(invocationOnMock.getArgument(0, Runnable.class));
            return null;
        }).when(schedulingTaskExecutor).execute(any(Runnable.class));
        doReturn(command).when(applicationContext).getBean("authorityPersonMacroDelete", MacroDeleteAuthorityPerson.class);
        doCallRealMethod().when(actionExecutor).commitAction(any(ActionContext.class), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        ActionContext.setup("test-facade", "test-doingMainLoop");
    }

    @AfterEach
    void tearDown() {
        threadPoolTaskExecutor.shutdown();
        threadPoolTaskExecutor = null;
    }

    @Test
    void shouldBeValidCommand() {
        reset(actionExecutor, schedulingTaskExecutor, applicationContext);
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        assertThat(nested.pop()).isSameAs(personCommand);
        assertThat(nested.pop()).isSameAs(profileCommand);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long personId = 1L;
        Long profileId = 2L;
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        Input<?> input = Input.of(personId);

        Context<Boolean> context = command.createContext(input);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter redoParameter = context.<MacroCommandParameter>getRedoParameter().value();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getRootInput().value()).isSameAs(personId);
        Context<?> personContext = redoParameter.getNestedContexts().pop();
        Context<?> profileContext = redoParameter.getNestedContexts().pop();
        assertThat(personContext.isReady()).isTrue();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(personContext.<Long>getRedoParameter().value()).isSameAs(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isSameAs(profileId);

        verify(personCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(personCommand, input);
        verify(personCommand).createContext(input);

        verify(profileCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(profileCommand, input);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_PesronNotFound() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long personId = 3L;

        Input<?> inputPersonId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputPersonId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.getUndoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, inputPersonId);
        verify(command).prepareContext(personCommand, inputPersonId);
        verify(personCommand).createContext(inputPersonId);

        verify(profileCommand).acceptPreparedContext(command, inputPersonId);
        verify(command).prepareContext(profileCommand, inputPersonId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(profileCommand, never()).createContext(any());
        verify(profileCommand).createFailedContext(any(AuthorityPersonNotFoundException.class));
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        reset(actionExecutor, schedulingTaskExecutor, applicationContext);
        Object wrongTypeInput = "something";

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Boolean> context = command.createContext(wrongInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.CommandId.DELETE_BY_ID);
        assertThat(context.getUndoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(personCommand, wrongInput);
        verify(personCommand).createContext(wrongInput);

        verify(profileCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(profileCommand, wrongInput);
        verify(command, never()).createPrincipalProfileContext(eq(profileCommand), any());
        verify(profileCommand).createFailedContext(any(CannotCreateCommandContextException.class));
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePrincipalProfileContextThrows() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long personId = 4L;
        Long profileId = 5L;
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        Input<?> input = Input.of(personId);

        doThrow(exception).when(profileCommand).createContext(Input.of(profileId));
        Context<Boolean> context = command.createContext(input);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getUndoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(personCommand, input);
        verify(personCommand).createContext(input);

        verify(profileCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(profileCommand, input);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePrincipalContextThrows() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long personId = 6L;
        Long profileId = 7L;
        Input<?> input = Input.of(personId);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        String errorMessage = "Cannot create nested person context";
        RuntimeException exception = new RuntimeException(errorMessage);

        doThrow(exception).when(personCommand).createContext(input);
        Context<Boolean> context = command.createContext(input);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getUndoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(personCommand, input);
        verify(personCommand).createContext(input);

        verify(profileCommand).acceptPreparedContext(command, input);
        verify(command).prepareContext(profileCommand, input);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldExecuteDoCommand() {
        Long profileId = 8L;
        Long personId = 9L;
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Input<?> inputPersonId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputPersonId);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(personContext.<Long>getRedoParameter().value()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isEqualTo(profileId);

        verifyPersonDoCommand(personContext);
        verifyProfileDoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteDoCommand_PersonNotFound() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long personId = 10L;
        Input<?> inputPersonId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputPersonId);
        assertThat(context.isReady()).isFalse();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command, never()).executeDo(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_ProfileNotFound() {
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 12L;
        Long personId = 11L;
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        Context<Boolean> context = command.createContext(Input.of(personId));
        assertThat(context.isReady()).isTrue();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        verifyPersonDoCommand(personContext);
        verifyPersonUndoCommand(personContext);
        verify(profileCommand, never()).undoCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_DeletePersonThrows() {
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 14L;
        Long personId = 13L;
        when(person.getId()).thenReturn(personId);
        when(profile.getId()).thenReturn(profileId);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(persistence.save(any(PrincipalProfile.class))).thenReturn(Optional.of(profile));
        Context<Boolean> context = command.createContext(Input.of(personId));
        String errorMessage = "Cannot delete person";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.deleteAuthorityPerson(personId)).thenThrow(exception);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(personContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(personContext.<PrincipalProfile>getUndoParameter().isEmpty()).isTrue();
        assertThat(personContext.getResult()).isEmpty();

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        verifyPersonDoCommand(personContext);
        verifyProfileDoCommand(profileContext);
        verifyProfileUndoCommand(profileContext);
        verify(personCommand, never()).undoCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 16L;
        Long personId = 15L;
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(Input.of(personId));
        String errorMessage = "Cannot delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyPersonDoCommand(personContext);
        verifyProfileDoCommand(profileContext);

        verifyPersonUndoCommand(personContext);
        verify(profileCommand, never()).undoCommand(any(Context.class));
    }

    @Test
    void shouldExecuteUndoCommand() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        Long profileId = 18L;
        Long personId = 17L;
        Context<Boolean> context = createPrincipalAndProfileFor(profileId, personId);
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        when(persistence.save(any(PrincipalProfile.class))).thenReturn(Optional.of(profile));

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verifyPersonUndoCommand(personContext);
        verifyProfileUndoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 20L;
        Long personId = 19L;
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        Context<Boolean> context = createPrincipalAndProfileFor(profileId, personId);
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        String errorMessage = "Cannot restore profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.save(any(PrincipalProfile.class))).thenThrow(exception);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verifyPersonUndoCommand(personContext);
        verifyProfileUndoCommand(profileContext);
        verifyPersonDoCommand(personContext, 2);
    }

    @Test
    void shouldNotExecuteUndoCommand_SavePersonThrows() {
        Long profileId = 22L;
        Long personId = 21L;
        doReturn(personCommand).when(applicationContext).getBean("authorityPersonDelete", AuthorityPersonCommand.class);
        doReturn(profileCommand).when(applicationContext).getBean("profilePrincipalDelete", PrincipalProfileCommand.class);
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        when(profile.getId()).thenReturn(profileId);
        Context<Boolean> context = createPrincipalAndProfileFor(profileId, personId);
        when(persistence.save(any(PrincipalProfile.class))).thenReturn(Optional.of(profile));
        String errorMessage = "Cannot restore person";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(AuthorityPerson.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isSameAs(exception);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult()).isEmpty();

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verifyPersonUndoCommand(personContext);
        verifyProfileUndoCommand(profileContext);
        verifyProfileDoCommand(profileContext, 2);
    }


    // private methods
    private Context<Boolean> createPrincipalAndProfileFor(Long profileId, Long personId) {
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        when(profile.getId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(Input.of(personId));
        command.doCommand(context);
        return context;
    }

    private void verifyProfileDoCommand(Context<?> nestedContext) {
        verifyProfileDoCommand(nestedContext, 1);
    }

    private void verifyProfileDoCommand(Context<?> nestedContext, int i) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        Context<Boolean> profileContext = (Context<Boolean>) nestedContext;
        verify(profileCommand, times(i)).doCommand(profileContext);
        verify(profileCommand, times(i)).executeDo(profileContext);
        Long id = nestedContext.<Long>getRedoParameter().value();
        verify(persistence, times(i)).findPrincipalProfileById(id);
        verify(persistence, times(i)).deleteProfileById(id);
    }

    private void verifyPersonDoCommand(Context<?> nestedContext) {
        verifyPersonDoCommand(nestedContext, 1);
    }

    private void verifyPersonDoCommand(Context<?> nestedContext, int i) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        Context<Boolean> personContext = (Context<Boolean>) nestedContext;
        verify(personCommand, times(i)).doCommand(personContext);
        verify(personCommand, times(i)).executeDo(personContext);
        Long id = nestedContext.<Long>getRedoParameter().value();
        verify(persistence, times(i + 1)).findAuthorityPersonById(id);
        verify(persistence, times(i)).deleteAuthorityPerson(id);
    }

    private void verifyPersonUndoCommand(Context<?> nestedContext) {
        verify(command).executeUndoNested(nestedContext);
        Context<Boolean> personContext = (Context<Boolean>) nestedContext;
        verify(personCommand).undoCommand(personContext);
        verify(personCommand).executeUndo(personContext);
        verify(persistence).save(any(AuthorityPerson.class));
    }

    private void verifyProfileUndoCommand(Context<?> nestedContext) {
        verify(command).executeUndoNested(nestedContext);
        Context<Boolean> profileContext = (Context<Boolean>) nestedContext;
        verify(profileCommand).undoCommand(profileContext);
        verify(profileCommand).executeUndo(profileContext);
        verify(persistence).save(any(PrincipalProfile.class));
    }
}