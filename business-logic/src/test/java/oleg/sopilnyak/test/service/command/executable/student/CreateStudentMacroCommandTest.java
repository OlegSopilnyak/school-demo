package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
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
    CreateOrUpdateStudentCommand personCommand;
    @Mock
    ActionExecutor actionExecutor;

    CreateStudentMacroCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new CreateStudentMacroCommand(personCommand, profileCommand, payloadMapper, actionExecutor) {
            @Override
            public NestedCommand<?> wrap(NestedCommand<?> command) {
                return spy(super.wrap(command));
            }
        });
    }

    @AfterEach
    void tearDown() {
        reset(payloadMapper);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        NestedCommand<?> nestedProfileCommand = nested.pop();
        if (nestedProfileCommand instanceof SequentialMacroCommand.Chained<?> chained) {
            assertThat(chained.unWrap()).isSameAs(profileCommand);
        } else {
            fail("nested profile command is not a chained command");
        }
        NestedCommand<?> nestedStudentCommand = nested.pop();
        if (nestedStudentCommand instanceof SequentialMacroCommand.Chained<?> chained) {
            assertThat(chained.unWrap()).isSameAs(personCommand);
        } else {
            fail("nested student command is not a chained command");
        }
    }

    @Test
    void shouldCreateMacroCommandContexts() {
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
        Object wrongTypeInput = "something";
        Deque<NestedCommand<?>> nested = new LinkedList<>(command.fromNest());
        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nested.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nested.pop();

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Optional<Student>> context = command.createContext(wrongInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.CREATE_OR_UPDATE);
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

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
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

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);
    }

    @Test
    void shouldNotExecuteDoCommand_CreateProfileDoNestedCommandsThrows() {
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

//        verify(profileCommand).doAsNestedCommand(eq(command), eq(profileContext), any(Context.StateChangedListener.class));
//        verify(command).doNestedCommand(eq(profileCommand), eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(StudentProfile.class));

        verify(command, never()).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any(Context.class));

//        verify(personCommand, never()).doAsNestedCommand(any(NestedCommandExecutionVisitor.class), any(Context.class), any(Context.StateChangedListener.class));
//        verify(command, never()).doNestedCommand(any(RootCommand.class), any(Context.class), any(Context.StateChangedListener.class));
    }

    @Test
    void shouldNotExecuteDoCommand_CreateStudentDoNestedCommandsThrows() {
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

        verify(command).transferPreviousExecuteDoResult(eq(profileCommand), any(Optional.class), eq(studentContext));
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileContext, profileId);
    }

    @Test
    void shouldExecuteUndoCommand() {
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
        doThrow(exception).when(command).rollbackNestedDone(any(Input.class));

        command.doCommand(context);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNestedDone(any(Input.class));
//        verify(personCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
//        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldNotExecuteUndoCommand_StudentUndoThrowsException() {
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
        Long profileId = 3L;
        Long studentId = 4L;
        Student newStudent = makeClearStudent(10);
        adjustStudentProfileSaving(profileId);
        adjustStudentSaving(studentId);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        RuntimeException exception = new RuntimeException("Cannot process profile undo command");

        command.doCommand(context);
        reset(persistence, command, personCommand);
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
//        inOrder.verify(command).doNestedCommand(eq(profileCommand), eq(profileContext), any(Context.StateChangedListener.class));
//        inOrder.verify(command).doNestedCommand(eq(personCommand), eq(studentContext), any(Context.StateChangedListener.class));
        // undo creating profile and student (student is first) revers commands order
//        inOrder.verify(command).undoNestedCommand(personCommand, studentContext);
//        inOrder.verify(command).undoNestedCommand(profileCommand, profileContext);

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
//        verify(profileCommand).undoAsNestedCommand(command, profileContext);
//        verify(command).undoNestedCommand(profileCommand, profileContext);
        verify(profileCommand).undoCommand(profileContext);
        verify(profileCommand).executeUndo(profileContext);
        verify(persistence).deleteProfileById(id);
    }

    private void verifyStudentUndoCommand(Context<Optional<Student>> studentContext, Long id) {
//        verify(personCommand).undoAsNestedCommand(command, studentContext);
//        verify(command).undoNestedCommand(personCommand, studentContext);
        verify(personCommand).undoCommand(studentContext);
        verify(personCommand).executeUndo(studentContext);
        verify(persistence).deleteStudent(id);
    }

    private void verifyProfileDoCommand(Context<?> nestedContext) {
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContext;
//        verify(profileCommand).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
//        verify(command).doNestedCommand(eq(profileCommand), eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(StudentProfile.class));
    }

    private void verifyStudentDoCommand(Context<?> nestedContext) {
        Context<Optional<Student>> personContext = (Context<Optional<Student>>) nestedContext;
//        verify(personCommand).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
//        verify(command).doNestedCommand(eq(personCommand), eq(personContext), any(Context.StateChangedListener.class));
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