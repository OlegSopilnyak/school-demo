package oleg.sopilnyak.test.service.command.executable.student;

import static oleg.sopilnyak.test.service.command.type.core.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.core.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.mapstruct.factory.Mappers;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
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
    CreateOrUpdateStudentCommand personCommand;
    @Mock
    CommandActionExecutor actionExecutor;
    @Mock
    ApplicationContext applicationContext;

    CreateStudentMacroCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new CreateStudentMacroCommand(personCommand, profileCommand, payloadMapper, actionExecutor));
        // setup nested commands
        ReflectionTestUtils.setField(profileCommand, "applicationContext", applicationContext);
        doReturn(profileCommand).when(applicationContext).getBean("profileStudentUpdate", StudentProfileCommand.class);
        ReflectionTestUtils.setField(personCommand, "applicationContext", applicationContext);
        doReturn(personCommand).when(applicationContext).getBean("studentUpdate", StudentCommand.class);
        // execute command locally
        doCallRealMethod().when(actionExecutor).commitAction(any(ActionContext.class), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        ActionContext.setup("test-facade", "test-action");
    }

    @AfterEach
    void tearDown() {
        reset(payloadMapper);
    }

    @Test
    void shouldBeValidCommand() {
        reset(actionExecutor, applicationContext);
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        reset(actionExecutor, applicationContext);
        StudentPayload newStudent = payloadMapper.toPayload(makeClearStudent(1));
        reset(payloadMapper);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nestedCommands.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nestedCommands.pop();

        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        assertThat(parameter).isNotNull();
        assertThat(parameter.getRootInput().value()).isSameAs(newStudent);
        Deque<Context<?>> nested = parameter.getNestedContexts();
        assertThat(nested).hasSameSizeAs(command.fromNest());
        Context<?> profileContext = nested.pop();
        Context<?> studentContext = nested.pop();

        assertThat(studentContext).isNotNull();
        assertThat(studentContext.isReady()).isTrue();
        assertThat(studentContext.getCommand()).isSameAs(nestedStudentCommand);
        Student student = studentContext.<Student>getRedoParameter().value();
        assertThat(student).isNotNull();
        assertThat(student.getId()).isNull();
        assertStudentEquals(newStudent, student);
        String emailPrefix = student.getFirstName().toLowerCase() + "." + student.getLastName().toLowerCase();

        assertThat(profileContext).isNotNull();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(profileContext.getCommand()).isSameAs(nestedProfileCommand);
        StudentProfile profile = profileContext.<StudentProfile>getRedoParameter().value();
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNull();
        assertThat(profile.getEmail()).startsWith(emailPrefix);
        assertThat(profile.getPhone()).isNotEmpty();

        verifyProfileCommandContext(newStudent, nestedProfileCommand);

        verifyStudentCommandContext(newStudent, nestedStudentCommand);
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        reset(actionExecutor, applicationContext);
        Object wrongTypeInput = "something";
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nested.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nested.pop();

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Optional<Student>> context = command.createContext(wrongInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains("profile.student.createOrUpdate");
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(nestedProfileCommand, wrongInput);
        verify(command, never()).createProfileContext(eq(nestedProfileCommand), any());

        verify(nestedStudentCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(nestedStudentCommand, wrongInput);
        verify(command, never()).createPersonContext(eq(nestedStudentCommand), any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateProfileContextThrows() {
        reset(actionExecutor, applicationContext);
        Input<Student> inputParameter = (Input<Student>) Input.of(makeClearStudent(2));
        String errorMessage = "Cannot create nested profile context";
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());

        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nested.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nested.pop();

        RuntimeException exception = new RuntimeException(errorMessage);
        when(nestedProfileCommand.createContext(any(Input.class))).thenThrow(exception);
        Context<Optional<Student>> context = command.createContext(inputParameter);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(nestedProfileCommand, inputParameter);
        verify(command).createProfileContext(nestedProfileCommand, inputParameter.value());
        verify(nestedProfileCommand).createContext(Mockito.<Input<StudentProfile>>any());

        verify(nestedStudentCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(nestedStudentCommand, inputParameter);
        verify(command).createPersonContext(nestedStudentCommand, inputParameter.value());
        verify(nestedStudentCommand).createContext(inputParameter);
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        reset(actionExecutor, applicationContext);
        Input<Student> inputParameter = (Input<Student>) Input.of(makeClearStudent(3));
        String errorMessage = "Cannot create nested student context";
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());

        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nested.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nested.pop();

        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(nestedStudentCommand).createContext(inputParameter);
        Context<Optional<Student>> context = command.createContext(inputParameter);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(nestedProfileCommand, inputParameter);
        verify(command).createProfileContext(nestedProfileCommand, inputParameter.value());
        verify(nestedProfileCommand).createContext(Mockito.<Input<StudentProfile>>any());

        verify(nestedStudentCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(nestedStudentCommand, inputParameter);
        verify(command).createPersonContext(nestedStudentCommand, inputParameter.value());
        verify(nestedStudentCommand).createContext(Mockito.<Input<Student>>any());
    }

    @Test
    void shouldExecuteDoCommand() {
        Long profileId = 1L;
        Long studentId = 2L;
        Student newStudent = makeClearStudent(4);
        adjustStudentProfileSaving(profileId);
        adjustStudentSaving(studentId);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));

        command.doCommand(context);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(context);
        Optional<Student> savedStudent = context.getResult().orElseThrow();
        assertThat(savedStudent.orElseThrow().getId()).isEqualTo(studentId);
        assertThat(savedStudent.orElseThrow().getProfileId()).isEqualTo(profileId);
        assertStudentEquals(savedStudent.orElseThrow(), newStudent, false);

        checkContextAfterDoCommand(profileContext);
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        assertThat(profileResult.orElseThrow().getId()).isEqualTo(profileId);
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(studentContext);
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        final Student student = studentResult.orElseThrow();
        assertStudentEquals(student, newStudent, false);
        assertThat(student.getId()).isEqualTo(studentId);
        assertThat(student.getProfileId()).isEqualTo(profileId);
        assertThat(studentContext.getUndoParameter().value()).isEqualTo(studentId);
        assertThat(savedStudent.orElseThrow()).isSameAs(student);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentUpdateInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        reset(actionExecutor, applicationContext);
        Student newStudent = makeClearStudent(5);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        RuntimeException exception = new RuntimeException("Cannot process nested commands");
        doThrow(exception).when(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
    }

    @Test
    void shouldNotExecuteDoCommand_getCommandResultThrows() {
        Long profileId = 21L;
        Long studentId = 32L;
        adjustStudentProfileSaving(profileId);
        adjustStudentSaving(studentId);
        Student newStudent = makeClearStudent(15);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        RuntimeException exception = new RuntimeException("Cannot get command result");
        doThrow(exception).when(command).finalCommandResult(any(Deque.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(profileContext);
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        assertThat(profileResult.orElseThrow().getId()).isEqualTo(profileId);
        assertThat(profileContext.getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(studentContext);
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        final Student student = studentResult.orElseThrow();
        assertStudentEquals(student, newStudent, false);
        assertThat(student.getId()).isEqualTo(studentId);
        assertThat(student.getProfileId()).isEqualTo(profileId);
        assertThat(studentContext.getUndoParameter().value()).isEqualTo(studentId);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentUpdateInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);
    }

    @Test
    void shouldNotExecuteDoCommand_CreateProfileDoNestedCommandsThrows() {
        reset(applicationContext);
        ReflectionTestUtils.setField(profileCommand, "applicationContext", applicationContext);
        doReturn(profileCommand).when(applicationContext).getBean("profileStudentUpdate", StudentProfileCommand.class);

        Student newStudent = makeClearStudent(6);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        RuntimeException exception = new RuntimeException("Cannot process profile nested command");
        doThrow(exception).when(persistence).save(any(StudentProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isEqualTo(exception);

        assertThat(studentContext.getState()).isEqualTo(CANCEL);

        verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(StudentProfile.class));

        verify(command, never()).transferResult(eq(profileCommand), any(), eq(studentContext));
        verify(command, never()).transferProfileIdToStudentUpdateInput(anyLong(), any(Context.class));

        verify(command, never()).executeDoNested(eq(studentContext), any(Context.StateChangedListener.class));
        verify(personCommand, never()).doCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_CreateStudentDoNestedCommandsThrows() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 10L;
        adjustStudentProfileSaving(profileId);
        Student newStudent = makeClearStudent(7);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        RuntimeException exception = new RuntimeException("Cannot process student nested command");
        doThrow(exception).when(persistence).save(any(Student.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();

        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        assertThat(studentContext.isFailed()).isTrue();
        Student student = studentContext.<Student>getRedoParameter().value();
        assertStudentEquals(student, newStudent, false);
        assertThat(student.getProfileId()).isEqualTo(profileId);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferResult(eq(profileCommand), any(), eq(studentContext));
        verify(command).transferProfileIdToStudentUpdateInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileContext, profileId);
    }

    @Test
    void shouldExecuteUndoCommand() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 11L;
        Long studentId = 21L;
        Student newStudent = makeClearStudent(8);
        adjustStudentProfileSaving(profileId);
        adjustStudentSaving(studentId);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));

        command.doCommand(context);
        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.getState()).isEqualTo(UNDONE);
        assertThat(studentContext.getState()).isEqualTo(UNDONE);

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyStudentUndoCommand(studentContext, studentId);

        // nested commands order
        checkUndoNestedCommandsOrder(profileContext, studentContext, studentId, profileId);
    }

    @Test
    void shouldNotExecuteUndoCommand_UndoNestedCommandsThrowsException() {
        Long profileId = 5L;
        Long studentId = 6L;
        Student newStudent = makeClearStudent(11);
        adjustStudentProfileSaving(profileId);
        adjustStudentSaving(studentId);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(command).rollbackNested(any(Deque.class));

        command.doCommand(context);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(personCommand, never()).undoCommand(any(Context.class));
        verify(profileCommand, never()).undoCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteUndoCommand_StudentUndoThrowsException() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 12L;
        Long studentId = 22L;
        Student newStudent = makeClearStudent(9);
        adjustStudentProfileSaving(profileId);
        adjustStudentSaving(studentId);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(persistence).deleteStudent(studentId);

        command.doCommand(context);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<?> profileContext = parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verifyStudentUndoCommand(studentContext, studentId);
    }

    @Test
    void shouldNotExecuteUndoCommand_ProfileUndoThrowsException() {
        doCallRealMethod().when(actionExecutor).rollbackAction(any(ActionContext.class), any(Context.class));
        Long profileId = 3L;
        Long studentId = 4L;
        Student student = makeClearStudent(10);
        adjustStudentProfileSaving(profileId);
        adjustStudentSaving(studentId);
        Context<Optional<Student>> context = command.createContext(Input.of(student));
        command.doCommand(context);
        reset(persistence, command, personCommand);
        RuntimeException exception = new RuntimeException("Cannot process profile undo command");
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isEqualTo(exception);
        assertThat(studentContext.isUndone()).isFalse();
        assertThat(studentContext.isDone()).isTrue();

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyStudentUndoCommand(studentContext, studentId);
        verifyStudentDoCommand(studentContext);
    }

    // private methods
    private void adjustStudentSaving(Long studentId) {
        doAnswer(invocation -> {
            Student student = invocation.getArgument(0, Student.class);
            StudentPayload result = payloadMapper.toPayload(student);
            result.setId(studentId);
            return Optional.of(result);
        }).when(persistence).save(any(Student.class));
    }

    private void adjustStudentProfileSaving(Long profileId) {
        doAnswer(invocation -> {
            StudentProfile profile = invocation.getArgument(0, StudentProfile.class);
            StudentProfilePayload result = payloadMapper.toPayload(profile);
            result.setId(profileId);
            return Optional.of(result);
        }).when(persistence).save(any(StudentProfile.class));
    }

    private void checkUndoNestedCommandsOrder(Context<Optional<StudentProfile>> profileContext,
                                              Context<Optional<Student>> studentContext,
                                              Long studentId, Long profileId) {
        // nested commands order
        InOrder inOrder = Mockito.inOrder(command);
        // creating profile and student (profile is first) avers commands order
        inOrder.verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        inOrder.verify(command).executeDoNested(eq(studentContext), any(Context.StateChangedListener.class));
        // undo creating profile and student (student is first) revers commands order
        inOrder.verify(command).executeUndoNested(studentContext);
        inOrder.verify(command).executeUndoNested(profileContext);

        // persistence operations order
        inOrder = Mockito.inOrder(persistence);
        // creating profile and student (profile is first) avers operations order
        inOrder.verify(persistence).save(any(StudentProfile.class));
        inOrder.verify(persistence).save(any(Student.class));
        // undo creating profile and student (student is first) revers operations order
        inOrder.verify(persistence).deleteStudent(studentId);
        inOrder.verify(persistence).deleteProfileById(profileId);
    }

    private void verifyProfileUndoCommand(Context<Optional<StudentProfile>> profileContext, Long id) {
        verify(command).executeUndoNested(profileContext);
        verify(profileCommand).undoCommand(profileContext);
        verify(profileCommand).executeUndo(profileContext);
        verify(persistence).deleteProfileById(id);
    }

    private void verifyStudentUndoCommand(Context<Optional<Student>> studentContext, Long id) {
        verify(command).executeUndoNested(studentContext);
        verify(personCommand).undoCommand(studentContext);
        verify(personCommand).executeUndo(studentContext);
        verify(persistence).deleteStudent(id);
    }

    private void verifyProfileDoCommand(Context<?> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContext;
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(StudentProfile.class));
    }

    private void verifyStudentDoCommand(Context<?> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any());
        Context<Optional<Student>> personContext = (Context<Optional<Student>>) nestedContext;
        verify(personCommand).doCommand(personContext);
        verify(personCommand).executeDo(personContext);
        verify(persistence).save(any(Student.class));
    }

    private static <T> void checkContextAfterDoCommand(Context<Optional<T>> context) {
        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().get()).isPresent();
    }

    private void verifyStudentCommandContext(StudentPayload newStudent, StudentCommand<?> studentCommand) {
        Input<?> inputNewStudent = Input.of(newStudent);
        verify(studentCommand).acceptPreparedContext(command, inputNewStudent);
        verify(command).prepareContext(studentCommand, inputNewStudent);
        verify(command).createPersonContext(studentCommand, newStudent);
        verify(studentCommand).createContext(any(Input.class));
    }

    private void verifyProfileCommandContext(StudentPayload newStudent, StudentProfileCommand<?> profileCommand) {
        Input<?> inputNewStudent = Input.of(newStudent);
        verify(profileCommand).acceptPreparedContext(command, inputNewStudent);
        verify(command).prepareContext(profileCommand, inputNewStudent);
        verify(command).createProfileContext(profileCommand, newStudent);
        verify(profileCommand).createContext(any(Input.class));
    }
}