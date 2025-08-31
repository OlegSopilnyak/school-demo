package oleg.sopilnyak.test.service.facade.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.course.CreateOrUpdateCourseCommand;
import oleg.sopilnyak.test.service.command.executable.course.DeleteCourseCommand;
import oleg.sopilnyak.test.service.command.executable.course.FindCourseCommand;
import oleg.sopilnyak.test.service.command.executable.course.FindCoursesWithoutStudentsCommand;
import oleg.sopilnyak.test.service.command.executable.course.FindRegisteredCoursesCommand;
import oleg.sopilnyak.test.service.command.executable.course.RegisterStudentToCourseCommand;
import oleg.sopilnyak.test.service.command.executable.course.UnRegisterStudentFromCourseCommand;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.facade.education.impl.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoursesFacadeImplTest {
    private static final String COURSE_FIND_BY_ID = "course.findById";
    private static final String COURSE_FIND_REGISTERED_FOR = "course.findRegisteredFor";
    private static final String COURSE_FIND_WITHOUT_STUDENTS = "course.findWithoutStudents";
    private static final String COURSE_CREATE_OR_UPDATE = "course.createOrUpdate";
    private static final String COURSE_DELETE = "course.delete";
    private static final String COURSE_REGISTER = "course.register";
    private static final String COURSE_UN_REGISTER = "course.unRegister";

    ActionExecutor actionExecutor = mock(ActionExecutor.class);
    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    @Spy
    CommandsFactory<CourseCommand<?>> factory = buildFactory();
    @Spy
    @InjectMocks
    CoursesFacadeImpl facade;

    @Mock
    Course mockedCourse;
    @Mock
    CoursePayload mockedCoursePayload;
    @Mock
    Student mockedStudent;

    @BeforeEach
    void setUp() {
        ActionContext.setup("test-facade", "test-action");
        doCallRealMethod().when(actionExecutor).commitAction(any(ActionContext.class), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @Test
    void shouldNotFindById() {
        Long courseId = 200L;

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isEmpty();
        verify(factory).command(COURSE_FIND_BY_ID);
        verify(factory.command(COURSE_FIND_BY_ID)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper, never()).toPayload(any(Course.class));
    }

    @Test
    void shouldFindById() {
        Long courseId = 201L;
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isPresent().contains(mockedCoursePayload);
        verify(factory).command(COURSE_FIND_BY_ID);
        verify(factory.command(COURSE_FIND_BY_ID)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper, times(2)).toPayload(mockedCourse);
    }

    @Test
    void shouldFindRegisteredFor() {
        Long studentId = 100L;
        when(persistenceFacade.findCoursesRegisteredForStudent(studentId)).thenReturn(Set.of(mockedCourse));

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).hasSize(1);
        verify(factory).command(COURSE_FIND_REGISTERED_FOR);
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).createContext(Input.of(studentId));
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldNotFindRegisteredFor() {
        Long studentId = 101L;

        Set<Course> course = facade.findRegisteredFor(studentId);

        assertThat(course).isEmpty();
        verify(factory).command(COURSE_FIND_REGISTERED_FOR);
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).createContext(Input.of(studentId));
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
        verify(payloadMapper, never()).toPayload(any(Course.class));
    }

    @Test
    void shouldFindWithoutStudents() {
        when(persistenceFacade.findCoursesWithoutStudents()).thenReturn(Set.of(mockedCourse));

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).hasSize(1);
        verify(factory).command(COURSE_FIND_WITHOUT_STUDENTS);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).createContext(Input.empty());
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldNotFindWithoutStudents() {

        Set<Course> course = facade.findWithoutStudents();

        assertThat(course).isEmpty();
        verify(factory).command(COURSE_FIND_WITHOUT_STUDENTS);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).createContext(Input.empty());
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
        verify(payloadMapper, never()).toPayload(any(Course.class));
    }

    @Test
    void shouldCreateOrUpdate() {
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        when(payloadMapper.toPayload(mockedCoursePayload)).thenReturn(mockedCoursePayload);
        when(persistenceFacade.save(mockedCoursePayload)).thenReturn(Optional.of(mockedCoursePayload));

        Optional<Course> course = facade.createOrUpdate(mockedCourse);

        assertThat(course).isPresent().contains(mockedCoursePayload);
        verify(factory).command(COURSE_CREATE_OR_UPDATE);
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).createContext(Input.of(mockedCoursePayload));
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade).save(mockedCoursePayload);
    }

    @Test
    void shouldNotCreateOrUpdate() {
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);

        Optional<Course> course = facade.createOrUpdate(mockedCourse);

        assertThat(course).isEmpty();
        verify(factory).command(COURSE_CREATE_OR_UPDATE);
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).createContext(Input.of(mockedCoursePayload));
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade).save(mockedCoursePayload);
    }

    @Test
    void shouldDelete() throws CourseNotFoundException, CourseWithStudentsException {
        Long courseId = 202L;
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);

        facade.delete(courseId);

        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade).deleteCourse(courseId);
    }

    @Test
    void shouldNotDelete_CourseNotExists() {
        Long courseId = 203L;

        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:203 is not exists.");
        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper, never()).toPayload(any(Course.class));
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    void shouldNotDelete_CourseWithStudents() {
        Long courseId = 204L;
        when(mockedCoursePayload.getStudents()).thenReturn(List.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);

        CourseWithStudentsException exception = assertThrows(CourseWithStudentsException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:204 has enrolled students.");
        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    void shouldRegister() throws
            CourseNotFoundException, CourseHasNoRoomException,
            StudentCoursesExceedException, StudentNotFoundException {
        Long studentId = 102L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        Long courseId = 205L;
        when(mockedCourse.getId()).thenReturn(courseId);
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        facade.register(studentId, courseId);

        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotRegister_StudentNotExists() {
        Long studentId = 103L;
        Long courseId = 206L;

        Exception exception = assertThrows(StudentNotFoundException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:103 is not exists.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotRegister_CourseNotExists() {
        Long studentId = 104L;
        Long courseId = 207L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Exception exception = assertThrows(CourseNotFoundException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:207 is not exists.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotRegister_StudentCoursesExceed() {
        Long studentId = 105L;
        Long courseId = 208L;
        when(mockedStudent.getId()).thenReturn(studentId);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(mockedStudent.getCourses()).thenReturn(IntStream.range(1, 10).mapToObj(i -> mockedCourse).toList());

        Exception exception = assertThrows(StudentCoursesExceedException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:105 exceeds maximum courses.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotRegister_NoRoomInTheCourse() {
        Long studentId = 106L;
        Long courseId = 209L;
        when(mockedCourse.getId()).thenReturn(courseId);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(mockedCourse.getStudents()).thenReturn(IntStream.range(1, 51).mapToObj(i -> mockedStudent).toList());

        Exception exception = assertThrows(CourseHasNoRoomException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:209 does not have enough rooms.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldUnRegister() throws CourseNotFoundException, StudentNotFoundException {
        Long studentId = 107L;
        Long courseId = 210L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        facade.unRegister(studentId, courseId);

        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotUnRegister_StudentNotExists() {
        Long studentId = 108L;
        Long courseId = 211L;

        Exception exception = assertThrows(StudentNotFoundException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:108 is not exists.");
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotUnRegister_CourseNotExists() {
        Long studentId = 109L;
        Long courseId = 212L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Exception exception = assertThrows(CourseNotFoundException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:212 is not exists.");
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).unLink(any(Student.class), any(Course.class));
    }

    private CommandsFactory<CourseCommand<?>> buildFactory() {
        return new CourseCommandsFactory(
                List.of(
                        spy(new FindCourseCommand(persistenceFacade, payloadMapper)),
                        spy(new FindRegisteredCoursesCommand(persistenceFacade, payloadMapper)),
                        spy(new FindCoursesWithoutStudentsCommand(persistenceFacade, payloadMapper)),
                        spy(new CreateOrUpdateCourseCommand(persistenceFacade, payloadMapper)),
                        spy(new DeleteCourseCommand(persistenceFacade, payloadMapper)),
                        spy(new RegisterStudentToCourseCommand(persistenceFacade, payloadMapper, 50, 5)),
                        spy(new UnRegisterStudentFromCourseCommand(persistenceFacade, payloadMapper))
                )
        );
    }

}