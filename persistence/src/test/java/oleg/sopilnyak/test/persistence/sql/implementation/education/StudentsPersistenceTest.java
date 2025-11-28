package oleg.sopilnyak.test.persistence.sql.implementation.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.repository.education.StudentRepository;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class StudentsPersistenceTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    StudentsPersistenceFacade persistence;
    @MockitoSpyBean
    @Autowired
    StudentRepository repository;

    @AfterEach
    void tearDown() {
        reset(persistence, repository);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void persistenceShouldBePresent() {
        assertThat(persistence).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentById() {
        Student student = createStudent(0);

        Optional<Student> found = persistence.findStudentById(student.getId());

        assertThat(found).isPresent().contains(student);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateStudent() {
        Student student = makeClearStudent(1);

        Optional<Student> created = persistence.save(student);

        assertThat(created).isPresent();
        assertStudentEquals(student, created.orElseThrow(), false);
        verify(repository).saveAndFlush(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateStudent() {
        StudentEntity student = createStudent(2);
        assertThat(student.getFirstName()).isNotEqualTo("firstName");
        student.setFirstName("firstName");

        Optional<Student> updated = persistence.save(student);

        assertThat(updated).isPresent();
        assertThat(updated.orElseThrow().getFirstName()).isEqualTo("firstName");
        assertStudentEquals(student, updated.orElseThrow(), false);
        verify(repository).saveAndFlush(any(StudentEntity.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudent() {
        StudentEntity student = createStudent(3);
        long id = student.getId();

        assertThat(persistence.deleteStudent(id)).isTrue();

        assertThat(persistence.findStudentById(id)).isEmpty();
        verify(repository).deleteById(id);
        verify(repository).flush();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeNoStudents() {
        assertThat(persistence.isNoStudents()).isTrue();

        verify(repository).count();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotBeNoStudents() {
        createStudent(4);

        assertThat(persistence.isNoStudents()).isFalse();

        verify(repository).count();
    }

    //private methods
    private StudentEntity createStudent(int order) {
        try {
            StudentEntity entity = persistence.save(makeClearStudent(order)).map(StudentEntity.class::cast).orElse(null);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNotNull();
            assertThat(repository.findById(entity.getId())).isPresent();
            return entity;
        } finally {
            reset(persistence, repository);
        }
    }
}