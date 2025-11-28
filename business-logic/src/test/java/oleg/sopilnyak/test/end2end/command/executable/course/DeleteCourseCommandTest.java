package oleg.sopilnyak.test.end2end.command.executable.course;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
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
class DeleteCourseCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    EducationPersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    @Qualifier("courseDelete")
    CourseCommand command;

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        deleteEntities(CourseEntity.class);
        deleteEntities(StudentEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
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
    void shouldNotDoCommand_CourseNotFound() {
        Long id = 102L;
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Course with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper, never()).toPayload(any(CourseEntity.class));
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotDoCommand_CourseHasEnrolledStudent() {
        Course course = persistCourse();
        Student student = persistStudent();
        Long studentId = student.getId();
        Long courseId = course.getId();
        assertThat(link(studentId, courseId)).isTrue();
        assertThat(findCourseById(courseId).orElseThrow().getStudents()).contains(findStudentById(studentId).orElseThrow());
        Context<Boolean> context = command.createContext(Input.of(courseId));

        command.doCommand(context);

        assertThat(unlink(studentId, courseId)).isTrue();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseWithStudentsException.class);
        assertThat(context.getException().getMessage()).startsWith("Course with ID:").endsWith(" has enrolled students.");
        verify(command).executeDo(context);
        verify(persistence).findCourseById(courseId);
        verify(payloadMapper).toPayload(any(CourseEntity.class));
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Course course = persistCourse();
        Long id = course.getId();
        String errorMessage = "Cannot delete course with ID:" + id;
        RuntimeException cannotExecute = new RuntimeException(errorMessage);
        doThrow(cannotExecute).when(persistence).deleteCourse(id);
        Context<Boolean> context = command.createContext(Input.of(id));

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(cannotExecute);
        assertThat(context.getException().getMessage()).isSameAs(errorMessage);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(any(CourseEntity.class));
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldUndoCommand_CourseFound() {
        Course course = persistCourse();
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(course));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of("course"));
        }

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Course' value:[course]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(Course.class));
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage())
                .isEqualTo("Wrong input parameter value (cannot be null or empty).");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(any(Course.class));
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        Course course = persistCourse();
        Context<Boolean> context = command.createContext();
        RuntimeException cannotExecute = new RuntimeException("Cannot restore");
        doThrow(cannotExecute).when(persistence).save(course);
        context.setState(DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(course));
        }

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
        return Optional.ofNullable(findEntity(CourseEntity.class, id, e -> e.getStudentSet().size()));
    }

    private Optional<Student> findStudentById(Long id) {
        return Optional.ofNullable(findEntity(StudentEntity.class, id, student -> student.getCourseSet().size()));
    }

    private StudentPayload persistStudent() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            Student source = makeClearStudent(0);
            StudentEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(StudentEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
            em.close();
        }
    }

    private boolean link(Long studentId, Long courseId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            StudentEntity student = em.find(StudentEntity.class, studentId);
            CourseEntity course = em.find(CourseEntity.class, courseId);
            if (!student.add(course)) {
                transaction.rollback();
                return false;
            }
            em.merge(student);
            em.flush();
            em.clear();
            transaction.commit();
            return true;
        } finally {
            em.close();
        }
    }

    private boolean unlink(Long studentId, Long courseId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            StudentEntity student = em.find(StudentEntity.class, studentId);
            CourseEntity course = em.find(CourseEntity.class, courseId);
            if (!student.remove(course)) {
                transaction.rollback();
                return false;
            }
            em.merge(student);
            em.flush();
            em.clear();
            transaction.commit();
            return true;
        } finally {
            em.close();
        }
    }
}