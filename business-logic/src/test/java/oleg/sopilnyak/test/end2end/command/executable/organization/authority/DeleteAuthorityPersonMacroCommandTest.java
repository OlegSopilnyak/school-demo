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
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.AuthorityPersonRepository;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonMacroCommand;
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
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class,
        AuthorityPersonFacadeImpl.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "school.hibernate.hbm2ddl.auto=update"
})
class DeleteAuthorityPersonMacroCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    AuthorityPersonRepository authorityPersonRepository;
    @Autowired
    PersonProfileRepository profileRepository;
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
    @Autowired
    ApplicationContext applicationContext;
    DeleteAuthorityPersonMacroCommand command;
    @Captor
    ArgumentCaptor<AuthorityPersonCommand> personCaptor;
    @Captor
    ArgumentCaptor<PrincipalProfileCommand> profileCaptor;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        Assertions.setMaxStackTraceElementsDisplayed(1000);
        command = spy(new DeleteAuthorityPersonMacroCommand(
                personCommand, profileCommand, schedulingTaskExecutor, persistence, actionExecutor, applicationContext
        ));
        ActionContext.setup("test-facade", "test-action");
        messagesExchangeService.initialize();
        authorityPersonRepository.deleteAll();
        profileRepository.deleteAll();
        authorityPersonRepository.flush();
        profileRepository.flush();
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void tearDown() {
        reset(command, profileCommand, personCommand, persistence, payloadMapper);
        messagesExchangeService.shutdown();
        authorityPersonRepository.deleteAll();
        profileRepository.deleteAll();
        authorityPersonRepository.flush();
        profileRepository.flush();
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(facade).isNotNull();
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(1));
        Long personId = person.getId();
        Long profileId = person.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);

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
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_PersonNotFound() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(3));
        Long personId = person.getId();
        Long profileId = person.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        assertThat(deletePersonEntity(personId)).isNull();

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
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createFailedContext(any(AuthorityPersonNotFoundException.class));
        verify(profileCommand, never()).createContext(any());
    }

    @Test
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
    void shouldNotCreateMacroCommandContext_CreatePrincipalProfileContextThrows() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(5));
        Long personId = person.getId();
        Long profileId = person.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
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
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(7));
        Long personId = person.getId();
        Long profileId = person.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        reset(persistence);
        Input<Long> inputId = Input.of(personId);

        String errorMessage = "Don't want to create nested person context, Bad guy!";
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
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldExecuteDoCommand() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(9));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        AuthorityPerson person = findPersonEntity(personId);
        PrincipalProfile profile = findProfileEntity(profileId);
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        reset(persistence);

        command.doCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();
        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isDone()).isTrue();
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isEqualTo(person);
        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);

//        verify(command).executeDo(context);
//        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        verify(command, times(2)).self();
        assertThat(personContext.<Long>getRedoParameter().value()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isEqualTo(profileId);

        verifyPersonDoCommand(personContext);
        verifyProfileDoCommand(profileContext);
    }

    @Test
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
    void shouldNotExecuteDoCommand_ProfileNotFound() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(11));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        AuthorityPersonEntity person = findPersonEntity(personId);
        assertThat(deleteProfileEntity(profileId)).isNull();
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        assertThat(context.isReady()).isTrue();
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isEqualTo(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
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
        Long undoPersonId = personContext.<Long>getRedoParameter().value();
        Long undoProfileId = profileContext.<Long>getRedoParameter().value();
        assertThat(findPersonEntity(personId)).isNull();
        assertThat(findProfileEntity(profileId)).isNull();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(undoPersonId) != null);

        verify(profileCommand, never()).undoCommand(any(Context.class));
        assertThat(findProfileEntity(undoProfileId)).isNull();
    }

    @Test
    void shouldNotExecuteDoCommand_DeletePersonThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(13));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        PrincipalProfile profile = findProfileEntity(profileId);
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
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(personContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(personContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
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

        verify(personCommand, never()).undoCommand(any(Context.class));
        Long undonePersonId = personContext.<Long>getRedoParameter().value();
        Long undoneProfileId = profileContext.<Long>getRedoParameter().value();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(undonePersonId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(undoneProfileId) != null);
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(15));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        AuthorityPerson person = findPersonEntity(personId);
        reset(persistence);
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        String errorMessage = "Don't want to delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isEqualTo(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
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

        verify(profileCommand, never()).undoCommand(any(Context.class));
        Long undonePersonId = personContext.<Long>getRedoParameter().value();
        Long undoneProfileId = profileContext.<Long>getRedoParameter().value();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(undonePersonId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(undoneProfileId) != null);
    }

    @Test
    void shouldExecuteUndoCommand() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(17));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        AuthorityPerson person = findPersonEntity(personId);
        PrincipalProfile profile = findProfileEntity(profileId);
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        command.doCommand(context);
        reset(persistence);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isEqualTo(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(personContext.<AuthorityPerson>getUndoParameter().value());

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<PrincipalProfile>getUndoParameter().value());

        Long undoPersonId = personContext.<Long>getRedoParameter().value();
        Long undoProfileId = profileContext.<Long>getRedoParameter().value();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(undoPersonId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(undoProfileId) != null);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(117));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        AuthorityPerson person = findPersonEntity(personId);
        PrincipalProfile profile = findProfileEntity(profileId);
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        command.doCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
        reset(persistence);
        String errorMessage = "Don't want to restore profile. Bad guy!";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.undoCommand(context);
//        await().atMost(200, TimeUnit.MILLISECONDS).until(() ->
//                context.<MacroCommandParameter>getRedoParameter().value().getNestedContexts().getFirst().isDone()
//        );

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();
        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isDone()).isTrue();
        assertAuthorityPersonEquals(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal(), person, false);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
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
        Long doPersonId = personContext.<Long>getRedoParameter().value();
        Long doProfileId = profileContext.<Long>getRedoParameter().value();
        verify(persistence).findAuthorityPersonById(doPersonId);
        verify(persistence).deleteAuthorityPerson(doPersonId);

        assertThat(findPersonEntity(doPersonId)).isNull();
        assertThat(findPersonEntity(personId)).isNull();
        assertThat(findProfileEntity(doProfileId)).isNull();
        assertThat(findProfileEntity(profileId)).isNull();
    }

    @Test
    void shouldNotExecuteUndoCommand_SavePersonThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(21));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        AuthorityPerson person = findPersonEntity(personId);
        PrincipalProfile profile = findProfileEntity(profileId);
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        command.doCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
        reset(persistence);
        String errorMessage = "Don't want to restore person. Bad guy!";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(AuthorityPerson.class));

        command.undoCommand(context);
//        await().atMost(200, TimeUnit.MILLISECONDS).until(() ->
//                context.<MacroCommandParameter>getRedoParameter().value().getNestedContexts().getLast().isDone()
//        );

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        final Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isSameAs(exception);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginal()).isEqualTo(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
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
        Long doProfileId = profileContext.<Long>getRedoParameter().value();
        verify(persistence).findPrincipalProfileById(doProfileId);
        verify(persistence).deleteProfileById(doProfileId);
        Long doPersonId = personContext.<Long>getRedoParameter().value();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(doPersonId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(doProfileId) == null);

        assertThat(findPersonEntity(doPersonId)).isNull();
        assertThat(findPersonEntity(personId)).isNull();
        assertThat(findProfileEntity(doProfileId)).isNull();
        assertThat(findProfileEntity(profileId)).isNull();
    }


    // private methods
    private AuthorityPersonEntity findPersonEntity(Long id) {
        return findEntity(AuthorityPersonEntity.class, id);
    }

    private AuthorityPersonEntity deletePersonEntity(Long id) {
        return deleteEntity(AuthorityPersonEntity.class, id);
    }

    private PrincipalProfileEntity findProfileEntity(Long id) {
        return findEntity(PrincipalProfileEntity.class, id);
    }

    private PrincipalProfileEntity deleteProfileEntity(Long id) {
        return deleteEntity(PrincipalProfileEntity.class, id);
    }

    private AuthorityPersonPayload create(AuthorityPerson person) {
        return (AuthorityPersonPayload) facade.create(person).orElseThrow();
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext) {
        verifyProfileDoCommand(nestedContext, 1);
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext, int i) {
//        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand, times(i)).doCommand(nestedContext);
        verify(profileCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyPersonDoCommand(Context<Boolean> nestedContext) {
        verifyPersonDoCommand(nestedContext, 1);
    }

    private void verifyPersonDoCommand(Context<Boolean> nestedContext, int i) {
//        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(personCommand, times(i)).doCommand(nestedContext);
        verify(personCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyPersonUndoCommand(Context<Boolean> nestedContext) {
        verify(command).executeUndoNested(nestedContext);
        verify(personCommand).undoCommand(nestedContext);
        verify(personCommand).executeUndo(nestedContext);
    }

    private void verifyProfileUndoCommand(Context<Boolean> nestedContext) {
        verify(command).executeUndoNested(nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
    }
}