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
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update",
        "school.courses.maximum.rooms=2", "school.students.maximum.courses=2"
})
class RegisterStudentToCourseCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    @Qualifier("courseRegisterStudent")
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
        assertThat(ReflectionTestUtils.getField(command, "coursesExceed")).isEqualTo(2);
        assertThat(ReflectionTestUtils.getField(command, "maximumRooms")).isEqualTo(2);
    }

    @Test
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
        assertThat(unlink(studentId, courseId)).isTrue();
    }

    @Test
    void shouldDoCommand_AlreadyLinked() {
        Student student = persistStudent();
        Course course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        assertThat(link(studentId, courseId)).isTrue();

        Student linkedStudent = findStudentById(studentId);
        Course linkedCourse = findCourseById(courseId);
        assertThat(linkedStudent.getCourses()).contains(linkedCourse);
        assertThat(linkedCourse.getStudents()).contains(linkedStudent);

        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(unlink(studentId, courseId)).isTrue();
        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        assertThat(context.getResult().orElseThrow()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
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
    void shouldNotDoCommand_MaximumRooms() {
        Course course = persistCourse();
        Long courseId = course.getId();
        Long studentId1 = persistStudent(1).getId();
        Long studentId2 = persistStudent(2).getId();
        assertThat(link(studentId1, courseId)).isTrue();
        assertThat(link(studentId2, courseId)).isTrue();
        assertThat(findCourseById(courseId).getStudents()).hasSize(2);
        Long studentId = persistStudent(3).getId();
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(unlink(studentId1, courseId)).isTrue();
        assertThat(unlink(studentId2, courseId)).isTrue();
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
    void shouldNotDoCommand_CoursesExceed() {
        Student student = persistStudent();
        Long studentId = student.getId();
        Long courseId1 = persistCourse(1).getId();
        Long courseId2 = persistCourse(2).getId();
        assertThat(link(studentId, courseId1)).isTrue();
        assertThat(link(studentId, courseId2)).isTrue();
        assertThat(findStudentById(studentId).getCourses()).hasSize(2);
        Long courseId = persistCourse(3).getId();
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        assertThat(unlink(studentId, courseId1)).isTrue();
        assertThat(unlink(studentId, courseId2)).isTrue();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentCoursesExceedException.class);
        assertThat(context.getException().getMessage()).startsWith("Student with ID:").contains(" exceeds maximum courses.");
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Student student = persistStudent();
        Long studentId = student.getId();
        Course course = persistCourse();
        Long courseId = course.getId();
        RuntimeException cannotExecute = new RuntimeException("Cannot link");
        doThrow(cannotExecute).when(persistence).link(any(Student.class), any(Course.class));
        Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        var error = assertThrows(Exception.class, () -> command.doCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(studentId);
        verify(persistence).findCourseById(courseId);
        verify(persistence).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldUndoCommand_Linked() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        assertThat(link(studentId, courseId)).isTrue();
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(studentId, courseId));
        }

        command.undoCommand(context);

        assertThat(findStudentById(student.getId()).getCourses()).isEmpty();
        assertThat(findCourseById(course.getId()).getStudents()).isEmpty();
        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence).unLink(student.getOriginal(), course.getOriginal());
    }

    @Test
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
    void shouldNotUndoCommand_ExceptionThrown() {
        StudentPayload student = persistStudent();
        CoursePayload course = persistCourse();
        Long studentId = student.getId();
        Long courseId = course.getId();
        Input<?> input = Input.of(studentId, courseId);
        assertThat(link(studentId, courseId)).isTrue();
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        RuntimeException cannotExecute = new RuntimeException("Cannot link");
        doThrow(cannotExecute).when(persistence).unLink(student.getOriginal(), course.getOriginal());
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(input));
        }

        var error = assertThrows(Exception.class, () -> command.undoCommand(context));

        assertThat(error).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        assertThat(findStudentById(studentId).getCourses()).contains(findCourseById(courseId));
        assertThat(findCourseById(courseId).getStudents()).contains(findStudentById(studentId));
        verify(persistence).unLink(student.getOriginal(), course.getOriginal());
        assertThat(unlink(studentId, courseId)).isTrue();
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
        return findEntity(CourseEntity.class, id, course -> course.getStudentSet().size());
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