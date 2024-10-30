package oleg.sopilnyak.test.persistence.sql.implementation.education;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.repository.education.CourseRepository;
import oleg.sopilnyak.test.persistence.sql.repository.education.StudentRepository;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class RegisterPersistenceTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    RegisterPersistenceFacade persistence;
    @SpyBean
    @Autowired
    StudentsPersistenceFacade studentsPersistence;
    @SpyBean
    @Autowired
    CoursesPersistenceFacade coursesPersistence;
    @SpyBean
    @Autowired
    CourseRepository courseRepository;
    @SpyBean
    @Autowired
    StudentRepository studentRepository;

    @AfterEach
    void tearDown() {
        reset(persistence, studentsPersistence, coursesPersistence, courseRepository, studentRepository);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheCourseWithStudent() {
        CourseEntity course = createCourse(1);
        StudentEntity student = createStudent(11);
        coursesPersistence.save(course);
        studentsPersistence.save(student);
        course.add(student);

        coursesPersistence.save(course);

        Optional<Course> received = coursesPersistence.findCourseById(course.getId());
        assertThat(course).isEqualTo(received.orElseThrow());
        assertThat(received.orElseThrow().getStudents()).contains(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void persistenceShouldBePresent() {
        assertThat(persistence).isNotNull();
        assertThat(studentsPersistence).isNotNull();
        assertThat(coursesPersistence).isNotNull();
        assertThat(courseRepository).isNotNull();
        assertThat(studentRepository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindEnrolledStudentsByCourseId() {
        Student student = createStudent(0);
        Course course = createCourse(0);
        long courseId = course.getId();
        persistence.link(student, course);

        Set<Student> enrolled = persistence.findEnrolledStudentsByCourseId(courseId);

        assertThat(enrolled).isNotEmpty().contains(student);
        verify(studentRepository).findStudentEntitiesByCourseSetId(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindNotEnrolledStudents() {
        Student student = createStudent(1);

        Set<Student> notEnrolledStudents = persistence.findNotEnrolledStudents();

        assertThat(notEnrolledStudents).isNotEmpty().contains(student);
        verify(studentRepository).findStudentEntitiesByCourseSetEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCoursesRegisteredForStudent() {
        Student student = createStudent(2);
        Course course = createCourse(2);
        long studentId = student.getId();
        persistence.link(student, course);

        Set<Course> registered = persistence.findCoursesRegisteredForStudent(studentId);

        assertThat(registered).isNotEmpty().contains(course);
        verify(courseRepository).findCourseEntitiesByStudentSetId(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCoursesWithoutStudents() {
        Course course = createCourse(3);

        Set<Course> registered = persistence.findCoursesWithoutStudents();

        assertThat(registered).isNotEmpty().contains(course);
        verify(courseRepository).findCourseEntitiesByStudentSetEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldLinkStudentToCourse() {
        StudentEntity student = createStudent(4);
        Course course = createCourse(4);

        assertThat(persistence.link(student, course)).isTrue();

        assertThat(course.getStudents()).contains(student);
        verify(studentRepository).findById(student.getId());
        verify(courseRepository).findById(course.getId());
        verify(studentRepository).saveAndFlush(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnLinkStudentFromCourse() {
        StudentEntity student = createStudent(5);
        Course course = createCourse(5);
        assertThat(student.add(course)).isTrue();
        studentRepository.saveAndFlush(student);
        assertThat(studentRepository.findById(student.getId()).orElseThrow().getCourses()).contains(course);
        reset(studentRepository);

        assertThat(persistence.unLink(student, course)).isTrue();

        verify(studentRepository).findById(student.getId());
        verify(courseRepository).findById(course.getId());
        verify(studentRepository).saveAndFlush(student);
        assertThat(courseRepository.findById(course.getId()).orElseThrow().getStudents()).isEmpty();
        assertThat(studentRepository.findById(student.getId()).orElseThrow().getCourses()).isEmpty();
    }

    //private methods
    private StudentEntity createStudent(int order) {
        try {
            StudentEntity entity = studentsPersistence.save(makeClearStudent(order)).map(StudentEntity.class::cast).orElse(null);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNotNull();
            assertThat(studentRepository.findById(entity.getId())).isPresent();
            return entity;
        } finally {
            reset(persistence, studentRepository);
        }
    }

    private CourseEntity createCourse(int order) {
        try {
            CourseEntity entity = coursesPersistence.save(makeClearCourse(order)).map(CourseEntity.class::cast).orElse(null);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNotNull();
            assertThat(courseRepository.findById(entity.getId())).isPresent();
            return entity;
        } finally {
            reset(persistence, courseRepository);
        }
    }
}