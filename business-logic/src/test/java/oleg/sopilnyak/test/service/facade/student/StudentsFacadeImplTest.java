package oleg.sopilnyak.test.service.facade.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.FindEnrolledStudentsCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.FindNotEnrolledStudentsCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.FindStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.MacroDeleteStudent;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.education.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
class StudentsFacadeImplTest {
    private static final String STUDENT_FIND_BY_ID = "student.findById";
    private static final String STUDENT_FIND_ENROLLED_TO = "student.findEnrolledTo";
    private static final String STUDENT_FIND_NOT_ENROLLED = "student.findNotEnrolled";
    private static final String STUDENT_CREATE_OR_UPDATE = "student.createOrUpdate";
    private static final String STUDENT_CREATE_NEW = "student.create.macro";
    private static final String STUDENT_DELETE_ALL = "student.delete.macro";

    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);

    StudentsFacadeImpl facade;
    @Mock
    ActionExecutor actionExecutor;
    @Mock
    SchedulingTaskExecutor schedulingTaskExecutor;

    @Mock
    Student mockedStudent;
    @Mock
    StudentPayload mockedStudentPayload;
    @Mock
    StudentProfile mockedProfile;
    @Mock
    StudentProfilePayload mockedStudentProfilePayload;

    CreateOrUpdateStudentCommand createStudentCommand;
    CreateOrUpdateStudentProfileCommand createProfileCommand;
    CreateStudentMacroCommand createMacroCommand;
    DeleteStudentCommand deleteStudentCommand;
    DeleteStudentProfileCommand deleteProfileCommand;
    DeleteStudentMacroCommand deleteStudentMacroCommand;

    CommandsFactory<StudentCommand<?>> factory;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        factory = buildFactory();
        facade = spy(new StudentsFacadeImpl(factory, payloadMapper, actionExecutor));
        doCallRealMethod().when(actionExecutor).commitAction(any(ActionContext.class), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        ActionContext.setup("test-facade", "test-action");
    }

    @Test
    void shouldNotFindById() {
        String commandId = STUDENT_FIND_BY_ID;
        StudentCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentFind", StudentCommand.class);
        Long studentId = 100L;

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isEmpty();
        verify(factory).command(STUDENT_FIND_BY_ID);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldFindById() {
        String commandId = STUDENT_FIND_BY_ID;
        StudentCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentFind", StudentCommand.class);
        Long studentId = 101L;
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isPresent();
        verify(factory).command(STUDENT_FIND_BY_ID);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(payloadMapper).toPayload(mockedStudent);
    }

    @Test
    void shouldNotFindEnrolledTo() {
        String commandId = STUDENT_FIND_ENROLLED_TO;
        StudentCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentFindEnrolled", StudentCommand.class);
        Long courseId = 200L;

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(Input.of(courseId));
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldFindEnrolledTo() {
        String commandId = STUDENT_FIND_ENROLLED_TO;
        StudentCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentFindEnrolled", StudentCommand.class);
        Long courseId = 210L;
        when(persistenceFacade.findEnrolledStudentsByCourseId(courseId)).thenReturn(Set.of(mockedStudent));

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).hasSize(1);
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(Input.of(courseId));
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
        verify(payloadMapper).toPayload(mockedStudent);
    }

    @Test
    void shouldNotFindNotEnrolled() {
        String commandId = STUDENT_FIND_NOT_ENROLLED;
        StudentCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentFindNotEnrolled", StudentCommand.class);

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(Input.empty());
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldFindNotEnrolled() {
        String commandId = STUDENT_FIND_NOT_ENROLLED;
        StudentCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentFindNotEnrolled", StudentCommand.class);
        when(persistenceFacade.findNotEnrolledStudents()).thenReturn(Set.of(mockedStudent));

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).hasSize(1);
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(Input.empty());
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
        verify(payloadMapper).toPayload(mockedStudent);
    }

    @Test
    void shouldNotCreateOrUpdate() {
        doReturn(createStudentCommand).when(applicationContext).getBean("studentUpdate", StudentCommand.class);
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);

        Optional<Student> result = facade.createOrUpdate(mockedStudent);

        assertThat(result).isEmpty();
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(Input.of(mockedStudentPayload));
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedStudent);
        verify(persistenceFacade).save(mockedStudentPayload);
        verify(payloadMapper, never()).toPayload(mockedStudentPayload);
    }

    @Test
    void shouldCreateNewStudent() {
        doReturn(createProfileCommand).when(applicationContext).getBean("profileStudentUpdate", StudentProfileCommand.class);
        doReturn(createStudentCommand).when(applicationContext).getBean("studentUpdate", StudentCommand.class);
        when(mockedStudentPayload.getFirstName()).thenReturn("John");
        when(mockedStudentPayload.getLastName()).thenReturn("Doe");
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);
        when(payloadMapper.toPayload(mockedStudentPayload)).thenReturn(mockedStudentPayload);
        when(persistenceFacade.save(mockedStudentPayload)).thenReturn(Optional.of(mockedStudentPayload));
        when(persistenceFacade.save(any(StudentProfilePayload.class))).thenReturn(Optional.of(mockedStudentProfilePayload));

        Optional<Student> result = facade.create(mockedStudent);

        assertThat(result.orElseThrow()).isEqualTo(mockedStudentPayload);
        verify(factory).command(STUDENT_CREATE_NEW);
        verify(factory.command(STUDENT_CREATE_NEW)).createContext(Input.of(mockedStudentPayload));
        verify(factory.command(STUDENT_CREATE_NEW)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedStudent);
        verify(persistenceFacade).save(mockedStudentPayload);
        verify(payloadMapper, never()).toPayload(mockedStudentPayload);
    }

    @Test
    void shouldCreateOrUpdateStudent() {
        doReturn(createStudentCommand).when(applicationContext).getBean("studentUpdate", StudentCommand.class);
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);
        when(payloadMapper.toPayload(mockedStudentPayload)).thenReturn(mockedStudentPayload);
        when(persistenceFacade.save(mockedStudentPayload)).thenReturn(Optional.of(mockedStudentPayload));

        Optional<Student> result = facade.createOrUpdate(mockedStudent);

        assertThat(result.orElseThrow()).isEqualTo(mockedStudentPayload);
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(Input.of(mockedStudentPayload));
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(payloadMapper).toPayload(mockedStudent);
        verify(persistenceFacade).save(mockedStudentPayload);
        verify(payloadMapper, never()).toPayload(mockedStudentPayload);
    }

    @Test
    void shouldDelete() throws StudentWithCoursesException, StudentNotFoundException {
        doReturn(deleteStudentCommand).when(applicationContext).getBean("studentDelete", StudentCommand.class);
        doReturn(deleteProfileCommand).when(applicationContext).getBean("profileStudentDelete", StudentProfileCommand.class);
        doReturn(deleteStudentMacroCommand).when(applicationContext).getBean("studentMacroDelete", MacroDeleteStudent.class);
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.initialize();
        doAnswer((Answer<Void>) invocationOnMock -> {
            threadPoolTaskExecutor.execute(invocationOnMock.getArgument(0, Runnable.class));
            return null;
        }).when(schedulingTaskExecutor).execute(any(Runnable.class));
        Long studentId = 101L;
        Long profileId = 1001L;
        when(mockedStudent.getProfileId()).thenReturn(profileId);
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findStudentProfileById(profileId)).thenReturn(Optional.of(mockedProfile));
        when(persistenceFacade.toEntity(mockedProfile)).thenReturn(mockedProfile);
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);
        when(payloadMapper.toPayload(mockedProfile)).thenReturn(mockedStudentProfilePayload);

        facade.delete(studentId);
        threadPoolTaskExecutor.shutdown();

        verify(factory).command(STUDENT_DELETE_ALL);
        verify(factory.command(STUDENT_DELETE_ALL)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade, atLeastOnce()).findStudentById(studentId);
        verify(persistenceFacade).findStudentProfileById(profileId);
        verify(persistenceFacade).toEntity(mockedProfile);
        verify(payloadMapper).toPayload(mockedStudent);
        verify(payloadMapper).toPayload(mockedProfile);
        verify(persistenceFacade).deleteStudent(studentId);
        verify(persistenceFacade).deleteProfileById(profileId);
    }

    @Test
    void shouldNotDelete_StudentNotExists() {
        doReturn(deleteStudentMacroCommand).when(applicationContext).getBean("studentMacroDelete", MacroDeleteStudent.class);
        Long studentId = 102L;

        StudentNotFoundException exception = assertThrows(StudentNotFoundException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:" + studentId + " is not exists.");
        verify(factory).command(STUDENT_DELETE_ALL);
        verify(factory.command(STUDENT_DELETE_ALL)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(payloadMapper, never()).toPayload(any(Student.class));
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    @Test
    void shouldNotDelete_StudentWithCourses() {
        doReturn(deleteStudentCommand).when(applicationContext).getBean("studentDelete", StudentCommand.class);
        doReturn(deleteStudentMacroCommand).when(applicationContext).getBean("studentMacroDelete", MacroDeleteStudent.class);
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.initialize();
        doAnswer((Answer<Void>) invocationOnMock -> {
            threadPoolTaskExecutor.execute(invocationOnMock.getArgument(0, Runnable.class));
            return null;
        }).when(schedulingTaskExecutor).execute(any(Runnable.class));
        Long studentId = 103L;
        Long profileId = 1003L;
        when(mockedStudent.getProfileId()).thenReturn(profileId);
        when(mockedStudentPayload.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findStudentById(studentId)).thenReturn(Optional.of(mockedStudent));
        when(persistenceFacade.findStudentProfileById(profileId)).thenReturn(Optional.of(mockedProfile));
        when(persistenceFacade.toEntity(mockedProfile)).thenReturn(mockedProfile);
        when(payloadMapper.toPayload(mockedStudent)).thenReturn(mockedStudentPayload);
        when(payloadMapper.toPayload(mockedProfile)).thenReturn(mockedStudentProfilePayload);

        StudentWithCoursesException exception = assertThrows(StudentWithCoursesException.class, () -> facade.delete(studentId));
        threadPoolTaskExecutor.shutdown();

        assertThat(exception.getMessage()).isEqualTo("Student with ID:103 has registered courses.");
        verify(factory).command(STUDENT_DELETE_ALL);
        verify(factory.command(STUDENT_DELETE_ALL)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade, atLeastOnce()).findStudentById(studentId);
        verify(payloadMapper).toPayload(mockedStudent);
        verify(persistenceFacade, never()).deleteStudent(studentId);
    }

    private CommandsFactory<StudentCommand<?>> buildFactory() {
        String acName = "applicationContext";
        createProfileCommand = spy(new CreateOrUpdateStudentProfileCommand(persistenceFacade, payloadMapper));
        deleteProfileCommand = spy(new DeleteStudentProfileCommand(persistenceFacade, payloadMapper));
        ReflectionTestUtils.setField(createProfileCommand, acName, applicationContext);
        ReflectionTestUtils.setField(deleteProfileCommand, acName, applicationContext);

        createStudentCommand = spy(new CreateOrUpdateStudentCommand(persistenceFacade, payloadMapper));
        createMacroCommand = spy(new CreateStudentMacroCommand(
                createStudentCommand, createProfileCommand, payloadMapper, actionExecutor
        ));
        deleteStudentCommand = spy(new DeleteStudentCommand(persistenceFacade, payloadMapper));
        deleteStudentMacroCommand = spy(new DeleteStudentMacroCommand(
                deleteStudentCommand, deleteProfileCommand, schedulingTaskExecutor, persistenceFacade, actionExecutor
        ));
        List<StudentCommand<?>> commands = List.of(
                spy(new FindStudentCommand(persistenceFacade, payloadMapper)),
                spy(new FindEnrolledStudentsCommand(persistenceFacade, payloadMapper)),
                spy(new FindNotEnrolledStudentsCommand(persistenceFacade, payloadMapper)),
                createStudentCommand,
                createMacroCommand,
                deleteStudentCommand,
                deleteStudentMacroCommand
        );
        commands.forEach(command -> {
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
        });
        return spy(new StudentCommandsFactory(commands));
    }
}