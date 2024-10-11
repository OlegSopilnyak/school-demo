package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.PrincipalProfilePayload;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class,
        AuthorityPersonFacadeImpl.class,
        DeletePrincipalProfileCommand.class,
        DeleteAuthorityPersonCommand.class,
        DeleteAuthorityPersonMacroCommand.class,
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
    @SpyBean
    @Autowired
    DeletePrincipalProfileCommand profileCommand;
    @SpyBean
    @Autowired
    DeleteAuthorityPersonCommand personCommand;
    @SpyBean
    @Autowired
    DeleteAuthorityPersonMacroCommand command;

    @BeforeEach
    void setUp() {
        Assertions.setMaxStackTraceElementsDisplayed(1000);
        command.runThreadPoolExecutor();
    }

    @AfterEach
    void tearDown() {
        command.stopThreadPoolExecutor();
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

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter<Void> redoParameter = context.getRedoParameter();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getInput()).isSameAs(personId);
        Context<Void> personContext = redoParameter.getNestedContexts().pop();
        Context<Void> profileContext = redoParameter.getNestedContexts().pop();
        assertThat(personContext.isReady()).isTrue();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(personContext.<Long>getRedoParameter()).isSameAs(personId);
        assertThat(profileContext.<Long>getRedoParameter()).isSameAs(profileId);

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_PersonNotFound() {
        AuthorityPersonPayload person = create(makeCleanAuthorityPerson(3));
        assertThat(persistence.findAuthorityPersonById(person.getId())).isPresent();
        Long personId = 3L;

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand, never()).createContext(any());
        verify(profileCommand).createContextInit();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";

        Context<AuthorityPerson> context = command.createContext(wrongTypeInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.DELETE_BY_ID);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(personCommand, wrongTypeInput);
        verify(personCommand).createContext(wrongTypeInput);

        verify(profileCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(profileCommand, wrongTypeInput);
        verify(command, never()).createPrincipalProfileContext(eq(profileCommand), any());
        verify(profileCommand).createContextInit();
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
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(profileId);

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(profileId);
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
        String errorMessage = "Cannot create nested person context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(personCommand).createContext(personId);

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(profileId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteDoCommand() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(9));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(personId);
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();
        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isEqualTo(profile);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(personContext.<Long>getRedoParameter()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter()).isEqualTo(profileId);

        verifyPersonDoCommand(personContext);
        verifyProfileDoCommand(profileContext);
        assertThat(persistence.findAuthorityPersonById(personId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_PersonNotFound() {
        Long personId = 10L;
        Context<Boolean> context = command.createContext(personId);
        assertThat(context.isReady()).isFalse();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.<Object>getRedoParameter()).isNull();
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
        Context<Boolean> context = command.createContext(personId);
        assertThat(context.isReady()).isTrue();
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyPersonDoCommand(personContext);
        verify(persistence).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(any(AuthorityPersonPayload.class));
        assertThat(persistence.findAuthorityPersonById(personContext.getRedoParameter())).isPresent();

        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));

        assertThat(persistence.findPrincipalProfileById(profileId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileContext.getRedoParameter())).isEmpty();
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
        Context<Boolean> context = command.createContext(personId);
        String errorMessage = "Cannot delete person";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteAuthorityPerson(personId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(personContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter()).isNull();
        assertThat(personContext.getResult()).isEmpty();

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        PrincipalProfilePayload savedProfile = profileContext.getUndoParameter();
        assertThat(savedProfile.getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyPersonDoCommand(personContext);
        verify(persistence, times(2)).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findPrincipalProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(savedProfile);

        verify(personCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        assertThat(persistence.findAuthorityPersonById(personContext.getRedoParameter())).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileContext.getRedoParameter())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(15));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        reset(persistence);
        Context<Boolean> context = command.createContext(personId);
        String errorMessage = "Cannot delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyPersonDoCommand(personContext);
        verify(persistence, times(2)).findAuthorityPersonById(personId);
        verify(persistence).deleteAuthorityPerson(personId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findPrincipalProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(personContext.<AuthorityPerson>getUndoParameter());

        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        assertThat(persistence.findAuthorityPersonById(personContext.getRedoParameter())).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileContext.getRedoParameter())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteUndoCommand() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(17));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(personId);
        command.doCommand(context);
        reset(persistence);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(personContext.<AuthorityPerson>getUndoParameter());

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<PrincipalProfile>getUndoParameter());

        assertThat(persistence.findAuthorityPersonById(personContext.getRedoParameter())).isPresent();
        assertThat(persistence.findPrincipalProfileById(profileContext.getRedoParameter())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        AuthorityPersonPayload personPayload = create(makeCleanAuthorityPerson(17));
        Long personId = personPayload.getId();
        Long profileId = personPayload.getProfileId();
        AuthorityPerson person = persistence.findAuthorityPersonById(personId).orElseThrow();
        PrincipalProfile profile = persistence.findPrincipalProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(personId);
        command.doCommand(context);
        reset(persistence);
        String errorMessage = "Cannot restore profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(PrincipalProfile.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);

        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();
        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertAuthorityPersonEquals(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal(), person, false);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(any(AuthorityPersonPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<PrincipalProfile>getUndoParameter());

        verifyPersonDoCommand(personContext, 2);
        Long newPersonId = personContext.getRedoParameter();
        verify(persistence).findAuthorityPersonById(newPersonId);
        verify(persistence).deleteAuthorityPerson(newPersonId);

        assertThat(persistence.findAuthorityPersonById(newPersonId)).isEmpty();
        assertThat(persistence.findAuthorityPersonById(personId)).isEmpty();
        assertThat(persistence.findPrincipalProfileById(profileContext.getRedoParameter())).isEmpty();
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
        Context<Boolean> context = command.createContext(personId);
        command.doCommand(context);
        reset(persistence);
        String errorMessage = "Cannot restore person";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(AuthorityPerson.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isSameAs(exception);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertProfilesEquals(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal(), profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));

        verifyPersonUndoCommand(personContext);
        verify(persistence).save(any(AuthorityPersonPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(any(PrincipalProfilePayload.class));

        verifyProfileDoCommand(profileContext, 2);
        Long newProfileId = profileContext.getRedoParameter();
        verify(persistence).findPrincipalProfileById(newProfileId);
        verify(persistence).deleteProfileById(newProfileId);

        assertThat(persistence.findAuthorityPersonById(personContext.getRedoParameter())).isEmpty();
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
        verify(profileCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command, times(i)).doNestedCommand(eq(profileCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand, times(i)).doCommand(nestedContext);
        verify(profileCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyPersonDoCommand(Context<Boolean> nestedContext) {
        verifyPersonDoCommand(nestedContext, 1);
    }

    private void verifyPersonDoCommand(Context<Boolean> nestedContext, int i) {
        verify(personCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command, times(i)).doNestedCommand(eq(personCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(personCommand, times(i)).doCommand(nestedContext);
        verify(personCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyPersonUndoCommand(Context<Boolean> nestedContext) {
        verify(personCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(personCommand, nestedContext);
        verify(personCommand).undoCommand(nestedContext);
        verify(personCommand).executeUndo(nestedContext);
    }

    private void verifyProfileUndoCommand(Context<Boolean> nestedContext) {
        verify(profileCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(profileCommand, nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
    }
}