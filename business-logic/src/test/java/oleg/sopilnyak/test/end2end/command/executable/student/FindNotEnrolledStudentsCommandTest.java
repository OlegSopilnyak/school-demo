package oleg.sopilnyak.test.end2end.command.executable.student;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.education.student.FindNotEnrolledStudentsCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, FindNotEnrolledStudentsCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class FindNotEnrolledStudentsCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    EducationPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    StudentCommand command;

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
    void shouldDoCommand_StudentsNotFound() {
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
    }

    @Test
    void shouldDoCommand_StudentsFound() {
        Student student = persistStudent();
        Student saved = findStudentById(student.getId());
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().orElseThrow()).contains(payloadMapper.toPayload(saved));
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
    }

    @Test
    void shouldDoCommand_StudentsFoundButEnrolled() {
        Student student = persistStudent();
        Course course = persistCourse();
        link(student.getId(), course.getId());
        assertThat(findStudentById(student.getId()).getCourses()).contains(findCourseById(course.getId()));
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
        unlink(student.getId(), course.getId());
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findNotEnrolledStudents();
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findNotEnrolledStudents();
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Set<Student>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }

    // private methods
    private Course findCourseById(Long id) {
        return findEntity(CourseEntity.class, id, course -> course.getStudentSet().size());
    }

    private Student findStudentById(Long id) {
        return findEntity(StudentEntity.class, id, student -> student.getCourseSet().size());
    }
    private Student persistStudent() {
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

    private Course persistCourse() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            Course source = makeClearCourse(0);
            CourseEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(CourseEntity.class, entity.getId()));
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