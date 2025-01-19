package oleg.sopilnyak.test.end2end.command.executable.course;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.course.UnRegisterStudentFromCourseCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, UnRegisterStudentFromCourseCommand.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"
})
@Rollback
class UnRegisterStudentFromCourseCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    EducationPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    UnRegisterStudentFromCourseCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_Linked() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        persistence.link(student, course);
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        reset(persistence, payloadMapper);
        Context<Boolean> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        assertThat(context.getUndoParameter().value()).isEqualTo(input);
        assertThat(findStudentById(studentId).getCourses()).isEmpty();
        assertThat(findCourseById(courseId).getStudents()).isEmpty();
        verify(persistence).unLink(student.getOriginal(), course.getOriginal());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NoStudent() {
        Long id = 132L;
        Context<Boolean> context = command.createContext(Input.of(id, id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Student with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence, never()).findCourseById(id);
        verify(persistence, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NoCourse() {
        Long id = persistStudent().getId();
        Context<Boolean> context = command.createContext(Input.of(id, id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Course with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Student student = persistStudent();
        Course course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        persistence.link(student, course);
        assertThat(persistence.findStudentById(studentId).orElseThrow().getCourses())
                .contains(persistence.findCourseById(courseId).orElseThrow());
        assertThat(persistence.findCourseById(courseId).orElseThrow().getStudents())
                .contains(persistence.findStudentById(studentId).orElseThrow());
        reset(persistence);
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistence).unLink(any(StudentEntity.class), any(CourseEntity.class));
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence).unLink(any(StudentEntity.class), any(CourseEntity.class));
        assertThat(context.getUndoParameter()).isNull();
        assertThat(persistence.findStudentById(studentId).orElseThrow().getCourses())
                .contains(persistence.findCourseById(courseId).orElseThrow());
        assertThat(persistence.findCourseById(courseId).orElseThrow().getStudents())
                .contains(persistence.findStudentById(studentId).orElseThrow());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_LinkedParameter() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        assertThat(findStudentById(studentId).getCourses()).isEmpty();
        assertThat(findCourseById(courseId).getStudents()).isEmpty();
        reset(persistence);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(input);
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).link(student.getOriginal(), course.getOriginal());
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_IgnoreParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.empty());
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("null"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_ExceptionThrown() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        assertThat(findStudentById(studentId).getCourses()).isEmpty();
        assertThat(findCourseById(courseId).getStudents()).isEmpty();
        reset(persistence);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(input);
        }

        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistence).link(student.getOriginal(), course.getOriginal());
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).link(student.getOriginal(), course.getOriginal());
        assertThat(findStudentById(studentId).getCourses()).isEmpty();
        assertThat(findCourseById(courseId).getStudents()).isEmpty();
    }

    // private methods
    private StudentPayload persistStudent() {
        int order = 0;
        try {
            Student student = makeStudent(order);
            Student entity = persistence.save(student).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<Student> dbStudent = persistence.findStudentById(id);
            assertStudentEquals(dbStudent.orElseThrow(), student, false);
            assertThat(dbStudent).contains(entity);
            return payloadMapper.toPayload(entity);
        } finally {
            reset(persistence, payloadMapper);
        }
    }

    private CoursePayload persistCourse() {
        int order = 0;
        try {
            Course course = makeCourse(order);
            Course entity = persistence.save(course).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<Course> dbCourse = persistence.findCourseById(id);
            assertCourseEquals(dbCourse.orElseThrow(), course, false);
            assertThat(dbCourse).contains(entity);
            return payloadMapper.toPayload(entity);
        } finally {
            reset(persistence, payloadMapper);
        }
    }

    private Student findStudentById(long id) {
        return persistence.findStudentById(id).orElseThrow();
    }

    private Course findCourseById(long id) {
        return persistence.findCourseById(id).orElseThrow();
    }
}