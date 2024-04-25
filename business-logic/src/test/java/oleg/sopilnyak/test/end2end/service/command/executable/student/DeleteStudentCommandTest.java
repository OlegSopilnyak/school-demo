package oleg.sopilnyak.test.end2end.service.command.executable.student;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.repository.CourseRepository;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.student.DeleteStudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class DeleteStudentCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    StudentCourseLinkPersistenceFacade persistence;
    @SpyBean
    @Autowired
    StudentRepository repository;
    @SpyBean
    @Autowired
    CourseRepository courseRepository;
    @SpyBean
    @Autowired
    DeleteStudentCommand command;
    @Mock
    Student instance;

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
    void shouldDoCommand_StudentFound() {
        Long id = persist().getId();
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Boolean result = (Boolean) context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).toEntity(any(Student.class));
        verify(persistence).deleteStudent(id);
        verify(repository).findById(id);
        verify(repository).deleteById(id);
        verify(repository).flush();
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = persist().getId();
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).deleteStudent(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).toEntity(any(Student.class));
        verify(persistence).deleteStudent(id);
        verify(repository).findById(id);
        verify(repository, never()).deleteById(anyLong());
        verify(repository, never()).flush();
        assertThat(repository.findById(id)).isNotEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_StudentNotFound() {
        Long id = 112L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:112 is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(repository).findById(id);
        verify(persistence, never()).toEntity(any(Student.class));
        verify(persistence, never()).deleteStudent(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_StudentHasCourse() {
        Student student = persist();
        Course course = persistCourse();
        persistence.link(student, course);
        assertThat(student.getCourses()).contains(course);
        reset(persistence, repository);
        Long id = student.getId();
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentWithCoursesException.class);
        assertThat(context.getException().getMessage()).startsWith("Student with ID:").endsWith(" has registered courses.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).toEntity(any(Student.class));
        verify(persistence, never()).deleteStudent(id);
        verify(repository).findById(id);
        assertThat(repository.findById(id)).isNotEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_RestoreStudent() {
        Student student = persist();
        Long id = student.getId();
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter(persistence.toEntity(student));
        repository.deleteById(id);
        assertThat(repository.findAll()).isEmpty();

        command.undoCommand(context);

        assertThat(repository.findAll()).isNotEmpty();
        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(student);
        verify(repository).saveAndFlush((StudentEntity) student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter("instance");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(instance);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Cannot invoke \"Object.toString()\" because \"parameter\" is null");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(instance);
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

    private Course persistCourse() {
        Course course = makeCourse(0);
        Course entity = persistence.save(course).orElse(null);
        assertThat(entity).isNotNull();
        long id = entity.getId();
        assertThat(courseRepository.findById(id)).isNotEmpty();
        reset(persistence, courseRepository);
        return entity;
    }
}