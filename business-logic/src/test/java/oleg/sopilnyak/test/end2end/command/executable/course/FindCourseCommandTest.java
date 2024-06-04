package oleg.sopilnyak.test.end2end.command.executable.course;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.course.FindCourseCommand;
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
@ContextConfiguration(classes = {PersistenceConfiguration.class, FindCourseCommand.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class FindCourseCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    CoursesPersistenceFacade persistence;
    @SpyBean
    @Autowired
    FindCourseCommand command;

    @AfterEach
    void tearDown() {
        reset(command);
        reset(persistence);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_CourseFound() {
        Course course = persist();
        Long id = course.getId();
        Context<Optional<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand_CourseNotFound() {
        Long id = 202L;
        Context<Optional<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 204L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findCourseById(id);
        Context<Optional<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteCommandUndoCommand() {
        Context<Optional<Course>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }

    // private methods
    private Course persist() {
        try {
            Course course = makeCourse(0);
            Course entity = persistence.save(course).orElse(null);
            assertThat(entity).isNotNull();
            long id = entity.getId();
            Optional<Course> dbCourse = persistence.findCourseById(id);
            assertCourseEquals(dbCourse.orElseThrow(), course, false);
            assertThat(dbCourse).contains(entity);
//            return persistence.toEntity(entity);
            return entity;
        } finally {
            reset(persistence);
        }
    }
}