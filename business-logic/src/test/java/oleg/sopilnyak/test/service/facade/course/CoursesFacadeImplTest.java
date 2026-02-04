package oleg.sopilnyak.test.service.facade.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.course.CreateOrUpdateCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.DeleteCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.FindCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.FindCoursesWithoutStudentsCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.FindRegisteredCoursesCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.RegisterStudentToCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.UnRegisterStudentFromCourseCommand;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.facade.education.impl.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class CoursesFacadeImplTest {
    private static final String COURSE_FIND_BY_ID = "school::education::courses:find.By.Id";
    private static final String COURSE_FIND_REGISTERED_FOR = "school::education::courses:find.Registered.To.The.Student";
    private static final String COURSE_FIND_WITHOUT_STUDENTS = "school::education::courses:find.Without.Any.Student";
    private static final String COURSE_CREATE_OR_UPDATE = "school::education::courses:create.Or.Update";
    private static final String COURSE_DELETE = "school::education::courses:delete";
    private static final String COURSE_REGISTER = "school::education::courses:register";
    private static final String COURSE_UN_REGISTER = "school::education::courses:unregister";

    CommandActionExecutor actionExecutor = mock(CommandActionExecutor.class);
    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    CommandsFactory<CourseCommand<?>> factory;
    CoursesFacadeImpl facade;

    @Mock
    Course mockedCourse;
    @Mock
    CoursePayload mockedCoursePayload;
    @Mock
    Student mockedStudent;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory());
        facade = spy(new CoursesFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-action");
        doCallRealMethod().when(actionExecutor).commitAction(any(ActionContext.class), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @Test
    void shouldFindById_Unified() {
        String commandId = COURSE_FIND_BY_ID;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFind", CourseCommand.class);
        Long courseId = 201L;
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        Optional<Course> course = facade.doActionAndResult(commandId, courseId);

        assertThat(course).isPresent().contains(mockedCoursePayload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldFindById() {
        String commandId = COURSE_FIND_BY_ID;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFind", CourseCommand.class);
        Long courseId = 201L;
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        Optional<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalFindById", courseId);

        assertThat(course).isPresent().contains(mockedCoursePayload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldNotFindById() {
        String commandId = COURSE_FIND_BY_ID;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFind", CourseCommand.class);
        Long courseId = 200L;

        Optional<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalFindById", courseId);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper, never()).toPayload(any(Course.class));
    }

    @Test
    void shouldFindRegisteredFor_Unified() {
        String commandId = COURSE_FIND_REGISTERED_FOR;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFindWithStudent", CourseCommand.class);
        Long studentId = 100L;
        when(persistenceFacade.findCoursesRegisteredForStudent(studentId)).thenReturn(Set.of(mockedCourse));

        Set<Course> course = facade.doActionAndResult(commandId, studentId);

        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldFindRegisteredFor() {
        String commandId = COURSE_FIND_REGISTERED_FOR;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFindWithStudent", CourseCommand.class);
        Long studentId = 100L;
        when(persistenceFacade.findCoursesRegisteredForStudent(studentId)).thenReturn(Set.of(mockedCourse));

        Set<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalFindRegisteredFor", studentId);

        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldNotFindRegisteredFor() {
        String commandId = COURSE_FIND_REGISTERED_FOR;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFindWithStudent", CourseCommand.class);
        Long studentId = 101L;

        Set<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalFindRegisteredFor", studentId);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
        verify(payloadMapper, never()).toPayload(any(Course.class));
    }

    @Test
    void shouldFindWithoutStudents_Unified() {
        String commandId = COURSE_FIND_WITHOUT_STUDENTS;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFindNoStudents", CourseCommand.class);
        when(persistenceFacade.findCoursesWithoutStudents()).thenReturn(Set.of(mockedCourse));

        Set<Course> course = facade.doActionAndResult(commandId);

        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldFindWithoutStudents() {
        String commandId = COURSE_FIND_WITHOUT_STUDENTS;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFindNoStudents", CourseCommand.class);
        when(persistenceFacade.findCoursesWithoutStudents()).thenReturn(Set.of(mockedCourse));

        Set<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalFindWithoutStudents");

        assertThat(course).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
        verify(payloadMapper).toPayload(mockedCourse);
    }

    @Test
    void shouldNotFindWithoutStudents() {
        String commandId = COURSE_FIND_WITHOUT_STUDENTS;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseFindNoStudents", CourseCommand.class);

        Set<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalFindWithoutStudents");

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
        verify(payloadMapper, never()).toPayload(any(Course.class));
    }

    @Test
    void shouldCreateOrUpdate_Unified() {
        String commandId = COURSE_CREATE_OR_UPDATE;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseUpdate", CourseCommand.class);
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        when(payloadMapper.toPayload(mockedCoursePayload)).thenReturn(mockedCoursePayload);
        when(persistenceFacade.save(mockedCoursePayload)).thenReturn(Optional.of(mockedCoursePayload));

        Optional<Course> course = facade.doActionAndResult(commandId, mockedCourse);

        assertThat(course).isPresent().contains(mockedCoursePayload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(mockedCoursePayload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade).save(mockedCoursePayload);
    }

    @Test
    void shouldCreateOrUpdate() {
        String commandId = COURSE_CREATE_OR_UPDATE;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseUpdate", CourseCommand.class);
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        when(payloadMapper.toPayload(mockedCoursePayload)).thenReturn(mockedCoursePayload);
        when(persistenceFacade.save(mockedCoursePayload)).thenReturn(Optional.of(mockedCoursePayload));

        Optional<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", mockedCourse);

        assertThat(course).isPresent().contains(mockedCoursePayload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(mockedCoursePayload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade).save(mockedCoursePayload);
    }

    @Test
    void shouldNotCreateOrUpdate() {
        String commandId = COURSE_CREATE_OR_UPDATE;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseUpdate", CourseCommand.class);
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);

        Optional<Course> course = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", mockedCourse);

        assertThat(course).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(mockedCoursePayload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade).save(mockedCoursePayload);
    }

    @Test
    void shouldDelete_Unified() throws CourseNotFoundException, CourseWithStudentsException {
        String commandId = COURSE_DELETE;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseDelete", CourseCommand.class);
        Long courseId = 202L;
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);

        facade.doActionAndResult(commandId, courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade).deleteCourse(courseId);
    }

    @Test
    void shouldDelete() throws CourseNotFoundException, CourseWithStudentsException {
        String commandId = COURSE_DELETE;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseDelete", CourseCommand.class);
        Long courseId = 202L;
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);

        ReflectionTestUtils.invokeMethod(facade, "internalDelete", courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade).deleteCourse(courseId);
    }

    @Test
    void shouldNotDelete_CourseNotExists() {
        String commandId = COURSE_DELETE;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseDelete", CourseCommand.class);
        Long courseId = 203L;

        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Course with ID:203 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper, never()).toPayload(any(Course.class));
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    void shouldNotDelete_CourseWithStudents() {
        String commandId = COURSE_DELETE;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseDelete", CourseCommand.class);
        Long courseId = 204L;
        when(mockedCoursePayload.getStudents()).thenReturn(List.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);

        CourseWithStudentsException exception = assertThrows(CourseWithStudentsException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Course with ID:204 has enrolled students.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistenceFacade, never()).deleteCourse(courseId);
    }

    @Test
    void shouldRegister_Unified() {
        String commandId = COURSE_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseRegisterStudent", CourseCommand.class);
        Long studentId = 102L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        Long courseId = 205L;
        when(mockedCourse.getId()).thenReturn(courseId);
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        facade.doActionAndResult(commandId, studentId, courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldRegister() {
        String commandId = COURSE_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseRegisterStudent", CourseCommand.class);
        Long studentId = 102L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        Long courseId = 205L;
        when(mockedCourse.getId()).thenReturn(courseId);
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        ReflectionTestUtils.invokeMethod(facade, "internalRegister", studentId, courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).link(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotRegister_StudentNotExists() {
        String commandId = COURSE_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseRegisterStudent", CourseCommand.class);
        Long studentId = 103L;
        Long courseId = 206L;

        Exception exception = assertThrows(StudentNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalRegister", studentId, courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Student with ID:103 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotRegister_CourseNotExists() {
        String commandId = COURSE_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseRegisterStudent", CourseCommand.class);
        Long studentId = 104L;
        Long courseId = 207L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Exception exception = assertThrows(CourseNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalRegister", studentId, courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Course with ID:207 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotRegister_StudentCoursesExceed() {
        String commandId = COURSE_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseRegisterStudent", CourseCommand.class);
        Long studentId = 105L;
        Long courseId = 208L;
        when(mockedStudent.getId()).thenReturn(studentId);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(mockedStudent.getCourses()).thenReturn(IntStream.range(1, 10).mapToObj(_ -> mockedCourse).toList());

        Exception exception = assertThrows(StudentCoursesExceedException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalRegister", studentId, courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Student with ID:105 exceeds maximum courses.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotRegister_NoRoomInTheCourse() {
        String commandId = COURSE_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseRegisterStudent", CourseCommand.class);
        Long studentId = 106L;
        Long courseId = 209L;
        when(mockedCourse.getId()).thenReturn(courseId);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));
        when(mockedCourse.getStudents()).thenReturn(IntStream.range(1, 51).mapToObj(_ -> mockedStudent).toList());

        Exception exception = assertThrows(CourseHasNoRoomException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalRegister", studentId, courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Course with ID:209 does not have enough rooms.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(Student.class), any(Course.class));
    }

    @Test
    void shouldUnRegister_Unified() {
        String commandId = COURSE_UN_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseUnRegisterStudent", CourseCommand.class);
        Long studentId = 107L;
        Long courseId = 210L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        facade.doActionAndResult(commandId, studentId, courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(mockedStudent, mockedCourse);
    }

    @Test
    void shouldUnRegister() {
        String commandId = COURSE_UN_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseUnRegisterStudent", CourseCommand.class);
        Long studentId = 107L;
        Long courseId = 210L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findCourseById(courseId)).thenReturn(Optional.of(mockedCourse));

        ReflectionTestUtils.invokeMethod(facade, "internalUnRegister", studentId, courseId);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(mockedStudent, mockedCourse);
    }

    @Test
    void shouldNotUnRegister_StudentNotExists() {
        String commandId = COURSE_UN_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseUnRegisterStudent", CourseCommand.class);
        Long studentId = 108L;
        Long courseId = 211L;

        Exception exception = assertThrows(StudentNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalUnRegister", studentId, courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Student with ID:108 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotUnRegister_CourseNotExists() {
        String commandId = COURSE_UN_REGISTER;
        CourseCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("courseUnRegisterStudent", CourseCommand.class);
        Long studentId = 109L;
        Long courseId = 212L;
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Exception exception = assertThrows(CourseNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalUnRegister", studentId, courseId)
        );

        assertThat(exception.getMessage()).isEqualTo("Course with ID:212 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(studentId, courseId));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).unLink(any(Student.class), any(Course.class));
    }

    private CommandsFactory<CourseCommand<?>> buildFactory() {
        Map<CourseCommand<?>, String> commands = Map.of(
                spy(new FindCourseCommand(persistenceFacade, payloadMapper)), "courseFind",
                spy(new FindRegisteredCoursesCommand(persistenceFacade, payloadMapper)),"courseFindWithStudent",
                spy(new FindCoursesWithoutStudentsCommand(persistenceFacade, payloadMapper)),"courseFindNoStudents",
                spy(new CreateOrUpdateCourseCommand(persistenceFacade, payloadMapper)),"courseUpdate",
                spy(new DeleteCourseCommand(persistenceFacade, payloadMapper)),"courseDelete",
                spy(new RegisterStudentToCourseCommand(persistenceFacade, payloadMapper, 50, 5)),"courseRegisterStudent",
                spy(new UnRegisterStudentFromCourseCommand(persistenceFacade, payloadMapper)),"courseUnRegisterStudent"
        );
        String acName = "applicationContext";
        commands.entrySet().forEach(entry -> {
            CourseCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
        });
        return new CourseCommandsFactory(commands.keySet());
    }

}