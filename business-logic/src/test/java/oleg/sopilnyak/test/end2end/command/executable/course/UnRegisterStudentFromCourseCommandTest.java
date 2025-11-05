package oleg.sopilnyak.test.end2end.command.executable.course;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
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
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
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
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"
})
class UnRegisterStudentFromCourseCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    EducationPersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    @Qualifier("courseUnRegisterStudent")
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
    void shouldDoCommand_Linked() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        assertThat(link(studentId, courseId)).isTrue();
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
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
    void shouldNotDoCommand_ExceptionThrown() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        assertThat(link(studentId, courseId)).isTrue();
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistence).unLink(student.getOriginal(), course.getOriginal());
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence).unLink(any(StudentEntity.class), any(CourseEntity.class));
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        assertThat(unlink(studentId, courseId)).isTrue();
    }

    @Test
    void shouldUndoCommand_LinkedParameter() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        assertThat(findStudentById(studentId).getCourses()).isEmpty();
        assertThat(findCourseById(courseId).getStudents()).isEmpty();
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
        assertThat(unlink(studentId, courseId)).isTrue();
    }

    @Test
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
    void shouldNotUndoCommand_ExceptionThrown() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        assertThat(findStudentById(studentId).getCourses()).isEmpty();
        assertThat(findCourseById(courseId).getStudents()).isEmpty();
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(input);
        }
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistence).link(student.getOriginal(), course.getOriginal());

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).link(student.getOriginal(), course.getOriginal());
        assertThat(findStudentById(studentId).getCourses()).isEmpty();
        assertThat(findCourseById(courseId).getStudents()).isEmpty();
    }

    // private methods
    private CoursePayload persistCourse(int order) {
        try {
            Course course = persist(makeClearCourse(order));
            return payloadMapper.toPayload(findCourseById(course.getId()));
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

    private Course findCourseById(Long id) {
        return findEntity(CourseEntity.class, id, e -> e.getStudentSet().size());
    }

    private Student findStudentById(Long id) {
        return findEntity(StudentEntity.class, id, student -> student.getCourseSet().size());
    }

    private StudentPayload persistStudent(int order) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            Student source = makeClearStudent(order);
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
    private StudentPayload persistStudent() {
        return persistStudent(0);
    }

    private CoursePayload persistCourse() {
        return persistCourse(0);
    }
}