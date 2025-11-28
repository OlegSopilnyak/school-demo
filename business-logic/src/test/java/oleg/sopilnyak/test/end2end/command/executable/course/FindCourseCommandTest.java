package oleg.sopilnyak.test.end2end.command.executable.course;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class FindCourseCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    CoursesPersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    @Qualifier("courseFind")
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
    void shouldDoCommand_CourseFound() {
        Course course = persist();
        Long id = course.getId();
        Context<Optional<Course>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertCourseEquals(course, context.getResult().orElseThrow().orElseThrow());
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    void shouldDoCommand_CourseNotFound() {
        Long id = 202L;
        Context<Optional<Course>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 204L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findCourseById(id);
        Context<Optional<Course>> context = command.createContext(Input.of(id));

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Optional<Course>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }

    // private methods
    private CoursePayload persist() {
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
        return Optional.ofNullable(findEntity(CourseEntity.class, id, e -> e.getStudentSet().size()));
    }
}