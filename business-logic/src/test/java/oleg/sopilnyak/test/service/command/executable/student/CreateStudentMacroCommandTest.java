package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.base.BaseType;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
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
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
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
        assertThat(ReflectionTestUtils.getField(studentCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(studentCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
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

    @Test
    void shouldExecuteDoCommand() {
        Long profileId = 1L;
        Long studentId = 2L;
        Student newStudent = makeClearStudent(4);
        doAnswer(invocation -> {
            StudentProfilePayload payload = invocation.getArgument(0, StudentProfilePayload.class);
            payload.setId(profileId);
            return Optional.of(payload);
        }).when(persistence).save(any(StudentProfile.class));
        doAnswer(invocation -> {
            StudentPayload payload = invocation.getArgument(0, StudentPayload.class);
            payload.setId(studentId);
            return Optional.of(payload);
        }).when(persistence).save(any(Student.class));
        Context<Optional<Student>> context = command.createContext(newStudent);

        command.doCommand(context);

        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Context<Optional<BaseType>> profileContext = parameter.getNestedContexts().pop();
        Context<Optional<BaseType>> studentContext = parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(context);
        Optional<Student> savedStudent = context.getResult().orElseThrow();
        assertThat(savedStudent.orElseThrow().getId()).isEqualTo(studentId);
        assertThat(savedStudent.orElseThrow().getProfileId()).isEqualTo(profileId);
        assertStudentEquals(savedStudent.orElseThrow(), newStudent, false);

        checkContextAfterDoCommand(profileContext);
        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        assertThat(profileResult.orElseThrow().getId()).isEqualTo(profileId);
        assertThat(profileContext.<Long>getUndoParameter()).isEqualTo(profileId);

        checkContextAfterDoCommand(studentContext);
        Optional<BaseType> studentResult = studentContext.getResult().orElseThrow();
        final Student student = studentResult.map(Student.class::cast).orElseThrow();
        assertStudentEquals(student, newStudent, false);
        assertThat(student.getId()).isEqualTo(studentId);
        assertThat(student.getProfileId()).isEqualTo(profileId);
        assertThat(studentContext.<Long>getUndoParameter()).isEqualTo(studentId);
        assertThat(savedStudent.orElseThrow()).isSameAs(student);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        Student newStudent = makeClearStudent(5);
        Context<Optional<Student>> context = command.createContext(newStudent);
        RuntimeException exception = new RuntimeException("Cannot process nested commands");
        doThrow(exception).when(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
    }

    @Test
    void shouldNotExecuteDoCommand_CreateProfileDoNestedCommandsThrows() {
        Student newStudent = makeClearStudent(6);
        Context<Optional<Student>> context = command.createContext(newStudent);
        RuntimeException exception = new RuntimeException("Cannot process profile nested command");
        doThrow(exception).when(persistence).save(any(StudentProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Context<Optional<BaseType>> profileContext = parameter.getNestedContexts().pop();
        Context<Optional<BaseType>> studentContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isEqualTo(exception);

        assertThat(studentContext.getState()).isEqualTo(CANCEL);

        verify(profileCommand).doAsNestedCommand(eq(command), eq(profileContext), any(Context.StateChangedListener.class));
        verify(command).doNestedCommand(eq(profileCommand), eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(StudentProfile.class));

        verify(command, never()).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any(Context.class));

        verify(studentCommand, never()).doAsNestedCommand(any(NestedCommandExecutionVisitor.class), any(Context.class), any(Context.StateChangedListener.class));
        verify(command, never()).doNestedCommand(any(RootCommand.class), any(Context.class), any(Context.StateChangedListener.class));
    }

    @Test
    void shouldNotExecuteDoCommand_CreateStudentDoNestedCommandsThrows() {
        Long profileId = 10L;
        doAnswer(invocation -> {
            StudentProfilePayload payload = invocation.getArgument(0, StudentProfilePayload.class);
            payload.setId(profileId);
            return Optional.of(payload);
        }).when(persistence).save(any(StudentProfile.class));
        RuntimeException exception = new RuntimeException("Cannot process student nested command");
        doThrow(exception).when(persistence).save(any(Student.class));
        Student newStudent = makeClearStudent(7);
        Context<Optional<Student>> context = command.createContext(newStudent);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Context<Optional<BaseType>> profileContext = parameter.getNestedContexts().pop();
        Context<Optional<BaseType>> studentContext = parameter.getNestedContexts().pop();

        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        assertThat(profileResult.orElseThrow().getId()).isEqualTo(profileId);
        assertThat(profileContext.<Long>getUndoParameter()).isEqualTo(profileId);
        assertThat(profileContext.getState()).isEqualTo(UNDONE);
        assertThat(profileContext.getResult()).isPresent();

        Student student = studentContext.getRedoParameter();
        assertStudentEquals(student, newStudent, false);
        assertThat(student.getProfileId()).isEqualTo(profileId);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileContext, profileId);
    }

    // private methods
    private void verifyProfileUndoCommand(Context<Optional<BaseType>> profileContext, Long id) {
        verify(profileCommand).undoAsNestedCommand(command, profileContext);
        verify(command).undoNestedCommand(profileCommand, profileContext);
        verify(profileCommand).undoCommand(profileContext);
        verify(profileCommand).executeUndo(profileContext);
        verify(persistence).deleteProfileById(id);
    }

    private void verifyStudentUndoCommand(Context<Optional<BaseType>> studentContext, Long id) {
        verify(studentCommand).undoAsNestedCommand(command, studentContext);
        verify(command).undoNestedCommand(studentCommand, studentContext);
        verify(studentCommand).undoCommand(studentContext);
        verify(studentCommand).executeUndo(studentContext);
        verify(persistence).deleteStudent(id);
    }

    private void verifyProfileDoCommand(Context<Optional<BaseType>> nestedContext) {
        verify(profileCommand).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command).doNestedCommand(eq(profileCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(nestedContext);
        verify(profileCommand).executeDo(nestedContext);
        verify(persistence).save(any(StudentProfile.class));
    }

    private void verifyStudentDoCommand(Context<Optional<BaseType>> nestedContext) {
        verify(studentCommand).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command).doNestedCommand(eq(studentCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(studentCommand).doCommand(nestedContext);
        verify(studentCommand).executeDo(nestedContext);
        verify(persistence).save(any(Student.class));
    }

    private static <T> void checkContextAfterDoCommand(Context<Optional<T>> context) {
        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().get()).isPresent();
    }

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