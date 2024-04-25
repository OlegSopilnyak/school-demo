package oleg.sopilnyak.test.end2end.service.command.executable.student;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.repository.CourseRepository;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.student.FindNotEnrolledStudentsCommand;
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

import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class FindNotEnrolledStudentsCommandTest extends MysqlTestModelFactory {
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
    FindNotEnrolledStudentsCommand command;

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
        assertThat(studentRepository).isNotNull();
        assertThat(courseRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentsNotFound() {
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = (Set<Student>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
        verify(studentRepository).findStudentEntitiesByCourseSetEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentsFound() {
        Student student = persistStudent();
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = (Set<Student>) context.getResult().orElseThrow();
        assertThat(result).isEqualTo(Set.of(student));
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
        verify(studentRepository).findStudentEntitiesByCourseSetEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentsFoundButEnrolled() {
        Student student = persistStudent();
        Course course = persistCourse();
        persistence.link(student, course);
        assertThat(student.getCourses()).contains(course);
        reset(persistence, studentRepository);
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = (Set<Student>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
        verify(studentRepository).findStudentEntitiesByCourseSetEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findNotEnrolledStudents();
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
        verify(studentRepository, never()).findStudentEntitiesByCourseSetEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteCommandUndoCommand() {
        Context<Set<Student>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }

    // private methods
    private Student persistStudent() {
        Student student = makeStudent(0);
        Student entity = persistence.save(student).orElse(null);
        assertThat(entity).isNotNull();
        long id = entity.getId();
        assertThat(studentRepository.findById(id)).isNotEmpty();
        reset(persistence, studentRepository);
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