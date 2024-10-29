package oleg.sopilnyak.test.persistence.sql.implementation.education;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.repository.education.CourseRepository;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CoursesPersistenceTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    CoursesPersistenceFacade persistence;
    @SpyBean
    @Autowired
    CourseRepository repository;

    @AfterEach
    void tearDown() {
        reset(persistence);
        reset(repository);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void persistenceShouldBePresent() {
        assertThat(persistence).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindCourseById() {
        Course course = createCourse(0);
        long id = course.getId();

        Optional<Course> found = persistence.findCourseById(id);

        assertThat(found).isPresent();
        verify(repository).findById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSaveCourse() {
        Course course = makeClearCourse(1);

        Optional<Course> found = persistence.save(course);

        assertThat(found).isPresent();
        assertCourseEquals(course, found.orElseThrow(), false);
        verify(repository).saveAndFlush(any(CourseEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteCourse() {
        Course course = createCourse(2);
        long id = course.getId();

        persistence.deleteCourse(id);

        verify(repository).deleteById(id);
        verify(repository).flush();
        assertThat(persistence.findCourseById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeNoCourses() {

        assertThat(persistence.isNoCourses()).isTrue();

        verify(repository).count();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotBeNoCourses_CourseMade() {
        createCourse(3);

        assertThat(persistence.isNoCourses()).isFalse();

        verify(repository).count();
    }

    //private methods
    private Course createCourse(int order) {
        try {
            Course entity = persistence.save(makeClearCourse(order)).orElse(null);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNotNull();
            assertThat(repository.findById(entity.getId())).isPresent();
            return entity;
        } finally {
            reset(persistence, repository);
        }
    }
}