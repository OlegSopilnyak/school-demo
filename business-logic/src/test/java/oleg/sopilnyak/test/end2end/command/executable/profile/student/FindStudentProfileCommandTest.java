package oleg.sopilnyak.test.end2end.command.executable.profile.student;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class FindStudentProfileCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    ProfilePersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    @Qualifier("profileStudentFind")
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
    void shouldDoCommand_EntityFound() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        Context<Optional<StudentProfile>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertProfilesEquals(profile, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldDoCommand_EntityNotFound() {
        Long id = 815L;
        Context<Optional<StudentProfile>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        long id = 816L;
        Context<Optional<StudentProfile>> context = command.createContext(Input.of("" + id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findStudentProfileById(anyLong());
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        StudentProfile profile = persistStudentProfile();
        Long id = profile.getId();
        doThrow(RuntimeException.class).when(persistence).findProfileById(id);
        Context<Optional<StudentProfile>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        Long id = 818L;
        Context<Optional<StudentProfile>> context = command.createContext(Input.of(id));
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }

    // private methods
    private StudentProfile findStudentProfileById(long id) {
        return findEntity(StudentProfileEntity.class, id);
    }

    private StudentProfile persist(StudentProfile newInstance) {
        StudentProfileEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
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