package oleg.sopilnyak.test.end2end.command.executable.profile.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdatePrincipalProfileCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class CreateOrUpdatePrincipalProfileCommandTest extends MysqlTestModelFactory {
    @Autowired
    EntityMapper entityMapper;
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    PrincipalProfileCommand command;

    @BeforeEach
    void setUp() {
        deleteEntities(PrincipalProfileEntity.class);
    }

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(PrincipalProfileEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    void shouldDoCommand_UpdateProfile() {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<PrincipalProfile>getUndoParameter().value()).isEqualTo(profile);
        assertProfilesEquals(profile, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(payloadMapper, times(2)).toPayload(any(PrincipalProfileEntity.class));
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldDoCommand_CreateProfile() {
        PrincipalProfile profile = makePrincipalProfile(null);
        if (profile instanceof FakePrincipalProfile fakeProfile) {
            fakeProfile.setLogin(fakeProfile.getLogin() + "-> 1");
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Input<PrincipalProfile> input = (Input<PrincipalProfile>) Input.of(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Optional<PrincipalProfile> doResult = context.getResult().orElseThrow();
        assertProfilesEquals(doResult.orElseThrow(), profile, false);
        assertThat(context.getUndoParameter().value()).isEqualTo(doResult.get().getId());
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
        verify(persistence).saveProfile(input.value());
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<PrincipalProfile>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_IncompatibleProfileType() {
        StudentProfile profile = persistStudentProfile();
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");

        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(profile.getId());
        verify(persistence).findProfileById(profile.getId());
        verify(persistence, never()).save(any(PrincipalProfile.class));
    }

    @Test
    void shouldNotDoCommand_ProfileNotFound() {
        Long id = 701L;
        PrincipalProfile profile = makePrincipalProfile(id);
        if (profile instanceof FakePrincipalProfile fakeProfile) {
            fakeProfile.setLogin(fakeProfile.getLogin() + "-> 1");
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(PrincipalProfile.class));
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        doThrow(RuntimeException.class).when(persistence).findProfileById(id);
        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(PrincipalProfile.class));
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        PrincipalProfile profile = makePrincipalProfile(null);
        if (profile instanceof FakePrincipalProfile fakeProfile) {
            fakeProfile.setLogin(fakeProfile.getLogin() + "-> 1");
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Input<PrincipalProfile> input = (Input<PrincipalProfile>) Input.of(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext(input);

        doThrow(RuntimeException.class).when(persistence).saveProfile(input.value());
        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
        verify(persistence).saveProfile(input.value());
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        PrincipalProfile exists = persistence.findPrincipalProfileById(id).orElseThrow();
        reset(persistence);
        doThrow(RuntimeException.class).when(persistence).saveProfile(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(profile));

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(exists);
        verify(payloadMapper).toPayload(exists);
        verify(persistence, times(2)).save(profile);
        verify(persistence, times(2)).saveProfile(profile);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of("input"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        assertThat(context.getException().getMessage()).contains("cannot be cast to class oleg.sopilnyak.test.school.common.model.PersonProfile");
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<PrincipalProfile>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).contains("Wrong input parameter value");
        verify(command).executeDo(context);
    }

    @Test
    void shouldUndoCommand_DeleteCreated() {
        Long id = persistPrincipalProfile().getId();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(persistence.findPrincipalProfileById(id)).isEmpty();
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldUndoCommand_RestoreUpdated() {
        PrincipalProfile profile = persistPrincipalProfile();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<PrincipalProfile>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Wrong input parameter value null");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("param"));
        }
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Long' value:[param]");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteByIdExceptionThrown() throws ProfileNotFoundException {
        PrincipalProfile profile = persistPrincipalProfile();
        Long id = profile.getId();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteProfileById(id);

        assertThrows(UnexpectedRollbackException.class, () -> command.undoCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(persistence.findPrincipalProfileById(id)).isPresent();
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotUndoCommand_SaveProfileExceptionThrown() {
        PrincipalProfile profile = persistPrincipalProfile();
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }
        context.setState(Context.State.DONE);

        doThrow(new RuntimeException()).when(persistence).saveProfile(profile);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    // private methods
    private PrincipalProfile persistPrincipalProfile() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            PrincipalProfile profile = makePrincipalProfile(null);
            if (profile instanceof FakePrincipalProfile fakeProfile) {
                fakeProfile.setLogin(fakeProfile.getLogin() + "->0");
                fakeProfile.setEmail(fakeProfile.getEmail() + ".site0");
            }
            PrincipalProfileEntity entity = entityMapper.toEntity(profile);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(PrincipalProfileEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
            em.close();
        }
    }

    private StudentProfile persistStudentProfile() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            StudentProfile profile = makeStudentProfile(null);
            if (profile instanceof FakeStudentsProfile fakeProfile) {
                fakeProfile.setEmail(fakeProfile.getEmail() + ".site0");
            }
            StudentProfileEntity entity = entityMapper.toEntity(profile);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(StudentProfileEntity.class, entity.getId()));
        } finally {
            reset(persistence, payloadMapper);
            em.close();
        }
    }
}