package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import org.jetbrains.annotations.NotNull;
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
    @Mock
    StudentProfile profile;

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

        Context<Boolean> context = command.createContext(studentId);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter redoParameter = context.getRedoParameter();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getInput()).isSameAs(studentId);
        Context<?> studentContext = redoParameter.getNestedContexts().pop();
        Context<?> profileContext = redoParameter.getNestedContexts().pop();
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

        Context<Boolean> context = command.createContext(studentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + studentId + " is not exists.");
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

        Context<Boolean> context = command.createContext(wrongTypeInput);

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

        Context<Boolean> context = command.createContext(studentId);

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

        Context<Boolean> context = command.createContext(studentId);

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
    void shouldExecuteDoCommand() {
        Long profileId = 8L;
        Long studentId = 9L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(studentId);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.getRedoParameter();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(studentContext.<StudentPayload>getUndoParameter().getOriginal()).isSameAs(student);
        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(studentContext.<Long>getRedoParameter()).isEqualTo(studentId);
        assertThat(profileContext.<Long>getRedoParameter()).isEqualTo(profileId);

//        verifyStudentDoCommand(studentContext);
//        verifyProfileDoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteDoCommand_StudentNotFound() {
        Long studentId = 10L;
        Context<Boolean> context = command.createContext(studentId);
        assertThat(context.isReady()).isFalse();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + studentId + " is not exists.");
        assertThat(context.<Object>getRedoParameter()).isNull();
        verify(command, never()).executeDo(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_ProfileNotFound() {
        Long profileId = 12L;
        Long studentId = 11L;
        when(student.getId()).thenReturn(studentId);
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.save(any(Student.class))).thenReturn(Optional.of(student));
        Context<Boolean> context = command.createContext(studentId);
        assertThat(context.isReady()).isTrue();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.getRedoParameter();

        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
//        verifyStudentDoCommand(studentContext);
//        verifyStudentUndoCommand(studentContext);
        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteStudentThrows() {
        Long profileId = 14L;
        Long studentId = 13L;
        when(student.getId()).thenReturn(studentId);
        when(profile.getId()).thenReturn(profileId);
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(persistence.save(any(StudentProfile.class))).thenReturn(Optional.of(profile));
        Context<Boolean> context = command.createContext(studentId);
        String errorMessage = "Cannot delete student";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.deleteStudent(studentId)).thenThrow(exception);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.getRedoParameter();

        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(studentContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(studentContext.<StudentPayload>getUndoParameter()).isNull();
        assertThat(studentContext.getResult()).isEmpty();

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
//        verifyStudentDoCommand(studentContext);
//        verifyProfileDoCommand(profileContext);
//        verifyProfileUndoCommand(profileContext);
        verify(studentCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        Long profileId = 16L;
        Long studentId = 15L;
        when(student.getId()).thenReturn(studentId);
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.save(any(Student.class))).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(studentId);
        String errorMessage = "Cannot delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.getRedoParameter();

        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

//        verifyStudentDoCommand(studentContext);
//        verifyProfileDoCommand(profileContext);
//
//        verifyStudentUndoCommand(studentContext);
        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldExecuteUndoCommand() {
        Long profileId = 18L;
        Long studentId = 17L;
        Context<Boolean> context = createStudentAndProfileFor(profileId, studentId);
        when(persistence.save(any(Student.class))).thenReturn(Optional.of(student));
        when(persistence.save(any(StudentProfile.class))).thenReturn(Optional.of(profile));

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        MacroCommandParameter parameter = context.getRedoParameter();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));
//        verifyStudentUndoCommand(studentContext);
//        verifyProfileUndoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        Long profileId = 20L;
        Long studentId = 19L;
        when(student.getId()).thenReturn(studentId);
        when(student.getProfileId()).thenReturn(profileId);
        Context<Boolean> context = createStudentAndProfileFor(profileId, studentId);
        when(persistence.save(any(Student.class))).thenReturn(Optional.of(student));
        String errorMessage = "Cannot restore profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.save(any(StudentProfile.class))).thenThrow(exception);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter parameter = context.getRedoParameter();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));
//        verifyStudentUndoCommand(studentContext);
//        verifyProfileUndoCommand(profileContext);
//        verifyStudentDoCommand(studentContext, 2);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveStudentThrows() {
        Long profileId = 22L;
        Long studentId = 21L;
        when(profile.getId()).thenReturn(profileId);
        Context<Boolean> context = createStudentAndProfileFor(profileId, studentId);
        when(persistence.save(any(StudentProfile.class))).thenReturn(Optional.of(profile));
        String errorMessage = "Cannot restore student";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.save(any(Student.class))).thenThrow(exception);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter parameter = context.getRedoParameter();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isSameAs(exception);
        assertThat(studentContext.<StudentPayload>getUndoParameter().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));
//        verifyStudentUndoCommand(studentContext);
//        verifyProfileUndoCommand(profileContext);
//        verifyProfileDoCommand(profileContext, 2);
    }


    // private methods
    private @NotNull Context<Boolean> createStudentAndProfileFor(Long profileId, Long studentId) {
        when(student.getId()).thenReturn(studentId);
        when(student.getProfileId()).thenReturn(profileId);
        when(profile.getId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(studentId);
        command.doCommand(context);
        return context;
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext) {
        verifyProfileDoCommand(nestedContext, 1);
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext, int i) {
        verify(profileCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command, times(i)).doNestedCommand(eq(profileCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand, times(i)).doCommand(nestedContext);
        verify(profileCommand, times(i)).executeDo(nestedContext);
        Long id = nestedContext.getRedoParameter();
        verify(persistence, times(i)).findStudentProfileById(id);
        verify(persistence, times(i)).deleteProfileById(id);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext) {
        verifyStudentDoCommand(nestedContext, 1);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext, int i) {
        verify(studentCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command, times(i)).doNestedCommand(eq(studentCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(studentCommand, times(i)).doCommand(nestedContext);
        verify(studentCommand, times(i)).executeDo(nestedContext);
        Long id = nestedContext.getRedoParameter();
        verify(persistence, times(i + 1)).findStudentById(id);
        verify(persistence, times(i)).deleteStudent(id);
    }

    private void verifyStudentUndoCommand(Context<Boolean> nestedContext) {
        verify(studentCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(studentCommand, nestedContext);
        verify(studentCommand).undoCommand(nestedContext);
        verify(studentCommand).executeUndo(nestedContext);
        verify(persistence).save(any(Student.class));
    }

    private void verifyProfileUndoCommand(Context<Boolean> nestedContext) {
        verify(profileCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(profileCommand, nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
        verify(persistence).save(any(StudentProfile.class));
    }
}