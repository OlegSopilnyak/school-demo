package oleg.sopilnyak.test.end2end.command.executable.profile.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
@SuppressWarnings("unchecked")
class DeleteStudentProfileCommandTest extends MysqlTestModelFactory {
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
    @Qualifier("profileStudentDelete")
    StudentProfileCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(StudentProfileEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    void shouldDoCommand_ProfileExists() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        StudentProfile exists = findStudentProfileById(id);
        assertThat(exists).isNotNull();
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getException()).isNull();
        assertThat(context.getResult()).contains(true);
        assertProfilesEquals(profile, context.<StudentProfile>getUndoParameter().value(), false);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(exists);
        verify(payloadMapper).toPayload(exists);
        verify(persistence).deleteProfileById(id);
        assertThat(findStudentProfileById(id)).isNull();
    }

    @Test
    void shouldNotDoCommand_NoProfile() {
        long id = 415L;
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(StudentProfile.class));
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() throws ProfileNotFoundException {
        Context<Boolean> context = command.createContext(Input.of("id"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findProfileById(anyLong());
    }

    @Test
    void shouldNotDoCommand_NullParameter() throws ProfileNotFoundException {
        Context<Boolean> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong input parameter value");
        verify(command).executeDo(context);
        verify(persistence, never()).findProfileById(anyLong());
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() throws ProfileNotFoundException {
        StudentProfilePayload profile = persistStudentProfile();
        long id = profile.getId();
        doThrow(new UnsupportedOperationException()).when(persistence).deleteProfileById(id);
        Context<Boolean> context = command.createContext(Input.of(id));

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile.getOriginal());
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldUndoCommand_UndoProfileExists() {
        StudentProfile profile = persistStudentProfile();
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldUndoCommand_WrongUndoCommandParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("input"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'StudentProfile' value:[input]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    void shouldUndoCommand_NullUndoCommandParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.empty());
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage())
                .isEqualTo("Wrong input parameter value (cannot be null or empty).");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        StudentProfile profile = persistStudentProfile();
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(profile));
        }
        doThrow(new UnsupportedOperationException()).when(persistence).saveProfile(profile);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
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
}