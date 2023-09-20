package oleg.sopilnyak.test.service.end2end.facade.student;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.student.*;
import oleg.sopilnyak.test.service.end2end.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.service.facade.student.StudentCommandFacade;
import oleg.sopilnyak.test.service.facade.student.StudentsFacadeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class StudentsFacadeImplTest extends MysqlTestModelFactory {
    @Autowired
    PersistenceFacade database;
    PersistenceFacade persistenceFacade;
    CommandsFactory factory;

    StudentsFacadeImpl facade;

    @BeforeEach
    void setUp() {
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new StudentsFacadeImpl(factory));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindById() {
        Long studentId = 100L;

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isEmpty();
        verify(factory).command(StudentCommandFacade.FIND_BY_ID);
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindById() {
        Student newStudent = makeClearTestStudent();
        Long studentId = getPersistentStudent(newStudent).getId();

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isNotEmpty();
        assertStudentEquals(newStudent, student.get(), false);
        verify(factory).command(StudentCommandFacade.FIND_BY_ID);
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindEnrolledTo() {
        Long courseId = 200L;

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).isEmpty();
        verify(factory).command(StudentCommandFacade.FIND_ENROLLED);
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindNotEnrolled() {

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(StudentCommandFacade.FIND_NOT_ENROLLED);
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdate() {
        Student student = mock(Student.class);

        Optional<Student> result = facade.createOrUpdate(student);

        assertThat(result).isNotEmpty();
        verify(factory).command(StudentCommandFacade.CREATE_OR_UPDATE);
        verify(persistenceFacade).save(any(Student.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDelete() {
        Long studentId = 101L;

        StudentNotExistsException exception = assertThrows(StudentNotExistsException.class, () -> facade.delete(studentId));

        assertThat("Student with ID:101 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(StudentCommandFacade.DELETE);
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    // private methods
    private Student getPersistentStudent(Student newStudent) {
        Optional<Student> saved = database.save(newStudent);
        assertThat(saved).isNotEmpty();
        return saved.get();
    }

    private CommandsFactory buildFactory(PersistenceFacade persistenceFacade) {
        return new SchoolCommandsFactory(
                Set.of(
                        new FindStudentCommand(persistenceFacade),
                        new FindEnrolledStudentsCommand(persistenceFacade),
                        new FindNotEnrolledStudentsCommand(persistenceFacade),
                        new CreateOrUpdateStudentCommand(persistenceFacade),
                        new DeleteStudentCommand(persistenceFacade)
                )
        );
    }
}