package oleg.sopilnyak.test.end2end.command.executable.course;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.course.RegisterStudentToCourseCommand;
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
@ContextConfiguration(classes = {PersistenceConfiguration.class, RegisterStudentToCourseCommand.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update",
        "school.courses.maximum.rooms=2", "school.students.maximum.courses=2"
})
@Rollback
class RegisterStudentToCourseCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    RegisterStudentToCourseCommand command;

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
        assertThat(command.getCoursesExceed()).isEqualTo(2);
        assertThat(command.getMaximumRooms()).isEqualTo(2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_LinkStudentWithCourse() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        Context<Boolean> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().value()).isEqualTo(input);
        assertThat(context.getResult().orElseThrow()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence).link(student.getOriginal(), course.getOriginal());
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_AlreadyLinked() {
        Student student = persistStudent();
        Course course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        persistence.link(student, course);

        Student linkedStudent = persistence.findStudentById(studentId).orElseThrow();
        Course linkedCourse = persistence.findCourseById(courseId).orElseThrow();
        assertThat(linkedStudent.getCourses()).contains(linkedCourse);
        assertThat(linkedCourse.getStudents()).contains(linkedStudent);
        reset(persistence);

        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isNull();
        assertThat(context.getResult().orElseThrow()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NoStudent() {
        Long id = 121L;
        Context<Boolean> context = command.createContext(Input.of(id, id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Student with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence, never()).findCourseById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_NoCourse() {
        Long studentId = persistStudent().getId();
        Long id = 122L;
        Context<Boolean> context = command.createContext(Input.of(studentId, id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Course with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_MaximumRooms() {
        Course course = persistCourse();
        Long courseId = course.getId();
        persistence.link(persistStudent(1), course);
        persistence.link(persistStudent(2), course);
        assertThat(persistence.findCourseById(courseId).orElseThrow().getStudents()).hasSize(2);
        reset(persistence);
        Long studentId = persistStudent(3).getId();
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseHasNoRoomException.class);
        assertThat(context.getException().getMessage()).startsWith("Course with ID:").contains(" does not have enough rooms.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_CoursesExceed() {
        Student student = persistStudent();
        Long studentId = student.getId();
        persistence.link(student, persistCourse(1));
        persistence.link(student, persistCourse(2));
        assertThat(persistence.findStudentById(studentId).orElseThrow().getCourses()).hasSize(2);
        reset(persistence);
        Long courseId = persistCourse(3).getId();
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentCoursesExceedException.class);
        assertThat(context.getException().getMessage()).startsWith("Student with ID:").contains(" exceeds maximum courses.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Student student = persistStudent();
        Long studentId = student.getId();
        Course course = persistCourse();
        Long courseId = course.getId();
        RuntimeException cannotExecute = new RuntimeException("Cannot link");
        doThrow(cannotExecute).when(persistence).link(any(Student.class), any(Course.class));
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence).link(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_Linked() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        persistence.link(student, course);
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        reset(persistence);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(studentId, courseId));
        }

        command.undoCommand(context);

        assertThat(persistence.findStudentById(student.getId()).orElseThrow().getCourses()).isEmpty();
        assertThat(persistence.findCourseById(course.getId()).orElseThrow().getStudents()).isEmpty();
        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence).unLink(student.getOriginal(), course.getOriginal());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_NotLinked() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.empty());
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence, never()).unLink(any(Student.class), any(Course.class));
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
        verify(persistence, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_ExceptionThrown() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        persistence.link(student, course);
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        reset(persistence);
        RuntimeException cannotExecute = new RuntimeException("Cannot link");
        doThrow(cannotExecute).when(persistence).unLink(student.getOriginal(), course.getOriginal());
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(input));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        verify(persistence).unLink(student.getOriginal(), course.getOriginal());
    }

    // private methods
    private StudentPayload persistStudent() {
        return persistStudent(0);
    }

    private StudentPayload persistStudent(int order) {
        try {
            StudentProfile profile = persistence.save(makeStudentProfile(null)).orElse(null);
            assertThat(profile).isNotNull();
            Student student = makeStudent(order);
            if (student instanceof FakeStudent fakeStudent) {
                fakeStudent.setProfileId(profile.getId());
            }
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
        return persistCourse(0);
    }

    private CoursePayload persistCourse(int order) {
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