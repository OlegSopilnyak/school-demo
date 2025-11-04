package oleg.sopilnyak.test.end2end.command.executable.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
//@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateCourseCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class CreateOrUpdateCourseCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    CoursesPersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    @Qualifier("courseUpdate")
    CourseCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(CourseEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    void shouldDoCommand_CreateCourse() {
        Course course = makeClearCourse(1);
        Context<Optional<Course>> context = command.createContext(Input.of(course));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Course result = context.getResult().orElseThrow().orElseThrow();
        assertThat(context.<Long>getUndoParameter().value()).isEqualTo(result.getId());
        assertCourseEquals(course, result, false);
        verify(command).executeDo(context);
        verify(persistence).save(payloadMapper.toPayload(course));
    }

    @Test
    void shouldDoCommand_UpdateCourse() {
        Course course = persistCourse();
        Course courseUpdated = payloadMapper.toPayload(course);
        if (courseUpdated instanceof CoursePayload updated) {
            updated.setName(updated.getName() + "-updated");
        }
        Context<Optional<Course>> context = command.createContext(Input.of(courseUpdated));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Course>getUndoParameter().value()).isEqualTo(course);
        assertCourseEquals(courseUpdated, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findCourseById(course.getId());
        verify(persistence).save(courseUpdated);
        verify(payloadMapper, times(2)).toPayload(any(CourseEntity.class));
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Course>> context = command.createContext(Input.of("course"));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findCourseById(anyLong());
        verify(persistence, never()).save(any(CourseEntity.class));
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findCourseById(anyLong());
        verify(persistence, never()).save(any(CourseEntity.class));
    }

    @Test
    void shouldNotDoCommand_CreateExceptionThrown() {
        Course course = makeClearCourse(1);
        CoursePayload payload = payloadMapper.toPayload(course);
        reset(payloadMapper);
        Context<Optional<Course>> context = command.createContext(Input.of(course));

        RuntimeException cannotExecute = new RuntimeException("Cannot create");
        doThrow(cannotExecute).when(persistence).save(payload);

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).save(payload);
        verify(payloadMapper, never()).toPayload(any(Course.class));
    }

    @Test
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Course course = persistCourse();
        RuntimeException cannotExecute = new RuntimeException("Cannot create");
        doThrow(cannotExecute).when(persistence).save(course);
        Context<Optional<Course>> context = command.createContext(Input.of(course));

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        assertCourseEquals(course, context.<Course>getUndoParameter().value());
        verify(command).executeDo(context);
        verify(persistence).findCourseById(course.getId());
        verify(payloadMapper).toPayload(any(CourseEntity.class));
        verify(persistence, times(2)).save(course);
    }

    @Test
    void shouldUndoCommand_CreateCourse() {
        Long id = persistCourse().getId();
        assertThat(isEmpty(CourseEntity.class)).isFalse();
        Context<Optional<Course>> context = command.createContext();
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(id));
        }

        command.undoCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> isEmpty(CourseEntity.class));

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldUndoCommand_UpdateCourse() {
        Course course = persistCourse();
        Context<Optional<Course>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(course));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertCourseEquals(course, persistence.findCourseById(course.getId()).orElseThrow());
        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Course>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("id"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Long' value:[id]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(CourseEntity.class));
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Optional<Course>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Wrong input parameter value null");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(CourseEntity.class));
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_CreateExceptionThrown() {
        Long id = 104L;
        Context<Optional<Course>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }
        RuntimeException cannotExecute = new RuntimeException("Cannot undo create");
        doThrow(cannotExecute).when(persistence).deleteCourse(id);

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldNotUndoCommand_UpdateExceptionThrown() {
        Course course = persistCourse();
        Context<Optional<Course>> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(course));
        }
        RuntimeException cannotExecute = new RuntimeException("Cannot undo update");
        doThrow(cannotExecute).when(persistence).save(course);

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }

    // private methods
    private Course persistCourse() {
        try {
            Course course = persist(makeClearCourse(0));
            return payloadMapper.toPayload(findCourseById(course.getId()).orElseThrow());
        } finally {
            reset(payloadMapper);
        }
    }

    private Course persist(Course newInstance) {
        CourseEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getStudents().forEach(em::persist);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
        }
    }

    private Optional<Course> findCourseById(Long id) {
        return Optional.ofNullable(findEntity(CourseEntity.class, id, e -> e.getStudents().size()));
    }
}