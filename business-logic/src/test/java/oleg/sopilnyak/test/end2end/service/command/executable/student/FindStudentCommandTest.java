package oleg.sopilnyak.test.end2end.service.command.executable.student;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.student.FindStudentCommand;
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

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class FindStudentCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    StudentsPersistenceFacade persistence;
    @SpyBean
    @Autowired
    StudentRepository repository;
    @SpyBean
    @Autowired
    FindStudentCommand command;

    @AfterEach
    void tearDown() {
        reset(command);
        reset(persistence);
        reset(repository);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentNotFound() {
        Long id = 106L;
        Context<Optional<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(repository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentFound() {
        Student student = persist();
        Long id = student.getId();
        Context<Optional<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertThat(result).contains(student);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(repository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = persist().getId();
        RuntimeException cannotExecute = new RuntimeException();
        doThrow(cannotExecute).when(persistence).findStudentById(id);
        Context<Optional<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(repository, never()).findById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        Student student = makeStudent(0);
        Student entity = persistence.save(student).orElse(null);
        assertThat(entity).isNotNull();
        long id = entity.getId();
        assertThat(repository.findById(id)).isNotEmpty();
        reset(persistence, repository);
        return entity;
    }
}