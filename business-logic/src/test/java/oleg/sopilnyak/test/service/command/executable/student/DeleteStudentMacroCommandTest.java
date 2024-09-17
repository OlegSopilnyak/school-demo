package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteStudentMacroCommandTest extends TestModelFactory {
    @Mock
    PersistenceFacade persistence;
    @Spy
    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);
    @Spy
    @InjectMocks
    DeleteStudentProfileCommand profileCommand;
    @Spy
    @InjectMocks
    DeleteStudentCommand studentCommand;

    final int maxPoolSize = 10;

    DeleteStudentMacroCommand command;

    @Mock
    Student student;

    @BeforeEach
    void setUp() {
        command = spy(new DeleteStudentMacroCommand(studentCommand, profileCommand, persistence, maxPoolSize));
        command.runThreadPoolExecutor();
    }

    @AfterEach
    void tearDown() {
        reset(payloadMapper);
        command.stopThreadPoolExecutor();
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(profileCommand).isNotNull();
        assertThat(studentCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(command, "maxPoolSize")).isSameAs(maxPoolSize);
        assertThat(ReflectionTestUtils.getField(studentCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(studentCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        Long studentId = 1L;
        Long profileId = 2L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));

        Context<Void> context = command.createContext(studentId);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter<Void> redoParameter = context.getRedoParameter();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getInput()).isSameAs(studentId);
        Context<Void> studentContext = redoParameter.getNestedContexts().pop();
        Context<Void> profileContext = redoParameter.getNestedContexts().pop();
        assertThat(studentContext.isReady()).isTrue();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(studentContext.<Long>getRedoParameter()).isSameAs(studentId);
        assertThat(profileContext.<Long>getRedoParameter()).isSameAs(profileId);

        verify(studentCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(studentCommand, studentId);
        verify(studentCommand).createContext(studentId);

        verify(profileCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(profileCommand, studentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(persistence).findStudentById(studentId);
        verify(profileCommand).createContext(profileId);
    }

    @Test
    void shouldNotCreateMacroCommandContext_StudentNotFound() {
        Long studentId = 3L;

        Context<Void> context = command.createContext(studentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Not exists student with ID: " + studentId);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(studentCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(studentCommand, studentId);
        verify(studentCommand).createContext(studentId);

        verify(profileCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(profileCommand, studentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(profileCommand, never()).createContext(any());
        verify(profileCommand).createContextInit();
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";

        Context<Student> context = command.createContext(wrongTypeInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.DELETE_BY_ID);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(studentCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(studentCommand, wrongTypeInput);
        verify(studentCommand).createContext(wrongTypeInput);

        verify(profileCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(profileCommand, wrongTypeInput);
        verify(command, never()).createStudentProfileContext(eq(profileCommand), any());
        verify(profileCommand).createContextInit();
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentProfileContextThrows() {
        Long studentId = 4L;
        Long profileId = 5L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(profileId);

        Context<Void> context = command.createContext(studentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(studentCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(studentCommand, studentId);
        verify(studentCommand).createContext(studentId);

        verify(profileCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(profileCommand, studentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(persistence).findStudentById(studentId);
        verify(profileCommand).createContext(profileId);
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentContextThrows() {
        Long studentId = 6L;
        Long profileId = 7L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        String errorMessage = "Cannot create nested student context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(studentCommand).createContext(studentId);

        Context<Void> context = command.createContext(studentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(studentCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(studentCommand, studentId);
        verify(studentCommand).createContext(studentId);

        verify(profileCommand).acceptPreparedContext(command, studentId);
        verify(command).prepareContext(profileCommand, studentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(persistence).findStudentById(studentId);
        verify(profileCommand).createContext(profileId);
    }
}