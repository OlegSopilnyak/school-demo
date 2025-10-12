package oleg.sopilnyak.test.service.command.executable.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
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
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

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
    @Mock
    ActionExecutor actionExecutor;
    @Mock
    SchedulingTaskExecutor schedulingTaskExecutor;
    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

    DeleteStudentMacroCommand command;

    @Mock
    Student student;
    @Mock
    StudentProfile profile;

    @BeforeEach
    void setUp() {
        command = spy(new DeleteStudentMacroCommand(
                studentCommand, profileCommand, schedulingTaskExecutor, persistence, actionExecutor
        ));
        doAnswer((Answer<Void>) invocationOnMock -> {
            threadPoolTaskExecutor.execute(invocationOnMock.getArgument(0, Runnable.class));
            return null;
        }).when(schedulingTaskExecutor).execute(any(Runnable.class));
        doCallRealMethod().when(actionExecutor).commitAction(any(ActionContext.class), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        ActionContext.setup("test-facade", "test-action");
        threadPoolTaskExecutor.initialize();
    }

    @AfterEach
    void tearDown() {
        reset(payloadMapper);
        threadPoolTaskExecutor.shutdown();
        threadPoolTaskExecutor = null;
    }

    @Test
    void shouldBeValidCommand() {
        reset(actionExecutor, schedulingTaskExecutor);
        assertThat(profileCommand).isNotNull();
        assertThat(studentCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(studentCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(studentCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        assertThat(nested.pop()).isSameAs(studentCommand);
        assertThat(nested.pop()).isSameAs(profileCommand);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long studentId = 1L;
        Long profileId = 2L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));

        Input<?> inputStudentId = Input.of(studentId);
        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter redoParameter = context.<MacroCommandParameter>getRedoParameter().value();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getRootInput().value()).isSameAs(studentId);
        Context<?> studentContext = redoParameter.getNestedContexts().pop();
        Context<?> profileContext = redoParameter.getNestedContexts().pop();
        assertThat(studentContext.isReady()).isTrue();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(studentContext.getRedoParameter().value()).isSameAs(studentId);
        assertThat(profileContext.getRedoParameter().value()).isSameAs(profileId);

        verify(studentCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(studentCommand, inputStudentId);
        verify(studentCommand).createContext(inputStudentId);

        verify(profileCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(profileCommand, inputStudentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(persistence).findStudentById(studentId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_StudentNotFound() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long studentId = 3L;

        Input<?> inputStudentId = Input.of(studentId);
        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + studentId + " is not exists.");
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(studentCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(studentCommand, inputStudentId);
        verify(studentCommand).createContext(inputStudentId);

        verify(profileCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(profileCommand, inputStudentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(profileCommand, never()).createContext(any());
        verify(profileCommand).createFailedContext(any(StudentNotFoundException.class));
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        reset(actionExecutor, schedulingTaskExecutor);
        Object wrongTypeInput = "something";

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Boolean> context = command.createContext(wrongInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.DELETE_BY_ID);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(studentCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(studentCommand, wrongInput);
        verify(studentCommand).createContext(wrongInput);

        verify(profileCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(profileCommand, wrongInput);
        verify(command, never()).createStudentProfileContext(eq(profileCommand), any());
        verify(profileCommand).createFailedContext(any(CannotCreateCommandContextException.class));
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentProfileContextThrows() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long studentId = 4L;
        Long profileId = 5L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(Input.of(profileId));

        Input<?> inputStudentId = Input.of(studentId);
        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(studentCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(studentCommand, inputStudentId);
        verify(studentCommand).createContext(inputStudentId);

        verify(profileCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(profileCommand, inputStudentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(persistence).findStudentById(studentId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentContextThrows() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long studentId = 6L;
        Long profileId = 7L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        String errorMessage = "Cannot create nested student context";
        RuntimeException exception = new RuntimeException(errorMessage);
        Input<?> inputStudentId = Input.of(studentId);
        doThrow(exception).when(studentCommand).createContext(inputStudentId);

        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(studentCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(studentCommand, inputStudentId);
        verify(studentCommand).createContext(inputStudentId);

        verify(profileCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(profileCommand, inputStudentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(persistence).findStudentById(studentId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldExecuteDoCommand() {
        Long profileId = 8L;
        Long studentId = 9L;
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(Input.of(studentId));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(studentContext.getRedoParameter().value()).isEqualTo(studentId);
        assertThat(profileContext.getRedoParameter().value()).isEqualTo(profileId);

        verifyStudentDoCommand(studentContext);
        verifyProfileDoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteDoCommand_StudentNotFound() {
        reset(actionExecutor, schedulingTaskExecutor);
        Long studentId = 10L;
        Context<Boolean> context = command.createContext(Input.of(studentId));
        assertThat(context.isReady()).isFalse();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + studentId + " is not exists.");
        assertThat(context.getRedoParameter().isEmpty()).isTrue();
        verify(command, never()).executeDo(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_ProfileNotFound() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 12L;
        Long studentId = 11L;
        when(student.getId()).thenReturn(studentId);
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.save(any(Student.class))).thenReturn(Optional.of(student));
        Context<Boolean> context = command.createContext(Input.of(studentId));
        assertThat(context.isReady()).isTrue();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        verifyStudentDoCommand(studentContext);
        verifyStudentUndoCommand(studentContext);
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteStudentThrows() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 14L;
        Long studentId = 13L;
        when(student.getId()).thenReturn(studentId);
        when(profile.getId()).thenReturn(profileId);
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(persistence.save(any(StudentProfile.class))).thenReturn(Optional.of(profile));
        Context<Boolean> context = command.createContext(Input.of(studentId));
        String errorMessage = "Cannot delete student";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.deleteStudent(studentId)).thenThrow(exception);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(studentContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(studentContext.<StudentPayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(studentContext.getResult()).isEmpty();

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        verifyStudentDoCommand(studentContext);
        verifyProfileDoCommand(profileContext);
        verifyProfileUndoCommand(profileContext);
        verify(studentCommand, never()).undoCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 16L;
        Long studentId = 15L;
        when(student.getId()).thenReturn(studentId);
        when(student.getProfileId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.save(any(Student.class))).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(Input.of(studentId));
        String errorMessage = "Cannot delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyStudentDoCommand(studentContext);
        verifyProfileDoCommand(profileContext);

        verifyStudentUndoCommand(studentContext);
        verify(profileCommand, never()).undoCommand(any(Context.class));
    }

    @Test
    void shouldExecuteUndoCommand() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 18L;
        Long studentId = 17L;
        Context<Boolean> context = createStudentAndProfileFor(profileId, studentId);
        when(persistence.save(any(Student.class))).thenReturn(Optional.of(student));
        when(persistence.save(any(StudentProfile.class))).thenReturn(Optional.of(profile));

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verifyStudentUndoCommand(studentContext);
        verifyProfileUndoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
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
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verifyStudentUndoCommand(studentContext);
        verifyProfileUndoCommand(profileContext);
        verifyStudentDoCommand(studentContext, 2);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveStudentThrows() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
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
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> studentContext = parameter.getNestedContexts().pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isSameAs(exception);
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<?> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verifyStudentUndoCommand(studentContext);
        verifyProfileUndoCommand(profileContext);
        verifyProfileDoCommand(profileContext, 2);
    }


    // private methods
    private @NotNull Context<Boolean> createStudentAndProfileFor(Long profileId, Long studentId) {
        when(student.getId()).thenReturn(studentId);
        when(student.getProfileId()).thenReturn(profileId);
        when(profile.getId()).thenReturn(profileId);
        when(persistence.findStudentById(studentId)).thenReturn(Optional.of(student));
        when(persistence.findStudentProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        return context;
    }

    private void verifyProfileDoCommand(Context<?> nestedContext) {
        verifyProfileDoCommand(nestedContext, 1);
    }

    private void verifyProfileDoCommand(Context<?> nestedContext, int i) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class) );
        Context<Boolean> profileContext = (Context<Boolean>) nestedContext;
        verify(profileCommand, times(i)).doCommand(profileContext);
        verify(profileCommand, times(i)).executeDo(profileContext);
        Long id = nestedContext.<Long>getRedoParameter().value();
        verify(persistence, times(i)).findStudentProfileById(id);
        verify(persistence, times(i)).deleteProfileById(id);
    }

    private void verifyStudentDoCommand(Context<?> nestedContext) {
        verifyStudentDoCommand(nestedContext, 1);
    }

    private void verifyStudentDoCommand(Context<?> nestedContext, int i) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class) );
        Context<Boolean> personContext = (Context<Boolean>) nestedContext;
        verify(studentCommand, times(i)).doCommand(personContext);
        verify(studentCommand, times(i)).executeDo(personContext);
        Long id = nestedContext.<Long>getRedoParameter().value();
        verify(persistence, times(i + 1)).findStudentById(id);
        verify(persistence, times(i)).deleteStudent(id);
    }

    private void verifyStudentUndoCommand(Context<?> nestedContext) {
        verify(command).executeUndoNested(nestedContext);
        verify(studentCommand).undoCommand(nestedContext);
        verify(studentCommand).executeUndo(nestedContext);
        verify(persistence).save(any(Student.class));
    }

    private void verifyProfileUndoCommand(Context<?> nestedContext) {
        verify(command).executeUndoNested(nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
        verify(persistence).save(any(StudentProfile.class));
    }
}