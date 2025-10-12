package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Deque;
import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class,
        AuthorityPersonFacadeImpl.class,
//        DeletePrincipalProfileCommand.class,
//        DeleteAuthorityPersonCommand.class,
        SchoolCommandsConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.parallel.max.pool.size=10",
        "school.spring.jpa.show-sql=true",
        "school.hibernate.hbm2ddl.auto=update"
})
@Rollback
class DeleteAuthorityPersonMacroCommandTest extends MysqlTestModelFactory {
    final int maxPoolSize = 10;
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    AuthorityPersonFacadeImpl facade;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    ActionExecutor actionExecutor;
    @SpyBean
    @Autowired
    SchedulingTaskExecutor schedulingTaskExecutor;
    @Autowired
    CommandThroughMessageService messagesExchangeService;
    @SpyBean
    @Autowired
    @Qualifier("profilePrincipalDelete")
    PrincipalProfileCommand profileCommand;
    @SpyBean
    @Autowired
    @Qualifier("authorityPersonDelete")
    AuthorityPersonCommand personCommand;

    DeleteAuthorityPersonMacroCommand command;
    @Captor
    ArgumentCaptor<AuthorityPersonCommand> personCaptor;
    @Captor
    ArgumentCaptor<PrincipalProfileCommand> profileCaptor;

    @BeforeEach
    void setUp() {
        Assertions.setMaxStackTraceElementsDisplayed(1000);
        command = spy(new DeleteAuthorityPersonMacroCommand(personCommand, profileCommand, schedulingTaskExecutor, persistence, actionExecutor));
        ActionContext.setup("test-facade", "test-action");
        messagesExchangeService.initialize();
    }

    @AfterEach
    void tearDown() {
        reset(command, profileCommand, personCommand, persistence, payloadMapper);
        messagesExchangeService.shutdown();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeValidCommand() {
        assertThat(facade).isNotNull();
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(command, "maxPoolSize")).isSameAs(maxPoolSize);
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateMacroCommandContexts() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(1));
        Long personId = person.getId();
        Long profileId = person.getProfileId();
        assertThat(persistence.findAuthorityPersonById(personId)).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();
        reset(persistence);

        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);

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

        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(command).createPrincipalProfileContext(profileCaptor.capture(), eq(personId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_PersonNotFound() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(3));
        assertThat(persistence.findAuthorityPersonById(person.getId())).isPresent();
        Long personId = 3L;

        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(command).createPrincipalProfileContext(profileCaptor.capture(), eq(personId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createFailedContext(any(AuthorityPersonNotFoundException.class));
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";

        Input<?> inputId = Input.of(wrongTypeInput);
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.DELETE_BY_ID);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(command, never()).createPrincipalProfileContext(any(), any());
        verify(profileCommand).createFailedContext(any(CannotCreateCommandContextException.class));
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_CreatePrincipalProfileContextThrows() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(5));
        Long personId = person.getId();
        Long profileId = person.getProfileId();
        assertThat(persistence.findAuthorityPersonById(personId)).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();
        reset(persistence);
        Input<Long> inputId = Input.of(personId);

        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(Input.of(profileId));
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(command).createPrincipalProfileContext(profileCaptor.capture(), eq(personId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(7));
        Long personId = person.getId();
        Long profileId = person.getProfileId();
        assertThat(persistence.findAuthorityPersonById(personId)).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileId)).isPresent();
        reset(persistence);
        Input<Long> inputId = Input.of(personId);

        String errorMessage = "Cannot create nested person context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(personCommand).createContext(inputId);

        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(command).createPrincipalProfileContext(profileCaptor.capture(), eq(personId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteDoCommand() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(9));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Boolean> personContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(personContext.<Long>getRedoParameter().value()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isEqualTo(profileId);

        verifyPersonDoCommand(personContext);
        verifyProfileDoCommand(profileContext);
        assertThat(persistence.findAuthorityPersonById(personId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_PersonNotFound() {
        Long personId = 10L;
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        assertThat(context.isReady()).isFalse();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.getRedoParameter().isEmpty()).isTrue();
        verify(command, never()).executeDo(any(Context.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_ProfileNotFound() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(11));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        persistence.deleteProfileById(profileId);
        assertThat(persistence.findPrincipalProfileById(profileId)).isEmpty();
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        assertThat(context.isReady()).isTrue();
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> personContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyPersonDoCommand(personContext);
        verify(persistence).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(any(AuthorityPersonPayload.class));
        assertThat(persistence.findAuthorityPersonById(personContext.<Long>getRedoParameter().value())).isPresent();

//        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));

        assertThat(persistence.findPrincipalProfileById(profileId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileContext.<Long>getRedoParameter().value())).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_DeletePersonThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(13));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        assertThat(persistence.findAuthorityPersonById(personId)).isPresent();
        reset(persistence);
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        String errorMessage = "Cannot delete person";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteAuthorityPerson(personId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> personContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(personContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(personContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        PrincipalProfilePayload savedProfile = profileContext.<PrincipalProfilePayload>getUndoParameter().value();
        assertThat(savedProfile.getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyPersonDoCommand(personContext);
        verify(persistence, times(2)).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findPrincipalProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(savedProfile);

//        verify(personCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        assertThat(persistence.findAuthorityPersonById(personContext.<Long>getRedoParameter().value())).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileContext.<Long>getRedoParameter().value())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(15));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        reset(persistence);
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        String errorMessage = "Cannot delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> personContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyPersonDoCommand(personContext);
        verify(persistence, times(2)).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findPrincipalProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(personContext.<AuthorityPerson>getUndoParameter().value());

//        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        assertThat(persistence.findAuthorityPersonById(personContext.<Long>getRedoParameter().value())).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileContext.<Long>getRedoParameter().value())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteUndoCommand() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(17));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        command.doCommand(context);
        reset(persistence);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> personContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(personContext.<AuthorityPerson>getUndoParameter().value());

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<PrincipalProfile>getUndoParameter().value());

        assertThat(persistence.findAuthorityPersonById(personContext.<Long>getRedoParameter().value())).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileContext.<Long>getRedoParameter().value())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(17));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        command.doCommand(context);
        reset(persistence);
        String errorMessage = "Don't want to restore profile. Bad guy!";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Boolean> personContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertAuthorityPersonEquals(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal(), person, false);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(any(AuthorityPersonPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<PrincipalProfile>getUndoParameter().value());

        verifyPersonDoCommand(personContext, 2);
        Long newPersonId = personContext.<Long>getRedoParameter().value();
        verify(persistence).findAuthorityPersonById(newPersonId);
        verify(persistence).deleteAuthorityPerson(newPersonId);

        assertThat(persistence.findAuthorityPersonById(newPersonId)).isEmpty();
        assertThat(persistence.findAuthorityPersonById(personId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileContext.<Long>getRedoParameter().value())).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_SavePersonThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(21));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        command.doCommand(context);
        reset(persistence);
        String errorMessage = "Cannot restore person";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(AuthorityPerson.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> personContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isSameAs(exception);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertProfilesEquals(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal(), profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(any(AuthorityPersonPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(any(PrincipalProfilePayload.class));

        verifyProfileDoCommand(profileContext, 2);
        Long newProfileId = profileContext.<Long>getRedoParameter().value();
        verify(persistence).findPrincipalProfileById(newProfileId);
        verify(persistence).deleteProfileById(newProfileId);

        assertThat(persistence.findAuthorityPersonById(personContext.<Long>getRedoParameter().value())).isEmpty();
        assertThat(persistence.findAuthorityPersonById(personId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(newProfileId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileId)).isEmpty();
    }


    // private methods
    private AuthorityPersonPayload create(AuthorityPerson person) {
        return (AuthorityPersonPayload) facade.create(person).orElseThrow();
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext) {
        verifyProfileDoCommand(nestedContext, 1);
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext, int i) {
//        verify(profileCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
//        verify(command, times(i)).doNestedCommand(any(RootCommand.class), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand, times(i)).doCommand(nestedContext);
        verify(profileCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyPersonDoCommand(Context<Boolean> nestedContext) {
        verifyPersonDoCommand(nestedContext, 1);
    }

    private void verifyPersonDoCommand(Context<Boolean> nestedContext, int i) {
//        verify(personCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
//        verify(command, times(i)).doNestedCommand(any(RootCommand.class), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(personCommand, times(i)).doCommand(nestedContext);
        verify(personCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyPersonUndoCommand(Context<Boolean> nestedContext) {
//        verify(personCommand).undoAsNestedCommand(command, nestedContext);
//        verify(command).undoNestedCommand(any(RootCommand.class), eq(nestedContext));
        verify(personCommand).undoCommand(nestedContext);
        verify(personCommand).executeUndo(nestedContext);
    }

    private void verifyProfileUndoCommand(Context<Boolean> nestedContext) {
//        verify(profileCommand).undoAsNestedCommand(command, nestedContext);
//        verify(command).undoNestedCommand(any(RootCommand.class), eq(nestedContext));
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
    }
}