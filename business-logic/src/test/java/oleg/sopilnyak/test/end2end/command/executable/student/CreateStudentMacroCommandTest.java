package oleg.sopilnyak.test.end2end.command.executable.student;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateStudentMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        PersistenceConfiguration.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class CreateStudentMacroCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    @Qualifier("profileStudentUpdate")
    StudentProfileCommand profileCommand;
    @MockitoSpyBean
    @Autowired
    @Qualifier("studentUpdate")
    StudentCommand studentCommand;
    @MockitoSpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @Autowired
    CommandThroughMessageService messagesExchangeService;

    CreateStudentMacroCommand command;
    @Captor
    ArgumentCaptor<StudentCommand> personCaptor;
    @Captor
    ArgumentCaptor<StudentProfileCommand> profileCaptor;

    @BeforeEach
    void setUp() {
        command = spy(new CreateStudentMacroCommand(studentCommand, profileCommand, payloadMapper, actionExecutor) {
            @Override
            public NestedCommand<?> wrap(NestedCommand<?> command) {
                return spy(super.wrap(command));
            }
        });
        ActionContext.setup("test-facade", "test-processing");
        messagesExchangeService.initialize();
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentProfileEntity.class);
    }

    @AfterEach
    void tearDown() {
        reset(command, profileCommand, studentCommand, persistence, payloadMapper);
        messagesExchangeService.shutdown();
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentProfileEntity.class);
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
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nestedCommands.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nestedCommands.pop();

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Optional<Student>> context = command.createContext(Input.of(wrongInput));

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.CommandId.CREATE_OR_UPDATE);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(nestedProfileCommand, wrongInput);
        verify(command, never()).createProfileContext(eq(nestedProfileCommand), any());
        verify(nestedProfileCommand, never()).createContext(any(Input.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(nestedStudentCommand, wrongInput);
        verify(command, never()).createPersonContext(eq(nestedStudentCommand), any());
        verify(nestedStudentCommand, never()).createContext(any(Input.class));
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateProfileContextThrows() {
        Student newStudent = makeClearStudent(2);
        Input<Student> newStudentInput = (Input<Student>) Input.of(newStudent);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nestedCommands.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nestedCommands.pop();

        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(nestedProfileCommand.createContext(any(Input.class))).thenThrow(exception);
        Context<Optional<Student>> context = command.createContext(newStudentInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, newStudentInput);
        verify(command).prepareContext(nestedProfileCommand, newStudentInput);
        verify(command).createProfileContext(nestedProfileCommand, newStudentInput.value());
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, newStudentInput);
        verify(command).prepareContext(nestedStudentCommand, newStudentInput);
        verify(command).createPersonContext(nestedStudentCommand, newStudentInput.value());
        verify(nestedStudentCommand).createContext(newStudentInput);
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreatePersonContextThrows() {
        Student newStudent = makeClearStudent(3);
        Deque<NestedCommand<?>> nestedCommands = new LinkedList<>(command.fromNest());
        StudentProfileCommand<?> nestedProfileCommand = (StudentProfileCommand<?>) nestedCommands.pop();
        StudentCommand<?> nestedStudentCommand = (StudentCommand<?>) nestedCommands.pop();
        Input<Student> newStudentInput = (Input<Student>) Input.of(newStudent);

        String errorMessage = "Cannot create nested student context";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(nestedStudentCommand.createContext(newStudentInput)).thenThrow(exception);
        Context<Optional<Student>> context = command.createContext(newStudentInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(nestedProfileCommand).acceptPreparedContext(command, newStudentInput);
        verify(command).prepareContext(nestedProfileCommand, newStudentInput);
        verify(command).createProfileContext(nestedProfileCommand, newStudentInput.value());
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, newStudentInput);
        verify(command).prepareContext(nestedStudentCommand, newStudentInput);
        verify(command).createPersonContext(nestedStudentCommand, newStudentInput.value());
        verify(nestedStudentCommand).createContext(newStudentInput);
    }

    @Test
    void shouldExecuteDoCommand() {
        Student newStudent = makeClearStudent(4);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));

        command.doCommand(context);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();

        checkContextAfterDoCommand(profileContext);
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(context);
        Optional<Student> savedStudent = context.getResult().orElseThrow();
        Long studentId = savedStudent.orElseThrow().getId();
        assertThat(savedStudent.orElseThrow().getProfileId()).isEqualTo(profileId);
        assertStudentEquals(savedStudent.orElseThrow(), newStudent, false);

        checkContextAfterDoCommand(studentContext);
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        final Student student = studentResult.orElseThrow();
        assertStudentEquals(student, newStudent, false);
        assertThat(student.getId()).isEqualTo(studentId);
        assertThat(student.getProfileId()).isEqualTo(profileId);
        assertThat(studentContext.<Long>getUndoParameter().value()).isEqualTo(studentId);
        assertThat(savedStudent.orElseThrow()).isSameAs(student);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);

        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
    }

    @Test
    void shouldNotExecuteDoCommand_DoNestedCommandsThrows() {
        Context<Optional<Student>> context = command.createContext(Input.of(makeClearStudent(5)));
        RuntimeException exception = new RuntimeException("Cannot process nested commands");
        doThrow(exception).when(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
    }

    @Test
    void shouldNotExecuteDoCommand_getCommandResultThrows() {
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
        Long profileId = profileResult.orElseThrow().getId();
        assertThat(profileContext.<Long>getUndoParameter().value()).isEqualTo(profileId);

        checkContextAfterDoCommand(studentContext);
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        final Student student = studentResult.orElseThrow();
        assertStudentEquals(student, newStudent, false);
        Long studentId = student.getId();
        assertThat(student.getProfileId()).isEqualTo(profileId);
        assertThat(studentContext.<Long>getUndoParameter().value()).isEqualTo(studentId);

        verify(command).executeDo(context);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));

        verifyProfileDoCommand(profileContext);

        verify(command).transferPreviousExecuteDoResult(profileCommand, profileContext.getResult().get(), studentContext);
        verify(command).transferProfileIdToStudentInput(profileId, studentContext);

        verifyStudentDoCommand(studentContext);

        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
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
        Context<Optional<Student>> personContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isEqualTo(exception);

        assertThat(personContext.getState()).isEqualTo(CANCEL);

        verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(profileContext);
        verify(profileCommand).executeDo(profileContext);
        verify(persistence).save(any(StudentProfile.class));

        verify(command, never()).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any(Context.class));

        verify(command, never()).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any(Context.class));

        verify(command, never()).executeDoNested(eq(personContext), any(Context.StateChangedListener.class));
        verify(studentCommand, never()).doCommand(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_CreateStudentDoNestedCommandsThrows() {
        Student newStudent = makeClearStudent(7);
        Input<Student> input = (Input<Student>) Input.of(newStudent);
        Context<Optional<Student>> context = command.createContext(input);

        RuntimeException exception = new RuntimeException("Cannot process student nested command");
        doThrow(exception).when(persistence).save(input.value());
        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isEqualTo(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        Long profileId = profileContext.<Long>getUndoParameter().value();

        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();
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
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
    }

    @Test
    void shouldExecuteUndoCommand() {
        Student person = makeClearStudent(8);
        Context<Optional<Student>> context = command.createContext(Input.of(person));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        command.doCommand(context);
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        assertStudentEquals(findStudentEntity(studentId), person, false);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);

        assertThat(profileContext.getState()).isEqualTo(UNDONE);
        assertThat(studentContext.getState()).isEqualTo(UNDONE);

        verify(command).executeUndo(context);
        verifyProfileUndoCommand(profileContext, profileId);
        verifyStudentUndoCommand(studentContext, studentId);
        // nested commands order
        checkUndoNestedCommandsOrder(profileContext, studentContext, studentId, profileId);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }


    @Test
    void shouldNotExecuteUndoCommand_UndoNestedCommandsThrowsException() {
        Student newStudent = makeClearStudent(11);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();

        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(command).rollbackNested(any(Deque.class));

        command.doCommand(context);
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));

        assertThat(persistence.findStudentById(studentId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
    }

    @Test
    void shouldNotExecuteUndoCommand_StudentUndoThrowsException() {
        Student newStudent = makeClearStudent(9);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();

        RuntimeException exception = new RuntimeException("Cannot process student undo command");

        command.doCommand(context);
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
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
    void shouldNotExecuteUndoCommand_ProfileUndoThrowsException() {
        Student newStudent = makeClearStudent(10);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        command.doCommand(context);
        reset(persistence, command, studentCommand);
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        assertStudentEquals(persistence.findStudentById(studentId).orElseThrow(), newStudent, false);
        RuntimeException exception = new RuntimeException("Cannot process profile undo command");
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.undoCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) == null);

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

        assertThat(findStudentEntity(studentId)).isNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
        Long resultPersonId = studentContext.getResult().orElseThrow().orElseThrow().getId();
        assertThat(findStudentEntity(resultPersonId)).isNotNull();
    }

    // private methods
    private StudentEntity findStudentEntity(Long id) {
        return findEntity(StudentEntity.class, id, student -> student.getCourseSet().size());
    }

    private StudentProfileEntity findProfileEntity(Long id) {
        return findEntity(StudentProfileEntity.class, id);
    }

    private void checkUndoNestedCommandsOrder(Context<Optional<StudentProfile>> profileContext,
                                              Context<Optional<Student>> personContext,
                                              Long studentId, Long profileId) {
        // nested commands order
        InOrder inOrder = Mockito.inOrder(command);
        // creating profile and student (profile is first) avers commands order
        inOrder.verify(command).executeDoNested(eq(profileContext), any(Context.StateChangedListener.class));
        inOrder.verify(command).executeDoNested(eq(personContext), any(Context.StateChangedListener.class));
        // undo creating profile and student (student is first) revers commands order
        inOrder.verify(command).executeUndoNested(personContext);
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

    private void verifyProfileUndoCommand(Context<Optional<StudentProfile>> nestedContext, Long id) {
        verify(command).executeUndoNested(nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
        verify(persistence).deleteProfileById(id);
    }

    private void verifyStudentUndoCommand(Context<Optional<Student>> nestedContext, Long id) {
        verify(command).executeUndoNested(nestedContext);
        verify(studentCommand).undoCommand(nestedContext);
        verify(studentCommand).executeUndo(nestedContext);
        verify(persistence).deleteStudent(id);
    }

    private void verifyProfileDoCommand(Context<Optional<StudentProfile>> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand).doCommand(nestedContext);
        verify(profileCommand).executeDo(nestedContext);
        verify(persistence).save(any(StudentProfile.class));
    }

    private void verifyStudentDoCommand(Context<Optional<Student>> nestedContext) {
        verify(command).executeDoNested(eq(nestedContext), any());
        verify(studentCommand).doCommand(nestedContext);
        verify(studentCommand).executeDo(nestedContext);
        verify(persistence).save(any(Student.class));
    }

    private static <T> void checkContextAfterDoCommand(Context<Optional<T>> context) {
        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        assertThat(context.getResult().get()).isPresent();
    }

    private void verifyStudentCommandContext(StudentPayload newStudent, StudentCommand<?> studentCommand) {
        Input<?> newStudentInput = Input.of(newStudent);
        verify(studentCommand).acceptPreparedContext(command, newStudentInput);
        verify(command).prepareContext(studentCommand, newStudentInput);
        verify(command).createPersonContext(studentCommand, newStudent);
        verify(studentCommand).createContext(any(Input.class));
    }

    private void verifyProfileCommandContext(StudentPayload newStudent, StudentProfileCommand<?> profileCommand) {
        Input<?> newStudentInput = Input.of(newStudent);
        verify(profileCommand).acceptPreparedContext(command, newStudentInput);
        verify(command).prepareContext(profileCommand, newStudentInput);
        verify(command).createProfileContext(profileCommand, newStudent);
        verify(profileCommand).createContext(any(Input.class));
    }
}