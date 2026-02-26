package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import jakarta.persistence.EntityManager;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        PersistenceConfiguration.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {
        "school.parallel.max.pool.size=10",
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
@SuppressWarnings("unchecked")
class DeleteAuthorityPersonMacroCommandTest extends MysqlTestModelFactory {
    private static final String PROFILE_DELETE_BY_ID = "school::person::profile::principal:delete.By.Id";
    @Autowired
    ApplicationContext applicationContext;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    CommandActionExecutor actionExecutor;
    @MockitoSpyBean
    @Autowired
    @Qualifier("parallelCommandNestedCommandsExecutor")
    Executor schedulingTaskExecutor;
    @MockitoSpyBean
    @Autowired
    @Qualifier("profilePrincipalDelete")
    PrincipalProfileCommand profileCommand;
    @MockitoSpyBean
    @Autowired
    @Qualifier("authorityPersonDelete")
    AuthorityPersonCommand personCommand;
    // delete person macro command
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
                personCommand, profileCommand, schedulingTaskExecutor, persistence, actionExecutor
        ));
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        ActionContext.setup("test-facade", "test-action");
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void tearDown() {
        reset(command, profileCommand, personCommand, persistence, payloadMapper);
        deleteEntities(AuthorityPersonEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
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
        assertThat(context.getException().getMessage()).contains(PROFILE_DELETE_BY_ID);
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
        AuthorityPerson person = findPersonEntity(personId);
        PrincipalProfile profile = findProfileEntity(profileId);
        Context<Boolean> context = command.createContext(Input.of(personId));
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
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginalType()).isEqualTo(person.getClass().getName());
        assertAuthorityPersonEquals(personContext.<AuthorityPersonPayload>getUndoParameter().value(), person, false);
        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().value().getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(profileContext.<PrincipalProfilePayload>getUndoParameter().value(), profile, false);

        verify(command).self();
        assertThat(personContext.<Long>getRedoParameter().value()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isEqualTo(profileId);

        verifyPersonDoCommand();
        verify(persistence).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyProfileDoCommand();
        verify(persistence).findPrincipalProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);
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
        assertThat(findProfileEntity(profileId)).isNotNull();
        assertThat(findPersonEntity(personId)).isNotNull();
        assertThat(deleteProfileEntity(profileId)).isNull();
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        reset(persistence, command);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().value().getOriginalType()).isEqualTo(personPayload.getOriginalType());
        assertAuthorityPersonEquals(personContext.<AuthorityPersonPayload>getUndoParameter().value(), personPayload, false);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).self();

        verifyPersonDoCommand();
        verify(persistence).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyPersonUndoCommand();
        verify(persistence).save(any(AuthorityPersonPayload.class));
        assertThat(findPersonEntity(personId)).isNull();
        assertThat(findProfileEntity(profileId)).isNull();
        Long undoPersonId = personContext.<Long>getRedoParameter().value();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(undoPersonId) != null);

        verify(profileCommand, never()).undoCommand(any(Context.class));
        Long undoProfileId = profileContext.<Long>getRedoParameter().value();
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
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        reset(persistence, command);
        String errorMessage = "Cannot delete person";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteAuthorityPerson(personId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
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
        assertThat(savedProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(savedProfile, profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).self();

        verifyPersonDoCommand(false);
        verify(persistence).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyProfileDoCommand();
        verify(persistence).findPrincipalProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyProfileUndoCommand();
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
        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);
        reset(persistence, command);
        String errorMessage = "Don't want to delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isUndone()).isTrue();
        AuthorityPersonPayload undoPerson = personContext.<AuthorityPersonPayload>getUndoParameter().value();
        assertThat(undoPerson.getOriginalType()).isEqualTo(person.getClass().getName());
        assertAuthorityPersonEquals(undoPerson, person, false);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(exception.getClass());
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).self();

        verifyPersonDoCommand();
        verify(persistence).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyProfileDoCommand(false);
        verify(persistence).findPrincipalProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyPersonUndoCommand();
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
        Context<Boolean> context = command.createContext(Input.of(personId));
        command.doCommand(context);
        reset(persistence, command);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isUndone()).isTrue();
        AuthorityPersonPayload undoPerson = personContext.<AuthorityPersonPayload>getUndoParameter().value();
        assertThat(undoPerson.getOriginalType()).isEqualTo(person.getClass().getName());
        assertAuthorityPersonEquals(undoPerson, person, false);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isUndone()).isTrue();
        PrincipalProfilePayload undoProfile = profileContext.<PrincipalProfilePayload>getUndoParameter().value();
        assertThat(undoProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(undoProfile, profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).self();

        verifyPersonUndoCommand();
        verify(persistence).save(personContext.<AuthorityPerson>getUndoParameter().value());

        verifyProfileUndoCommand();
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
        AuthorityPerson person = findPersonEntity(personId);
        PrincipalProfile profile = findProfileEntity(profileId);
        Context<Boolean> context = command.createContext(Input.of(personId));
        command.doCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
        reset(persistence, command);
        String errorMessage = "Don't want to restore profile. Bad guy!";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.undoCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() ->
                context.<MacroCommandParameter>getRedoParameter().value().getNestedContexts().getFirst().isDone()
        );

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = parameter.getNestedContexts();
        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isDone()).isTrue();
        AuthorityPersonPayload undoPerson = personContext.<AuthorityPersonPayload>getUndoParameter().value();
        assertThat(undoPerson.getOriginalType()).isEqualTo(person.getClass().getName());
        assertAuthorityPersonEquals(undoPerson, person, false);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(exception.getClass());
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        PrincipalProfilePayload undoProfile = profileContext.<PrincipalProfilePayload>getUndoParameter().value();
        assertThat(undoProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(undoProfile, profile, false);
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).self();

        verifyPersonUndoCommand();
        verify(persistence).save(any(AuthorityPersonPayload.class));

        verifyProfileUndoCommand();
        verify(persistence).save(profileContext.<PrincipalProfile>getUndoParameter().value());

        verifyPersonDoCommand(2);
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
        AuthorityPerson person = findPersonEntity(personId);
        PrincipalProfile profile = findProfileEntity(profileId);
        Context<Boolean> context = command.createContext(Input.of(personId));
        command.doCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findPersonEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
        reset(persistence, command);
        String errorMessage = "Don't want to restore person. Bad guy!";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(AuthorityPerson.class));

        command.undoCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() ->
                context.<MacroCommandParameter>getRedoParameter().value().getNestedContexts().getLast().isDone()
        );

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        final Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());

        Context<Boolean> personContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isInstanceOf(exception.getClass());
        assertThat(personContext.getException().getMessage()).isEqualTo(errorMessage);
        AuthorityPersonPayload undoPerson = personContext.<AuthorityPersonPayload>getUndoParameter().value();
        assertThat(undoPerson.getOriginalType()).isEqualTo(person.getClass().getName());
        assertAuthorityPersonEquals(undoPerson, person, false);
        assertThat(personContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isDone()).isTrue();
        PrincipalProfilePayload undoProfile = profileContext.<PrincipalProfilePayload>getUndoParameter().value();
        assertThat(undoProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(undoProfile, profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).self();

        verifyPersonUndoCommand();
        verify(persistence).save(any(AuthorityPersonPayload.class));

        verifyProfileUndoCommand();
        verify(persistence).save(any(PrincipalProfilePayload.class));

        verifyProfileDoCommand(2);
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

    private AuthorityPersonPayload create(AuthorityPerson newPerson) {
        try {
            PrincipalProfile profile = persist(makePrincipalProfile(null));
            if (newPerson instanceof FakeAuthorityPerson fake) {
                fake.setProfileId(profile.getId());
            } else {
                fail("Not a fake person type");
            }
            return payloadMapper.toPayload(persist(newPerson));
        } finally {
            reset(payloadMapper);
        }
    }

    private AuthorityPerson persist(AuthorityPerson newInstance) {
        AuthorityPersonEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private PrincipalProfile persist(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private void verifyPersonDoCommand() {
        verifyPersonDoCommand(true);
    }

    private void verifyPersonDoCommand(boolean checkResult) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        if (checkResult) {
            assertThat(nestedContext.getResult().orElseThrow()).isTrue();
        }
        assertThat(nestedContext.getCommand().getId()).isEqualTo(personCommand.getId());
        verify(personCommand).executeDo(nestedContext);
    }

    private void verifyProfileDoCommand() {
        verifyProfileDoCommand(true);
    }

    private void verifyProfileDoCommand(boolean checkResult) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        if (checkResult) {
            assertThat(nestedContext.getResult().orElseThrow()).isTrue();
        }
        assertThat(nestedContext.getCommand().getId()).isEqualTo(profileCommand.getId());
        verify(profileCommand).executeDo(nestedContext);
    }

    private void verifyPersonUndoCommand() {
        String contextCommandId = personCommand.getId();
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(command, atLeastOnce()).executeUndoNested(contextCaptor.capture());
        boolean isPerson = contextCaptor.getAllValues().stream()
                .anyMatch(context -> contextCommandId.equals(context.getCommand().getId()));
        assertThat(isPerson).isTrue();
        contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand).undoCommand(contextCaptor.capture());
        assertContextOf(contextCaptor.getValue(), personCommand);
        verify(personCommand).executeUndo(contextCaptor.getValue());
    }

    private void assertContextOf(Context<?> context, RootCommand<?> command) {
        assertThat(context.getCommand().getId()).isEqualTo(command.getId());
    }

    private void verifyProfileUndoCommand() {
        String contextCommandId = profileCommand.getId();
        ArgumentCaptor<Context<Optional<PrincipalProfile>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(command, atLeastOnce()).executeUndoNested(contextCaptor.capture());
        boolean isProfile = contextCaptor.getAllValues().stream()
                .anyMatch(context -> contextCommandId.equals(context.getCommand().getId()));
        assertThat(isProfile).isTrue();
        contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).undoCommand(contextCaptor.capture());
        assertContextOf(contextCaptor.getValue(), profileCommand);
        verify(profileCommand).executeUndo(contextCaptor.getValue());
    }

    private void verifyPersonDoCommand(int times) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand, times(times)).doCommand(contextCaptor.capture());
        contextCaptor.getAllValues().forEach(context -> verify(personCommand).executeDo(context));
    }

    private void verifyProfileDoCommand(int times) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand, times(times)).doCommand(contextCaptor.capture());
        contextCaptor.getAllValues().forEach(context -> verify(profileCommand).executeDo(context));
    }
}
