package oleg.sopilnyak.test.end2end.command.executable.student;

import static oleg.sopilnyak.test.service.command.type.core.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.education.student.FindStudentCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityTransaction;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, FindStudentCommand.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
@SuppressWarnings("unchecked")
class FindStudentCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    StudentsPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    StudentCommand command;

    @BeforeEach
    void setUp() {
        deleteEntities(StudentProfileEntity.class);
        deleteEntities(StudentEntity.class);
    }

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(StudentProfileEntity.class);
        deleteEntities(StudentEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    void shouldDoCommand_StudentNotFound() {
        Long id = 106L;
        Context<Optional<Student>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
    }

    @Test
    void shouldDoCommand_StudentFound() {
        Student student = persist();
        Long id = student.getId();
        Context<Optional<Student>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertStudentEquals(student, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = persist().getId();
        RuntimeException cannotExecute = new RuntimeException();
        doThrow(cannotExecute).when(persistence).findStudentById(id);
        Context<Optional<Student>> context = command.createContext(Input.of(id));

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
        verify(persistence, never()).findStudentById(anyLong());
    }

    // private methods
    private Student persist() {
        try (var em = entityManagerFactory.createEntityManager()) {
            StudentEntity entity = entityMapper.toEntity(makeClearStudent(0));
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            em.persist(entity);
            em.flush();
            transaction.commit();
            return payloadMapper.toPayload(em.find(StudentEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
        }
    }
}