package oleg.sopilnyak.test.end2end.command.executable.course;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.course.DeleteCourseCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
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
@ContextConfiguration(classes = {PersistenceConfiguration.class, DeleteCourseCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class DeleteCourseCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    EducationPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    DeleteCourseCommand command;

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
    void shouldDoCommand_CourseFound() {
        Course course = persistCourse();
        Long id = course.getId();
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertCourseEquals(course, context.<Course>getUndoParameter().value(), false);
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().get()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(any(CourseEntity.class));
        verify(persistence).deleteCourse(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_CourseNotFound() {
        Long id = 102L;
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isNull();
        assertThat(context.getException()).isInstanceOf(CourseNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Course with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper, never()).toPayload(any(CourseEntity.class));
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_CourseHasEnrolledStudent() {
        Course course = persistCourse();
        Student student = persistStudent();
        persistence.link(student, course);
        Long id = course.getId();
        assertThat(persistence.findCourseById(id).orElseThrow().getStudents()).contains(student);
        reset(persistence);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isNull();
        assertThat(context.getException()).isInstanceOf(CourseWithStudentsException.class);
        assertThat(context.getException().getMessage()).startsWith("Course with ID:").endsWith(" has enrolled students.");
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(any(CourseEntity.class));
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Course course = persistCourse();
        Long id = course.getId();
        String errorMessage = "Cannot delete course with ID:" + id;
        RuntimeException cannotExecute = new RuntimeException(errorMessage);
        doThrow(cannotExecute).when(persistence).deleteCourse(id);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(cannotExecute);
        assertThat(context.getException().getMessage()).isSameAs(errorMessage);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(any(CourseEntity.class));
        verify(persistence).deleteCourse(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_CourseFound() {
        Course course = persistCourse();
        Context<Boolean> context = command.createContext();
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(course));
        }
//        context.setState(DONE);
//        context.setUndoParameter(course);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of("course"));
        }
//        context.setState(DONE);
//        context.setUndoParameter("course");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Course' value:[course]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Wrong input parameter value null");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(Course.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotUndoCommand_ExceptionThrown() {
        Course course = persistCourse();
        Context<Boolean> context = command.createContext();
        RuntimeException cannotExecute = new RuntimeException("Cannot restore");
        doThrow(cannotExecute).when(persistence).save(course);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(course));
        }
//        context.setState(DONE);
//        context.setUndoParameter(course);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }

    // private methods
    private Student persistStudent() {
        try {
            Student student = makeStudent(0);
            Student entity = persistence.save(student).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<Student> dbStudent = persistence.findStudentById(id);
            assertStudentEquals(dbStudent.orElseThrow(), student, false);
            assertThat(dbStudent).contains(entity);
            return entity;
        } finally {
            reset(persistence);
        }
    }

    private Course persistCourse() {
        try {
            Course course = makeCourse(0);
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
}