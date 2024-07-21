package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.base.BaseType;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentPayload;
import oleg.sopilnyak.test.service.message.StudentProfilePayload;
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

import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateStudentMacroCommandTest extends TestModelFactory {
    @Mock
    PersistenceFacade persistence;
    @Spy
    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);
    @Spy
    @InjectMocks
    CreateOrUpdateStudentProfileCommand profileCommand;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentCommand studentCommand;

    CreateStudentMacroCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new CreateStudentMacroCommand(studentCommand, profileCommand, payloadMapper));
    }

    @AfterEach
    void tearDown() {
        reset(payloadMapper);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(profileCommand).isNotNull();
        assertThat(studentCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(studentCommand, "persistence")).isEqualTo(persistence);
        assertThat(ReflectionTestUtils.getField(studentCommand, "payloadMapper")).isEqualTo(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isEqualTo(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isEqualTo(payloadMapper);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        StudentPayload newStudent = payloadMapper.toPayload(makeClearStudent(1));
        reset(payloadMapper);

        Context<Student> context = command.createContext(newStudent);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter<BaseType> parameter = context.getRedoParameter();
        assertThat(parameter).isNotNull();
        assertThat(parameter.getInput()).isSameAs(newStudent);
        Deque<Context<BaseType>> nested = parameter.getNestedContexts();
        assertThat(nested).hasSameSizeAs(command.fromNest());
        Context<BaseType> profileContext = nested.pop();
        Context<BaseType> studentContext = nested.pop();

        assertThat(studentContext).isNotNull();
        assertThat(studentContext.isReady()).isTrue();
        assertThat(studentContext.getCommand()).isSameAs(studentCommand);
        Student student = studentContext.getRedoParameter();
        assertThat(student).isNotNull();
        assertThat(student.getId()).isNull();
        assertStudentEquals(newStudent, student);
        String emailPrefix = student.getFirstName().toLowerCase() + "." + student.getLastName().toLowerCase();

        assertThat(profileContext).isNotNull();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(profileContext.getCommand()).isSameAs(profileCommand);
        StudentProfile profile = profileContext.getRedoParameter();
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNull();
        assertThat(profile.getEmail()).startsWith(emailPrefix);
        assertThat(profile.getPhone()).isNotEmpty();

        verifyProfileCommandContext(newStudent);

        verifyStudentCommandContext(newStudent);
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";

        Context<Student> context = command.createContext(wrongTypeInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.CREATE_OR_UPDATE);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(profileCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(profileCommand, wrongTypeInput);
        verify(command, never()).createStudentProfileContext(eq(profileCommand), any());

        verify(studentCommand, never()).acceptPreparedContext(eq(command), any());
        verify(command, never()).prepareContext(eq(studentCommand), any());
        verify(command, never()).createStudentContext(eq(studentCommand), any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentProfileContextThrows() {
        String errorMessage = "Cannot create nested profile context";
        Student newStudent = makeClearStudent(2);
        RuntimeException exception = new RuntimeException(errorMessage);
        when(profileCommand.createContext(any(StudentProfilePayload.class))).thenThrow(exception);

        Context<Student> context = command.createContext(newStudent);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(profileCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(profileCommand, newStudent);
        verify(command).createStudentProfileContext(profileCommand, newStudent);
        verify(profileCommand).createContext(any(StudentProfilePayload.class));

        verify(studentCommand, never()).acceptPreparedContext(eq(command), any());
        verify(command, never()).prepareContext(eq(studentCommand), any());
        verify(command, never()).createStudentContext(eq(studentCommand), any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentContextThrows() {
        String errorMessage = "Cannot create nested student context";
        Student newStudent = makeClearStudent(3);
        RuntimeException exception = new RuntimeException(errorMessage);
        when(studentCommand.createContext(any(StudentPayload.class))).thenThrow(exception);

        Context<Student> context = command.createContext(newStudent);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(profileCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(profileCommand, newStudent);
        verify(command).createStudentProfileContext(profileCommand, newStudent);
        verify(profileCommand).createContext(any(StudentProfilePayload.class));

        verify(studentCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(studentCommand, newStudent);
        verify(command).createStudentContext(studentCommand, newStudent);
        verify(studentCommand).createContext(any(StudentPayload.class));
    }

    // private methods
    private void verifyStudentCommandContext(StudentPayload newStudent) {
        verify(studentCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(studentCommand, newStudent);
        verify(command).createStudentContext(studentCommand, newStudent);
        verify(studentCommand).createContext(any(StudentPayload.class));
    }

    private void verifyProfileCommandContext(StudentPayload newStudent) {
        verify(profileCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(profileCommand, newStudent);
        verify(command).createStudentProfileContext(profileCommand, newStudent);
        verify(profileCommand).createContext(any(StudentProfilePayload.class));
    }
}