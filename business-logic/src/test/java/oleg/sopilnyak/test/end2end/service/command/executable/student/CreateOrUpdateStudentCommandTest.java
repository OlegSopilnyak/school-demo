package oleg.sopilnyak.test.end2end.service.command.executable.student;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CreateOrUpdateStudentCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    StudentsPersistenceFacade persistence;
    @SpyBean
    @Autowired
    StudentRepository studentRepository;
    @SpyBean
    @Autowired
    CreateOrUpdateStudentCommand command;
//    @Mock
//    Student instance;

    @AfterEach
    void tearDown() {
        reset(command);
        reset(persistence);
        reset(studentRepository);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(studentRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_CreateStudent() {
        Student student = makeClearStudent(1);
        Context<Optional<Student>> context = command.createContext(student);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertThat(result).isPresent();
        assertStudentEquals(student, result.get(), false);
        assertThat(context.getUndoParameter()).isEqualTo(result.get().getId());
        verify(command).executeDo(context);
        verify(persistence).save(student);
        verify(studentRepository).saveAndFlush(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_UpdateStudent() {
        Student student = persistStudent();
        Student studentUpdated = persistence.toEntity(student);
        if (studentUpdated instanceof StudentEntity updated) {
            updated.setFirstName(student.getFirstName() + "-updated");
        }
        assertThat(studentRepository.findAll()).hasSize(1);
        Context<Optional<Student>> context = command.createContext(studentUpdated);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(student);
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertStudentEquals(studentUpdated, result.get(), false);
        verify(command).executeDo(context);
        assertThat(studentRepository.findAll()).hasSize(1);
        verify(persistence).toEntity(studentUpdated);
        verify(persistence).save(studentUpdated);
        verify(studentRepository).saveAndFlush((StudentEntity) studentUpdated);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext("instance");

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).save(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_CreateExceptionThrown() {
        Student student = makeClearStudent(1);
        RuntimeException cannotExecute = new RuntimeException("Cannot create");
        doThrow(cannotExecute).when(persistence).save(student);
        Context<Optional<Student>> context = command.createContext(student);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).save(student);
        verify(studentRepository, never()).saveAndFlush(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Student student = persistStudent();
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistence).save(student);
        Context<Optional<Student>> context = command.createContext(student);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(student);
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).toEntity(student);
        verify(persistence, times(2)).save(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_DeleteStudent() {
        Long id = persistStudent().getId();
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
        verify(studentRepository).deleteById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_RestoreStudent() {
        Student student = persistStudent();
        assertThat(studentRepository.findAll()).hasSize(1);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(student);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(studentRepository.findAll()).hasSize(1);
        verify(command).executeUndo(context);
        verify(persistence).save(student);
        verify(studentRepository).saveAndFlush(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("instance");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_DeleteThrown() {
        Long id = 111L;
        doThrow(RuntimeException.class).when(persistence).deleteStudent(id);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
        verify(studentRepository, never()).deleteById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_RestoreThrown() {
        Student student = persistStudent();
        doThrow(RuntimeException.class).when(persistence).save(student);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(student);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(student);
    }

    // private methods
    private Student persistStudent() {
        try {
            Student student = makeStudent(0);
            Student entity = persistence.save(student).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            assertThat(studentRepository.findById(id)).isNotEmpty();
            return persistence.toEntity(entity);
        } finally {
            reset(persistence, studentRepository);
        }
    }
}