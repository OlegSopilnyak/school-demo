package oleg.sopilnyak.test.end2end.command.executable.course;

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
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class FindCoursesWithoutStudentsCommandTest extends MysqlTestModelFactory {
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
    @Qualifier("courseFindNoStudents")
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
    void shouldDoCommand_CoursesFound() {
        Course course = persistCourse();
        Course saved = findCourseById(course.getId()).orElseThrow();
        Context<Set<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Set<Course> courses = context.getResult().orElseThrow();
        assertThat(courses.stream().filter(entity -> Objects.equals(entity.getId(), saved.getId())).findAny()).isPresent();
        verify(command).executeDo(context);
        verify(persistence).findCoursesWithoutStudents();
    }

    @Test
    void shouldDoCommand_CoursesWithStudent() {
        Course course = persistCourse();
        assertThat(findCoursesWithoutStudents()).contains(findCourseById(course.getId()).orElseThrow());
        Student student = persistStudent();
        assertThat(link(student.getId(), course.getId())).isTrue();
        reset(persistence);
        Context<Set<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(unlink(student.getId(), course.getId())).isTrue();
        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findCoursesWithoutStudents();
    }

    @Test
    void shouldDoCommand_CoursesNotFound() {
        Context<Set<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Course> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findCoursesWithoutStudents();
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findCoursesWithoutStudents();
        Context<Set<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCoursesWithoutStudents();
    }

    @Test
    void shouldUndoCommand() {
        Context<Set<Course>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }

    // private methods
    private Set<Course> findCoursesWithoutStudents() {
        return findAllFor(CourseEntity.class, entity -> entity.getStudentSet().size()).stream()
                .filter(entity -> entity.getStudentSet().isEmpty())
                .collect(Collectors.toSet());
    }

    private Optional<Course> findCourseById(Long id) {
        return Optional.ofNullable(findEntity(CourseEntity.class, id, e -> e.getStudentSet().size()));
    }

    private Course persistCourse() {
        try {
            Course course = persist(makeClearCourse(0));
            return payloadMapper.toPayload(findCourseById(course.getId()).orElseThrow());
        } finally {
            reset(payloadMapper);
        }
    }

    private CourseEntity persist(Course newInstance) {
        CourseEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getStudentSet().forEach(em::persist);
            em.getTransaction().commit();
            return em.find(CourseEntity.class, entity.getId());
        } finally {
            em.close();
        }
    }

    private Optional<Student> findStudentById(Long id) {
        return Optional.ofNullable(findEntity(StudentEntity.class, id, student -> student.getCourseSet().size()));
    }

    private StudentPayload persistStudent() {
        try {
            Student entity = persist(makeClearStudent(0));
            return payloadMapper.toPayload(findStudentById(entity.getId()).orElseThrow());
        } finally {
            reset(payloadMapper);
        }
    }

    private StudentEntity persist(Student newInstance) {
        StudentEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getCourseSet().forEach(em::persist);
            em.getTransaction().commit();
            return em.find(StudentEntity.class, entity.getId());
        } finally {
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