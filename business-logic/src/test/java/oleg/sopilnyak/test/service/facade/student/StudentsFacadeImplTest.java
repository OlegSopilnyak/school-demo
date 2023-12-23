package oleg.sopilnyak.test.service.facade.student;

import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.student.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    @Mock
    Student mockedStudent;

    @Test
    void shouldNotFindById() {
        String commandId = StudentCommandFacade.FIND_BY_ID;
        Long studentId = 100L;

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldFindById() {
        String commandId = StudentCommandFacade.FIND_BY_ID;
        Long studentId = 101L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldNotFindEnrolledTo() {
        String commandId = StudentCommandFacade.FIND_ENROLLED;
        Long courseId = 200L;

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldFindEnrolledTo() {
        String commandId = StudentCommandFacade.FIND_ENROLLED;
        Long courseId = 200L;
        when(persistenceFacade.findEnrolledStudentsByCourseId(courseId)).thenReturn(Set.of(mockedStudent));

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(courseId);
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldNotFindNotEnrolled() {
        String commandId = StudentCommandFacade.FIND_NOT_ENROLLED;

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldFindNotEnrolled() {
        String commandId = StudentCommandFacade.FIND_NOT_ENROLLED;
        when(persistenceFacade.findNotEnrolledStudents()).thenReturn(Set.of(mockedStudent));

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(null);
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldNotCreateOrUpdate() {
        String commandId = StudentCommandFacade.CREATE_OR_UPDATE;

        Optional<Student> result = facade.createOrUpdate(mockedStudent);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockedStudent);
        verify(persistenceFacade).save(mockedStudent);
    }

    @Test
    void shouldCreateOrUpdate() {
        String commandId = StudentCommandFacade.CREATE_OR_UPDATE;
        when(persistenceFacade.save(mockedStudent)).thenReturn(Optional.of(mockedStudent));

        Optional<Student> result = facade.createOrUpdate(mockedStudent);

        assertThat(result).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(mockedStudent);
        verify(persistenceFacade).save(mockedStudent);
    }

    @Test
    void shouldDelete() throws StudentWithCoursesException, StudentNotExistsException {
        String commandId = StudentCommandFacade.DELETE;
        Long studentId = 101L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        facade.delete(studentId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).deleteStudent(studentId);
    }

    @Test
    void shouldNotDelete_StudentNotExists() {
        String commandId = StudentCommandFacade.DELETE;
        Long studentId = 102L;

        StudentNotExistsException exception = assertThrows(StudentNotExistsException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:102 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    @Test
    void shouldNotDelete_StudentWithCourses() {
        String commandId = StudentCommandFacade.DELETE;
        Long studentId = 103L;
        when(mockedStudent.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        StudentWithCoursesException exception = assertThrows(StudentWithCoursesException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:103 has registered courses.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).execute(studentId);
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    private CommandsFactory buildFactory() {
        return new SchoolCommandsFactory("students",
                Set.of(
                        spy(new FindStudentCommand(persistenceFacade)),
                        spy(new FindEnrolledStudentsCommand(persistenceFacade)),
                        spy(new FindNotEnrolledStudentsCommand(persistenceFacade)),
                        spy(new CreateOrUpdateStudentCommand(persistenceFacade)),
                        spy(new DeleteStudentCommand(persistenceFacade))
                )
        );
    }
}