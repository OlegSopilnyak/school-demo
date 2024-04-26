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
    StudentRepository studentRepository;
    @SpyBean
    @Autowired
    CourseRepository courseRepository;
    @SpyBean
    @Autowired
    DeleteStudentCommand command;

    @AfterEach
    void tearDown() {
        reset(command);
        reset(persistence);
        reset(studentRepository);
        reset(courseRepository);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(courseRepository).isNotNull();
        assertThat(studentRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentFound() {
        Long id = persistStudent().getId();
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
        verify(studentRepository).findById(id);
        verify(studentRepository).deleteById(id);
        verify(studentRepository).flush();
        assertThat(studentRepository.findById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = persistStudent().getId();
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
        verify(studentRepository).findById(id);
        verify(studentRepository, never()).deleteById(anyLong());
        verify(studentRepository, never()).flush();
        assertThat(studentRepository.findById(id)).isNotEmpty();
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
        verify(studentRepository).findById(id);
        verify(persistence, never()).toEntity(any(Student.class));
        verify(persistence, never()).deleteStudent(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_StudentHasCourse() {
        Student student = persistStudent();
        Course course = persistCourse();
        persistence.link(student, course);
        assertThat(persistence.findStudentById(student.getId()).orElseThrow().getCourses()).contains(course);
        reset(persistence, studentRepository);
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
        verify(studentRepository).findById(id);
        assertThat(studentRepository.findById(id)).isNotEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_RestoreStudent() {
        Student student = persistStudent();
        Long id = student.getId();
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter(persistence.toEntity(student));
        studentRepository.deleteById(id);
        assertThat(studentRepository.findAll()).isEmpty();

        command.undoCommand(context);

        assertThat(studentRepository.findAll()).isNotEmpty();
        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(student);
        verify(studentRepository).saveAndFlush((StudentEntity) student);
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
        verify(persistence, never()).save(any(Student.class));
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
        verify(persistence, never()).save(any(Student.class));
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

    private Course persistCourse() {
        try {
            Course course = makeCourse(0);
            Course entity = persistence.save(course).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            assertThat(courseRepository.findById(id)).isNotEmpty();
            return persistence.toEntity(entity);
        } finally {
            reset(persistence, studentRepository);
        }
    }
}