package oleg.sopilnyak.test.service.facade.student;

import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.student.*;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.facade.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentPayload;
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
    private static final String STUDENT_FIND_BY_ID = "student.findById";
    private static final String STUDENT_FIND_ENROLLED_TO = "student.findEnrolledTo";
    private static final String STUDENT_FIND_NOT_ENROLLED = "student.findNotEnrolled";
    private static final String STUDENT_CREATE_OR_UPDATE = "student.createOrUpdate";
    private static final String STUDENT_DELETE = "student.delete";

    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    @Spy
    CommandsFactory<StudentCommand> factory = buildFactory();

    @Spy
    @InjectMocks
    StudentsFacadeImpl facade;

    @Mock
    Student mockedStudent;
    @Mock
    StudentPayload mockedStudentPayload;

    @Test
    void shouldNotFindById() {
        Long studentId = 100L;

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isEmpty();
        verify(factory).command(STUDENT_FIND_BY_ID);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(studentId);
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldFindById() {
        Long studentId = 101L;
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isPresent();
        verify(factory).command(STUDENT_FIND_BY_ID);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(studentId);
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldNotFindEnrolledTo() {
        Long courseId = 200L;

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(courseId);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldFindEnrolledTo() {
        Long courseId = 200L;
        when(persistenceFacade.findEnrolledStudentsByCourseId(courseId)).thenReturn(Set.of(mockedStudent));

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).hasSize(1);
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(courseId);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldNotFindNotEnrolled() {

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(null);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldFindNotEnrolled() {
        when(persistenceFacade.findNotEnrolledStudents()).thenReturn(Set.of(mockedStudent));

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).hasSize(1);
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(null);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldNotCreateOrUpdate() {
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);

        Optional<Student> result = facade.createOrUpdate(mockedStudent);

        assertThat(result).isEmpty();
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(mockedStudentPayload);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedStudent);
        verify(persistenceFacade).save(mockedStudentPayload);
        verify(payloadMapper, never()).toPayload(mockedStudentPayload);
    }

    @Test
    void shouldCreateOrUpdate() {
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);
        when(payloadMapper.toPayload(mockedStudentPayload)).thenReturn(mockedStudentPayload);
        when(persistenceFacade.save(mockedStudentPayload)).thenReturn(Optional.of(mockedStudentPayload));

        Optional<Student> result = facade.createOrUpdate(mockedStudent);

        assertThat(result).isPresent();
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(mockedStudentPayload);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedStudent);
        verify(persistenceFacade).save(mockedStudentPayload);
        verify(payloadMapper).toPayload(mockedStudentPayload);
    }

    @Test
    void shouldDelete() throws StudentWithCoursesException, NotExistStudentException {
        Long studentId = 101L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);

        facade.delete(studentId);

        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).deleteStudent(studentId);
    }

    @Test
    void shouldNotDelete_StudentNotExists() {
        Long studentId = 102L;

        NotExistStudentException exception = assertThrows(NotExistStudentException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:102 is not exists.");
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    @Test
    void shouldNotDelete_StudentWithCourses() {
        Long studentId = 103L;
        when(mockedStudentPayload.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);

        StudentWithCoursesException exception = assertThrows(StudentWithCoursesException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:103 has registered courses.");
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(studentId);
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    private CommandsFactory<StudentCommand> buildFactory() {
        return new StudentCommandsFactory(
                Set.of(
                        spy(new FindStudentCommand(persistenceFacade)),
                        spy(new FindEnrolledStudentsCommand(persistenceFacade)),
                        spy(new FindNotEnrolledStudentsCommand(persistenceFacade)),
                        spy(new CreateOrUpdateStudentCommand(persistenceFacade, payloadMapper)),
                        spy(new DeleteStudentCommand(persistenceFacade, payloadMapper))
                )
        );
    }
}