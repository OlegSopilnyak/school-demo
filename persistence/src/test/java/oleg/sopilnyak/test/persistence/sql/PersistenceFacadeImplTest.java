package oleg.sopilnyak.test.persistence.sql;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Rollback
class PersistenceFacadeImplTest {
    @Container
    private static final MySQLContainer<?> database = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @Autowired
    PersistenceFacade facade;

    @Test
    void shouldLoadContext() {
        assertThat(facade).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentById() {
        StudentEntity student = buildStudentEntity(null);
        Optional<Student> saved = facade.save(student);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        assertThat(id).isNotNull();

        Optional<Student> received = facade.findStudentById(id);

        assertThat(saved).isEqualTo(received);
        assertThat(saved.get()).isEqualTo(received.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheStudent() {
        StudentEntity student = buildStudentEntity(1);

        facade.save(student);

        assertThat(student).isEqualTo(facade.findStudentById(student.getId()).get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheStudentWithTheCourse() {
        StudentEntity student = buildStudentEntity(3);
        facade.save(student);
        CourseEntity course = buildCourseEntity(33);
        facade.save(course);
        student.add(course);

        facade.save(student);

        Optional<Student> saved = facade.findStudentById(student.getId());

        assertThat(student).isEqualTo(saved.get());
        assertThat(((StudentEntity) saved.get()).getCourseSet()).contains(course);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudent() {
        StudentEntity student = buildStudentEntity(2);
        facade.save(student);
        assertThat(student).isEqualTo(facade.findStudentById(student.getId()).get());

        boolean success = facade.deleteStudent(student.getId());

        assertThat(success).isTrue();
        assertThat(facade.findStudentById(student.getId())).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCourseById() {
        CourseEntity course = buildCourseEntity(null);
        Optional<Course> saved = facade.save(course);
        assertThat(saved).isNotEmpty();
        Long id = saved.get().getId();
        assertThat(id).isNotNull();

        Optional<Course> received = facade.findCourseById(id);

        assertThat(saved).isEqualTo(received);
        assertThat(saved.get()).isEqualTo(received.get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheCourse() {
        CourseEntity course = buildCourseEntity(1);

        facade.save(course);

        assertThat(course).isEqualTo(facade.findCourseById(course.getId()).get());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveTheCourseWithStudent() {
        CourseEntity course = buildCourseEntity(1);
        StudentEntity student = buildStudentEntity(11);
        facade.save(course);
        facade.save(student);
        course.add(student);

        Optional<Course> saved = facade.save(course);

        Optional<Course> received = facade.findCourseById(course.getId());
        assertThat(course).isEqualTo(received.get());
        assertThat(received.get().getStudents()).contains(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteCourse() {
        CourseEntity course = buildCourseEntity(2);
        facade.save(course);
        assertThat(course).isEqualTo(facade.findCourseById(course.getId()).get());

        boolean success = facade.deleteCourse(course.getId());

        assertThat(success).isTrue();
        assertThat(facade.findCourseById(course.getId())).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCoursesRegisteredForStudent() {
        CourseEntity course1 = buildCourseEntity(3);
        facade.save(course1);
        CourseEntity course2 = buildCourseEntity(4);
        facade.save(course2);
        StudentEntity student = buildStudentEntity(null);
        facade.save(student);
        student.add(course1);
        student.add(course2);
        facade.save(student);

        Set<Course> registered = facade.findCoursesRegisteredForStudent(student.getId());

        assertThat(registered).containsExactlyInAnyOrder(course1, course2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCoursesWithoutStudents() {
        CourseEntity course1 = buildCourseEntity(3);
        facade.save(course1);
        CourseEntity course2 = buildCourseEntity(4);
        facade.save(course2);

        Set<Course> empty = facade.findCoursesWithoutStudents();

        assertThat(empty).containsExactlyInAnyOrder(course1, course2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindEnrolledStudentsByCourseId() {
        StudentEntity student1 = buildStudentEntity(5);
        StudentEntity student2 = buildStudentEntity(6);
        facade.save(student1);
        facade.save(student2);
        CourseEntity course = buildCourseEntity(55);
        facade.save(course);
        course.add(student1);
        course.add(student2);
        facade.save(course);

        Set<Student> enrolled = facade.findEnrolledStudentsByCourseId(course.getId());

        assertThat(enrolled).containsExactlyInAnyOrder(student1, student2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindNotEnrolledStudents() {
        StudentEntity student1 = buildStudentEntity(5);
        StudentEntity student2 = buildStudentEntity(6);
        facade.save(student1);
        facade.save(student2);

        Set<Student> enrolled = facade.findNotEnrolledStudents();

        assertThat(enrolled).containsExactlyInAnyOrder(student1, student2);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldLinkStudentToCourse() {
        StudentEntity student = buildStudentEntity(7);
        facade.save(student);
        CourseEntity course = buildCourseEntity(77);
        facade.save(course);

        boolean success = facade.link(student, course);

        assertThat(success).isTrue();
        Optional<Course> courseOptional = facade.findCourseById(course.getId());
        Optional<Student> studentOptional = facade.findStudentById(student.getId());
        assertThat(studentOptional.get().getCourses()).contains(course);
        assertThat(courseOptional.get().getStudents()).contains(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUnlinkStudentFromTheCourse() {
        StudentEntity student = buildStudentEntity(8);
        facade.save(student);
        CourseEntity course = buildCourseEntity(88);
        facade.save(course);
        assertThat(facade.link(student, course)).isTrue();
        assertThat(student.getCourses()).contains(course);
        assertThat(course.getStudents()).contains(student);

        boolean success = facade.unLink(student, course);

        assertThat(success).isTrue();
        Optional<Course> courseOptional = facade.findCourseById(course.getId());
        Optional<Student> studentOptional = facade.findStudentById(student.getId());
        assertThat(studentOptional.get().getCourses()).isEmpty();
        assertThat(courseOptional.get().getStudents()).isEmpty();
    }

    private static StudentEntity buildStudentEntity(Integer counter) {
        return counter == null ? StudentEntity.builder()
                .firstName("first-name")
                .lastName("last-name")
                .gender("gender")
                .description("description")
                .build() :
                StudentEntity.builder()
                        .firstName("first-name-" + counter)
                        .lastName("last-name-" + counter)
                        .gender("gender-" + counter)
                        .description("description-" + counter)
                        .build();
    }

    private static CourseEntity buildCourseEntity(Integer counter) {
        return counter == null ? CourseEntity.builder()
                .name("course-name")
                .description("description")
                .build() :
                CourseEntity.builder()
                        .name("course-name-" + counter)
                        .description("description-" + counter)
                        .build();
    }

}
