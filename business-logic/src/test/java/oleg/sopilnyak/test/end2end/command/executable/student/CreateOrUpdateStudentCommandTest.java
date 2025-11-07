package oleg.sopilnyak.test.end2end.command.executable.student;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
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
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
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
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateStudentCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class CreateOrUpdateStudentCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    StudentsPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    @Qualifier("studentUpdate")
    StudentCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(StudentEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    void shouldDoCommand_CreateStudent() {
        Student student = makeClearStudent(1);
        Context<Optional<Student>> context = command.createContext(Input.of(student));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = context.getResult().orElseThrow();
        assertThat(result).isPresent();
        assertStudentEquals(student, result.get(), false);
        assertThat(context.getUndoParameter().value()).isEqualTo(result.get().getId());
        verify(command).executeDo(context);
        verify(persistence).save(payloadMapper.toPayload(student));
    }

    @Test
    void shouldDoCommand_UpdateStudent() {
        Student student = persistStudent();
        Student studentUpdated = payloadMapper.toPayload(student);
        if (studentUpdated instanceof StudentPayload updated) {
            updated.setFirstName(student.getFirstName() + "-updated");
        }
        Context<Optional<Student>> context = command.createContext(Input.of(studentUpdated));
        reset(payloadMapper);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().value()).isEqualTo(student);
        assertStudentEquals(studentUpdated, context.getResult().orElseThrow().orElseThrow(), false);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(student.getId());
        verify(payloadMapper, times(2)).toPayload(any(StudentEntity.class));
        verify(persistence).save(studentUpdated);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext(Input.of("instance"));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).save(any(StudentEntity.class));
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).save(any(StudentEntity.class));
    }

    @Test
    void shouldNotDoCommand_CreateExceptionThrown() {
        Student student = makeClearStudent(1);
        StudentPayload payload = payloadMapper.toPayload(student);
        reset(payloadMapper);
        Context<Optional<Student>> context = command.createContext(Input.of(student));

        String errorMessage = "Cannot create";
        RuntimeException cannotExecute = new RuntimeException(errorMessage);
        doThrow(cannotExecute).when(persistence).save(payload);

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(cannotExecute);
        assertThat(context.getException().getMessage()).isSameAs(errorMessage);
        verify(command).executeDo(context);
        verify(persistence).save(payload);
    }

    @Test
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Student student = persistStudent();
        Context<Optional<Student>> context = command.createContext(Input.of(student));

        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistence).save(student);
        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter().value()).isEqualTo(student);
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence, times(2)).save(student);
    }

    @Test
    void shouldUndoCommand_DeleteStudent() {
        Long id = persistStudent().getId();
        assertThat(isEmpty(StudentEntity.class)).isFalse();
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(isEmpty(StudentEntity.class)).isTrue();
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldUndoCommand_RestoreStudent() {
        Student student = persistStudent();
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(student));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(student);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("instance"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Long' value:[instance]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(StudentEntity.class));
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.empty());
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(StudentEntity.class));
    }

    @Test
    void shouldNotUndoCommand_DeleteThrown() {
        Long id = 111L;
        doThrow(RuntimeException.class).when(persistence).deleteStudent(id);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldNotUndoCommand_RestoreThrown() {
        Student student = persistStudent();
        doThrow(RuntimeException.class).when(persistence).save(student);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(student));
        }

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(student);
    }

    // private methods
    private StudentPayload persistStudent() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            Student source = makeClearStudent(0);
            StudentEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(StudentEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
            em.close();
        }
    }
}