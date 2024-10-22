package oleg.sopilnyak.test.end2end.command.executable.student;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.EducationPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.student.FindEnrolledStudentsCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
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

import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, FindEnrolledStudentsCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class FindEnrolledStudentsCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    EducationPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    FindEnrolledStudentsCommand command;

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
    void shouldDoCommand_StudentsNotFound() {
        Long id = 210L;
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findEnrolledStudentsByCourseId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentEnrolledToCourse() {
        Student student = persistStudent();
        Course course = persistCourse();
        persistence.link(student, course);
        assertThat(persistence.findStudentById(student.getId()).orElseThrow().getCourses())
                .contains(persistence.findCourseById(course.getId()).orElseThrow());
        Student saved = persistence.findStudentById(student.getId()).orElseThrow();
        reset(persistence);
        Long id = course.getId();
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().orElseThrow()).contains(saved);
        verify(command).executeDo(context);
        verify(persistence).findEnrolledStudentsByCourseId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_StudentNotEnrolled() {
        persistStudent();
        Long id = persistCourse().getId();
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findEnrolledStudentsByCourseId(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 212L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findEnrolledStudentsByCourseId(id);
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findEnrolledStudentsByCourseId(id);
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
        try {
            Student student = makeStudent(0);
            Student entity = persistence.save(student).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            assertThat(persistence.findStudentById(id)).isPresent();
            return payloadMapper.toPayload(entity);
        } finally {
            reset(persistence, payloadMapper);
        }
    }

    private Course persistCourse() {
        try {
            Course course = makeCourse(0);
            Course entity = persistence.save(course).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            assertThat(persistence.findCourseById(id)).isPresent();
            return payloadMapper.toPayload(entity);
        } finally {
            reset(persistence, payloadMapper);
        }
    }
}