package oleg.sopilnyak.test.service.facade.student;

import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.student.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentsFacadeImplTest {
    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    @Spy
    CommandsFactory factory = buildFactory();

    @Spy
    @InjectMocks
    StudentsFacadeImpl facade;

    @Test
    void shouldNotFindById() {
        Long studentId = 100L;

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isEmpty();
        verify(factory).command(StudentCommandFacade.FIND_BY_ID);
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldFindById() {
        //TODO implement it later
    }

    @Test
    void shouldNotFindEnrolledTo() {
        Long courseId = 200L;

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).isEmpty();
        verify(factory).command(StudentCommandFacade.FIND_ENROLLED);
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldNotFindNotEnrolled() {

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(StudentCommandFacade.FIND_NOT_ENROLLED);
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldNotCreateOrUpdate() {
        Student student = mock(Student.class);

        Optional<Student> result = facade.createOrUpdate(student);

        assertThat(result).isEmpty();
        verify(factory).command(StudentCommandFacade.CREATE_OR_UPDATE);
        verify(persistenceFacade).save(any(Student.class));
    }

    @Test
    void shouldNotDelete() {
        Long studentId = 101L;

        StudentNotExistsException exception = assertThrows(StudentNotExistsException.class, () -> facade.delete(studentId));

        assertThat("Student with ID:101 is not exists.").isEqualTo(exception.getMessage());
        verify(factory).command(StudentCommandFacade.DELETE);
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    private CommandsFactory buildFactory() {
        return new SchoolCommandsFactory(
                Map.of(
                        StudentCommandFacade.FIND_BY_ID, spy(new FindStudentCommand(persistenceFacade)),
                        StudentCommandFacade.FIND_ENROLLED, spy(new FindEnrolledStudentsCommand(persistenceFacade)),
                        StudentCommandFacade.FIND_NOT_ENROLLED, spy(new FindNotEnrolledStudentsCommand(persistenceFacade)),
                        StudentCommandFacade.CREATE_OR_UPDATE, spy(new CreateOrUpdateStudentCommand(persistenceFacade)),
                        StudentCommandFacade.DELETE, spy(new DeleteStudentCommand(persistenceFacade))
                )
        );
    }
}