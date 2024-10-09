package oleg.sopilnyak.test.end2end.command.executable.student;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.base.BaseType;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.command.executable.student.CreateStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
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
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class,
        CreateOrUpdateStudentProfileCommand.class,
        CreateOrUpdateStudentCommand.class,
        CreateStudentMacroCommand.class,
        TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CreateStudentMacroCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    CreateOrUpdateStudentProfileCommand profileCommand;
    @SpyBean
    @Autowired
    CreateOrUpdateStudentCommand studentCommand;

    CreateStudentMacroCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new CreateStudentMacroCommand(studentCommand, profileCommand, payloadMapper){
            @Override
            public NestedCommand wrap(NestedCommand command) {
                return spy(super.wrap(command));
            }
        });
    }

    @AfterEach
    void tearDown() {
        reset(command, profileCommand, studentCommand, persistence, payloadMapper);
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateMacroCommandContexts() {
        StudentPayload newStudent = payloadMapper.toPayload(makeClearStudent(1));
        reset(payloadMapper);
        Deque<NestedCommand> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand nestedProfileCommand = (StudentProfileCommand) nestedCommands.pop();
        StudentCommand nestedStudentCommand = (StudentCommand) nestedCommands.pop();

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
        assertThat(studentContext.getCommand()).isSameAs(nestedStudentCommand);
        Student student = studentContext.getRedoParameter();
        assertThat(student).isNotNull();
        assertThat(student.getId()).isNull();
        assertStudentEquals(newStudent, student);
        String emailPrefix = student.getFirstName().toLowerCase() + "." + student.getLastName().toLowerCase();

        assertThat(profileContext).isNotNull();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(profileContext.getCommand()).isSameAs(nestedProfileCommand);
        StudentProfile profile = profileContext.getRedoParameter();
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNull();
        assertThat(profile.getEmail()).startsWith(emailPrefix);
        assertThat(profile.getPhone()).isNotEmpty();

        verifyProfileCommandContext(newStudent, nestedProfileCommand);

        verifyStudentCommandContext(newStudent, nestedStudentCommand);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";
        Deque<NestedCommand> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand nestedProfileCommand = (StudentProfileCommand) nestedCommands.pop();
        StudentCommand nestedStudentCommand = (StudentCommand) nestedCommands.pop();

        Context<Student> context = command.createContext(wrongTypeInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.CREATE_OR_UPDATE);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(nestedProfileCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(nestedProfileCommand, wrongTypeInput);
        verify(command, never()).createProfileContext(eq(nestedProfileCommand), any());
        verify(nestedProfileCommand, never()).createContext(any(StudentProfilePayload.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(nestedStudentCommand, wrongTypeInput);
        verify(command, never()).createPersonContext(eq(nestedStudentCommand), any());
        verify(nestedStudentCommand, never()).createContext(any(StudentPayload.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_CreateProfileContextThrows() {
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        Student newStudent = makeClearStudent(2);
        Deque<NestedCommand> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand nestedProfileCommand = (StudentProfileCommand) nestedCommands.pop();
        StudentCommand nestedStudentCommand = (StudentCommand) nestedCommands.pop();
        when(nestedProfileCommand.createContext(any(StudentProfilePayload.class))).thenThrow(exception);

        Context<Student> context = command.createContext(newStudent);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(nestedProfileCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(nestedProfileCommand, newStudent);
        verify(command).createProfileContext(nestedProfileCommand, newStudent);
        verify(nestedProfileCommand).createContext(any(StudentProfilePayload.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(nestedStudentCommand, newStudent);
        verify(command).createPersonContext(nestedStudentCommand, newStudent);
        verify(nestedStudentCommand).createContext(any(StudentPayload.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        String errorMessage = "Cannot create nested student context";
        RuntimeException exception = new RuntimeException(errorMessage);
        Student newStudent = makeClearStudent(3);
        Deque<NestedCommand> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand nestedProfileCommand = (StudentProfileCommand) nestedCommands.pop();
        StudentCommand nestedStudentCommand = (StudentCommand) nestedCommands.pop();
        when(nestedStudentCommand.createContext(any(StudentPayload.class))).thenThrow(exception);

        Context<Student> context = command.createContext(newStudent);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(nestedProfileCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(nestedProfileCommand, newStudent);
        verify(command).createProfileContext(nestedProfileCommand, newStudent);
        verify(nestedProfileCommand).createContext(any(StudentProfilePayload.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(nestedStudentCommand, newStudent);
        verify(command).createPersonContext(nestedStudentCommand, newStudent);
        verify(nestedStudentCommand).createContext(any(StudentPayload.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteDoCommand() {
        Student newStudent = makeClearStudent(4);
        Context<Optional<Student>> context = command.createContext(newStudent);

        command.doCommand(context);

        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Context<Optional<BaseType>> profileContext = parameter.getNestedContexts().pop();
        Context<Optional<BaseType>> studentContext = parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(profileContext);
        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        assertThat(profileContext.<Long>getUndoParameter()).isEqualTo(profileId);

        checkContextAfterDoCommand(context);
        Optional<Student> savedStudent = context.getResult().orElseThrow();
        Long studentId = savedStudent.orElseThrow().getId();
        assertThat(savedStudent.orElseThrow().getProfileId()).isEqualTo(profileId);
        assertStudentEquals(savedStudent.orElseThrow(), newStudent, false);

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

        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        Context<Optional<Student>> context = command.createContext(makeClearStudent(5));
        RuntimeException exception = new RuntimeException("Cannot process nested commands");
        doThrow(exception).when(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_getCommandResultThrows() {
        Student newStudent = makeClearStudent(15);
        Context<Optional<Student>> context = command.createContext(newStudent);
        RuntimeException exception = new RuntimeException("Cannot get command result");
        doThrow(exception).when(command).getDoCommandResult(any(Deque.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Context<Optional<BaseType>> profileContext = parameter.getNestedContexts().pop();
        Context<Optional<BaseType>> studentContext = parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(profileContext);
        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        assertThat(profileContext.<Long>getUndoParameter()).isEqualTo(profileId);

        checkContextAfterDoCommand(studentContext);
        Optional<BaseType> studentResult = studentContext.getResult().orElseThrow();
        final Student student = studentResult.map(Student.class::cast).orElseThrow();
        assertStudentEquals(student, newStudent, false);
        Long studentId = student.getId();
        assertThat(student.getProfileId()).isEqualTo(profileId);
        assertThat(studentContext.<Long>getUndoParameter()).isEqualTo(studentId);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);

        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_CreateStudentDoNestedCommandsThrows() {
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
        Long profileId = profileResult.orElseThrow().getId();
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
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteUndoCommand() {
        Student newStudent = makeClearStudent(8);
        Context<Optional<Student>> context = command.createContext(newStudent);
        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Deque<Context<Optional<BaseType>>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<BaseType>> profileContext = nestedContexts.pop();
        Context<Optional<BaseType>> studentContext = nestedContexts.pop();

        command.doCommand(context);
        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        Optional<BaseType> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(((Student)studentResult.orElseThrow()).getProfileId());
        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);

        assertThat(profileContext.getState()).isEqualTo(UNDONE);
        assertThat(studentContext.getState()).isEqualTo(UNDONE);

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyStudentUndoCommand(studentContext, studentId);
        // nested commands order
        checkUndoNestedCommandsOrder(profileContext, studentContext, studentId, profileId);

        assertThat(persistence.findStudentById(studentId)).isEmpty();
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_UndoNestedCommandsThrowsException() {
        Student newStudent = makeClearStudent(11);
        Context<Optional<Student>> context = command.createContext(newStudent);
        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Deque<Context<Optional<BaseType>>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<BaseType>> profileContext = nestedContexts.pop();
        Context<Optional<BaseType>> studentContext = nestedContexts.pop();

        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(command).undoNestedCommands(any(Deque.class));

        command.doCommand(context);
        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        Optional<BaseType> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(((Student)studentResult.orElseThrow()).getProfileId());
        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));
        verify(studentCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));

        assertThat(persistence.findStudentById(studentId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_StudentUndoThrowsException() {
        Student newStudent = makeClearStudent(9);
        Context<Optional<Student>> context = command.createContext(newStudent);
        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Deque<Context<Optional<BaseType>>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<BaseType>> profileContext = nestedContexts.pop();
        Context<Optional<BaseType>> studentContext = nestedContexts.pop();

        RuntimeException exception = new RuntimeException("Cannot process student undo command");

        command.doCommand(context);
        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        Optional<BaseType> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(((Student)studentResult.orElseThrow()).getProfileId());
        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        doThrow(exception).when(persistence).deleteStudent(studentId);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        assertThat(profileContext.isDone()).isTrue();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verifyStudentUndoCommand(studentContext, studentId);

        assertThat(persistence.findStudentById(studentId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_ProfileUndoThrowsException() {
        Student newStudent = makeClearStudent(10);
        Context<Optional<Student>> context = command.createContext(newStudent);
        MacroCommandParameter<Optional<BaseType>> parameter = context.getRedoParameter();
        Deque<Context<Optional<BaseType>>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<BaseType>> profileContext = nestedContexts.pop();
        Context<Optional<BaseType>> studentContext = nestedContexts.pop();

        RuntimeException exception = new RuntimeException("Cannot process profile undo command");

        command.doCommand(context);
        reset(persistence, command, studentCommand);
        Optional<BaseType> profileResult = profileContext.getResult().orElseThrow();
        Optional<BaseType> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(((Student)studentResult.orElseThrow()).getProfileId());
        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        doThrow(exception).when(persistence).deleteProfileById(profileId);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isEqualTo(exception);
        assertThat(studentContext.isUndone()).isFalse();
        assertThat(studentContext.isDone()).isTrue();

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyStudentUndoCommand(studentContext, studentId);
        verifyStudentDoCommand(studentContext);

        studentId = studentContext.getResult().orElseThrow().orElseThrow().getId();
        profileId = profileContext.getResult().orElseThrow().orElseThrow().getId();
        assertThat(persistence.findStudentById(studentId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
    }

    // private methods
    private void checkUndoNestedCommandsOrder(Context<Optional<BaseType>> profileContext, Context<Optional<BaseType>> studentContext, Long studentId, Long profileId) {
        // nested commands order
        InOrder inOrder = Mockito.inOrder(command);
        // creating profile and student (profile is first) avers commands order
        inOrder.verify(command).doNestedCommand(eq(profileCommand), eq(profileContext), any(Context.StateChangedListener.class));
        inOrder.verify(command).doNestedCommand(eq(studentCommand), eq(studentContext), any(Context.StateChangedListener.class));
        // undo creating profile and student (student is first) revers commands order
        inOrder.verify(command).undoNestedCommand(studentCommand, studentContext);
        inOrder.verify(command).undoNestedCommand(profileCommand, profileContext);

        // persistence operations order
        inOrder = Mockito.inOrder(persistence);
        // creating profile and student (profile is first) avers operations order
        inOrder.verify(persistence).save(any(StudentProfile.class));
        inOrder.verify(persistence).save(any(Student.class));
        // undo creating profile and student (student is first) revers operations order
        inOrder.verify(persistence).deleteStudent(studentId);
        inOrder.verify(persistence).deleteProfileById(profileId);
    }

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

    private void verifyStudentCommandContext(StudentPayload newStudent, StudentCommand studentCommand) {
        verify(studentCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(studentCommand, newStudent);
        verify(command).createPersonContext(studentCommand, newStudent);
        verify(studentCommand).createContext(any(StudentPayload.class));
    }

    private void verifyProfileCommandContext(StudentPayload newStudent, StudentProfileCommand profileCommand) {
        verify(profileCommand).acceptPreparedContext(command, newStudent);
        verify(command).prepareContext(profileCommand, newStudent);
        verify(command).createProfileContext(profileCommand, newStudent);
        verify(profileCommand).createContext(any(StudentProfilePayload.class));
    }
}