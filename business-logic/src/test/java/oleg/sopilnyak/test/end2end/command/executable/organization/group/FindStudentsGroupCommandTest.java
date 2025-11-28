package oleg.sopilnyak.test.end2end.command.executable.organization.group;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, FindStudentsGroupCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class FindStudentsGroupCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    StudentsGroupPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    StudentsGroupCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(CourseEntity.class);
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentsGroupEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
    }

    @Test
    void shouldDoCommand_EntityExists() {
        StudentsGroup entity = persistClear();
        Long id = entity.getId();
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        assertThat(context.getResult().orElseThrow()).contains(payloadMapper.toPayload(findStudentsGroupById(id)));
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        long id = 521L;
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        long id = 522L;
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(id));
        doThrow(RuntimeException.class).when(persistence).findStudentsGroupById(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        long id = 523L;
        Context<Optional<StudentsGroup>> context = command.createContext(Input.of(id));
        context.setState(DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(persistClear()));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }

    //private methods
    private StudentsGroup findStudentsGroupById(Long id) {
        return findEntity(StudentsGroupEntity.class, id, entity -> entity.getStudentEntitySet().size());
    }

    private StudentsGroup persistClear() {
        StudentsGroup source = makeCleanStudentsGroup(10);
        if (source instanceof FakeStudentsGroup fake) {
            fake.setStudents(List.of());
            fake.setLeader(null);
        }
        return persist(source);
    }

    private StudentsGroup persist(StudentsGroup source) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            StudentsGroupEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(StudentsGroupEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
            em.close();
        }
    }
}