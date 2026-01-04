package oleg.sopilnyak.test.end2end.command.executable.profile.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
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
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
class CreateOrUpdateStudentProfileCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    ProfilePersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    @Qualifier("profileStudentUpdate")
    StudentProfileCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(StudentProfileEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    void shouldDoCommand_UpdateProfile() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        Context<Optional<StudentProfile>> context = spy(command.createContext(Input.of(profile)));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().value()).isEqualTo(profile);
        assertProfilesEquals(profile, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldDoCommand_CreateProfile() {
        StudentProfile profile = makeStudentProfile(null);
        if (profile instanceof FakeStudentsProfile fakeProfile) {
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Input<StudentProfile> input = (Input<StudentProfile>) Input.of(profile);
        Context<Optional<StudentProfile>> context = spy(command.createContext(input));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Optional<StudentProfile> doResult = context.getResult().orElseThrow();
        assertProfilesEquals(doResult.orElseThrow(), profile, false);
        assertThat(context.getUndoParameter().value()).isEqualTo(doResult.get().getId());
        verify(command).executeDo(context);
        verify(persistence).save(input.value());
        verify(persistence).saveProfile(input.value());
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<StudentProfile>> context = spy(command.createContext());

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_IncompatibleProfileType() {
        PrincipalProfile profile = persistPrincipalProfile();
        Context<Optional<StudentProfile>> context = spy(command.createContext(Input.of(profile)));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(profile.getId());
        verify(persistence).findProfileById(profile.getId());
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    void shouldNotDoCommand_ProfileNotFound() {
        Long id = 801L;
        StudentProfile profile = makeStudentProfile(id);
        if (profile instanceof FakeStudentsProfile fakeProfile) {
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Context<Optional<StudentProfile>> context = spy(command.createContext(Input.of(profile)));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        doThrow(RuntimeException.class).when(persistence).findProfileById(id);
        Context<Optional<StudentProfile>> context = spy(command.createContext(Input.of(profile)));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        StudentProfile profile = makeStudentProfile(null);
        if (profile instanceof FakeStudentsProfile fakeProfile) {
            fakeProfile.setEmail(fakeProfile.getEmail() + ".site1");
        }
        Input<StudentProfile> input = (Input<StudentProfile>) Input.of(profile);
        Context<Optional<StudentProfile>> context = spy(command.createContext(Input.of(input)));

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
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        StudentProfile exists = persistence.findStudentProfileById(id).orElseThrow();
        reset(persistence);
        doThrow(RuntimeException.class).when(persistence).saveProfile(profile);
        Context<Optional<StudentProfile>> context = spy(command.createContext(Input.of(profile)));

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(exists);
        verify(payloadMapper).toPayload(exists);
        verify(persistence, times(2)).save(profile);
        verify(persistence, times(2)).saveProfile(profile);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<StudentProfile>> context = spy(command.createContext(Input.of("input")));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        assertThat(context.getException().getMessage()).contains("cannot be cast to class oleg.sopilnyak.test.school.common.model.PersonProfile");
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<StudentProfile>> context = spy(command.createContext(null));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).contains("Wrong input parameter value");
        verify(command).executeDo(context);
    }

    @Test
    void shouldUndoCommand_DeleteCreated() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        Context<Optional<StudentProfile>> context = spy(command.createContext());
        context.setState(Context.State.WORK);
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldUndoCommand_RestoreUpdated() {
        StudentProfile profile = persistStudentProfile();
        Context<Optional<StudentProfile>> context = spy(command.createContext());
        context.setState(Context.State.WORK);
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<StudentProfile>> context = spy(command.createContext());

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<StudentProfile>> context = spy(command.createContext());
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage())
                .isEqualTo("Wrong input parameter value (cannot be null or empty).");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<StudentProfile>> context = spy(command.createContext());
        context.setState(Context.State.WORK);
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("param"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Long' value:[param]");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteByIdExceptionThrown() throws ProfileNotFoundException {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        Context<Optional<StudentProfile>> context = spy(command.createContext());
        context.setState(Context.State.WORK);
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        doThrow(new RuntimeException()).when(persistence).deleteProfileById(id);

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotUndoCommand_SaveProfileExceptionThrown() {
        StudentProfile profile = persistStudentProfile();
        Context<Optional<StudentProfile>> context = spy(command.createContext());
        context.setState(Context.State.WORK);
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }
        doCallRealMethod().when(persistence).save(profile);
        doThrow(new RuntimeException()).when(persistence).saveProfile(profile);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    // private methods
    private StudentProfile findStudentProfileById(long id) {
        return findEntity(StudentProfileEntity.class, id);
    }

    private StudentProfile persist(StudentProfile newInstance) {
        StudentProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private StudentProfilePayload persistStudentProfile() {
        try {
            StudentProfile profile = makeStudentProfile(null);
            if (profile instanceof FakeStudentsProfile fakeProfile) {
                fakeProfile.setEmail(fakeProfile.getEmail() + ".site.0");
            }
            StudentProfile entity = persist(profile);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            StudentProfile dbProfile = findStudentProfileById(id);
            assertProfilesEquals(dbProfile, profile, false);
            assertThat(dbProfile).isEqualTo(entity);
            return payloadMapper.toPayload(persistence.toEntity(entity));
        } finally {
            reset(persistence, payloadMapper);
        }
    }

    private PrincipalProfilePayload persistPrincipalProfile() {
        try {
            PrincipalProfile profile = makePrincipalProfile(null);
            if (profile instanceof FakePrincipalProfile fakeProfile) {
                fakeProfile.setLogin(fakeProfile.getLogin() + "->0");
                fakeProfile.setEmail(fakeProfile.getEmail() + ".site-0");
            }
            PrincipalProfile entity = persist(profile);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            PrincipalProfile dbProfile = findPrincipalProfileById(id);
            assertProfilesEquals(dbProfile, profile, false);
            assertThat(dbProfile).isEqualTo(entity);
            return payloadMapper.toPayload(persistence.toEntity(entity));
        } finally {
            reset(persistence, payloadMapper);
        }
    }

    private PrincipalProfile findPrincipalProfileById(long id) {
        return findEntity(PrincipalProfileEntity.class, id);
    }

    private PrincipalProfile persist(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }
}