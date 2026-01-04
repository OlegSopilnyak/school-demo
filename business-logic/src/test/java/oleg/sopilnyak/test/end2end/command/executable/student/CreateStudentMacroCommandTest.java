package oleg.sopilnyak.test.end2end.command.executable.student;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        PersistenceConfiguration.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
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
    CommandActionExecutor actionExecutor;
    @Autowired
    CommandThroughMessageService messagesExchangeService;

    CreateStudentMacroCommand command;
    @Captor
    ArgumentCaptor<StudentCommand> personCaptor;
    @Captor
    ArgumentCaptor<StudentProfileCommand> profileCaptor;

    @BeforeEach
    void setUp() {
        command = spy(new CreateStudentMacroCommand(studentCommand, profileCommand, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-doingMainLoop");
    }

    @AfterEach
    void tearDown() {
        reset(command, profileCommand, studentCommand, persistence, payloadMapper);
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
        assertEquals(studentContext.getCommand(), nestedStudentCommand);
        nestedStudentCommand = (StudentCommand<?>) studentContext.getCommand();
        Student student = studentContext.<Student>getRedoParameter().value();
        assertThat(student).isNotNull();
        assertThat(student.getId()).isNull();
        assertStudentEquals(newStudent, student);
        String emailPrefix = student.getFirstName().toLowerCase() + "." + student.getLastName().toLowerCase();

        assertThat(profileContext).isNotNull();
        assertThat(profileContext.isReady()).isTrue();
        assertEquals(profileContext.getCommand(), nestedProfileCommand);
        nestedProfileCommand = (StudentProfileCommand<?>) profileContext.getCommand();
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
        ArgumentCaptor<StudentProfileCommand> profileCmdCaptor = ArgumentCaptor.forClass(StudentProfileCommand.class);
        ArgumentCaptor<Input> profileInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(profileCmdCaptor.capture(), profileInputCaptor.capture());
        // check profile command-context
        assertEquals(profileCmdCaptor.getValue(), nestedProfileCommand);
        nestedProfileCommand = profileCmdCaptor.getValue();
        assertThat(profileInputCaptor.getValue()).isSameAs(wrongInput);

        verify(command, never()).createProfileContext(any(StudentProfileCommand.class), any());
        verify(nestedProfileCommand, never()).createContext(any(Input.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, wrongInput);
        ArgumentCaptor<StudentCommand> personCmdCaptor = ArgumentCaptor.forClass(StudentCommand.class);
        ArgumentCaptor<Input> personInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(personCmdCaptor.capture(), personInputCaptor.capture());
        // check person command-context
        assertEquals(personCmdCaptor.getValue(), nestedStudentCommand);
        nestedStudentCommand = personCmdCaptor.getValue();

        verify(command, never()).createPersonContext(any(StudentCommand.class), any());
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
        ArgumentCaptor<StudentProfileCommand> profileCmdCaptor = ArgumentCaptor.forClass(StudentProfileCommand.class);
        ArgumentCaptor<Input> profileInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(profileCmdCaptor.capture(), profileInputCaptor.capture());
        // check profile command-context
        assertEquals(profileCmdCaptor.getValue(), nestedProfileCommand);
        nestedProfileCommand = profileCmdCaptor.getValue();
        assertThat(profileInputCaptor.getValue()).isEqualTo(newStudentInput);
        verify(command).createProfileContext(nestedProfileCommand, newStudentInput.value());
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, newStudentInput);
        ArgumentCaptor<StudentCommand> personCmdCaptor = ArgumentCaptor.forClass(StudentCommand.class);
        ArgumentCaptor<Input> personInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(personCmdCaptor.capture(), personInputCaptor.capture());
        // check person command-context
        assertEquals(personCmdCaptor.getValue(), nestedStudentCommand);
        nestedStudentCommand = personCmdCaptor.getValue();
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
        ArgumentCaptor<StudentProfileCommand> profileCmdCaptor = ArgumentCaptor.forClass(StudentProfileCommand.class);
        ArgumentCaptor<Input> profileInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(profileCmdCaptor.capture(), profileInputCaptor.capture());
        // check profile command-context
        assertEquals(profileCmdCaptor.getValue(), nestedProfileCommand);
        nestedProfileCommand = profileCmdCaptor.getValue();
        assertThat(profileInputCaptor.getValue()).isEqualTo(newStudentInput);
        verify(command).createProfileContext(nestedProfileCommand, newStudentInput.value());
        verify(nestedProfileCommand).createContext(any(Input.class));

        verify(nestedStudentCommand).acceptPreparedContext(command, newStudentInput);
        ArgumentCaptor<StudentCommand> personCmdCaptor = ArgumentCaptor.forClass(StudentCommand.class);
        ArgumentCaptor<Input> personInputCaptor = ArgumentCaptor.forClass(Input.class);
        verify(command).prepareContext(personCmdCaptor.capture(), personInputCaptor.capture());
        // check person command-context
        assertEquals(personCmdCaptor.getValue(), nestedStudentCommand);
        nestedStudentCommand = personCmdCaptor.getValue();
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
        verify(command).transferResult(any(RootCommand.class), any(), any(Context.class));
        verify(command, times(command.fromNest().size())).executeDoNested(any(Context.class), any(Context.StateChangedListener.class));

        // check nested profile create
        verifyProfileDoCommand();

        // check transfer profile-id to person creation command-context
        ArgumentCaptor<Context<Optional<Student>>> personContextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Optional<StudentProfile>> profileResultCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(command).transferResult(any(RootCommand.class), profileResultCaptor.capture(), personContextCaptor.capture());
        assertThat(profileResultCaptor.getValue().orElseThrow().getId()).isEqualTo(profileId);
        verify(command).transferProfileIdToStudentUpdateInput(profileId, personContextCaptor.getValue());

        // check nested person create
        verifyStudentDoCommand();

        // check database state
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) != null);
        assertStudentEquals(findStudentEntity(studentId), newStudent, false);
        assertThat(findProfileEntity(profileId)).isNotNull();
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

        verifyProfileDoCommand();

        verifyStudentDoCommand();

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
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        // checking profile execution
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();
        assertThat(profileContext.getException()).isInstanceOf(exception.getClass());
        assertThat(profileContext.getException().getMessage()).isEqualTo(exception.getMessage());
        // checking person execution
        Context<Optional<Student>> personContext = (Context<Optional<Student>>) parameter.getNestedContexts().pop();
        assertThat(personContext.getState()).isEqualTo(CANCEL);

        verify(command).executeDoNested(any(Context.class), any(Context.StateChangedListener.class));
        ArgumentCaptor<Context<Optional<StudentProfile>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).doCommand(contextCaptor.capture());
        verify(profileCommand).executeDo(contextCaptor.getValue());
        verify(persistence).save(any(StudentProfile.class));

        verify(command, never()).transferResult(any(RootCommand.class), any(Optional.class), any(Context.class));
        verify(command, never()).transferProfileIdToStudentUpdateInput(anyLong(), any(Context.class));

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
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());
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

        // check nested profile create
        verifyProfileDoCommand();

        // check transfer profile-id to person creation command-context
        ArgumentCaptor<Context<Optional<Student>>> personContextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Optional<StudentProfile>> profileResultCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(command).transferResult(any(RootCommand.class), profileResultCaptor.capture(), personContextCaptor.capture());
        assertThat(profileResultCaptor.getValue().orElseThrow().getId()).isEqualTo(profileId);
        verify(command).transferProfileIdToStudentUpdateInput(profileId, personContextCaptor.getValue());

        // check nested person create
        verifyPersonDoCommand(false);

        // changes' compensation after nested command fail
        verifyProfileUndoCommand(profileId);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }

    @Test
    void shouldExecuteUndoCommand() {
        Student person = makeClearStudent(8);
        Context<Optional<Student>> context = command.createContext(Input.of(person));
        command.doCommand(context);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        assertStudentEquals(findStudentEntity(studentId), person, false);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        // prepare nested contexts to check
        parameter = context.<MacroCommandParameter>getRedoParameter().value();
        nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(studentContext.isUndone()).isTrue();

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(command, times(command.fromNest().size())).executeUndoNested(any(Context.class));
        verifyProfileUndoCommand(profileId);
        verifyStudentUndoCommand(studentId);
        // nested commands order
        checkUndoNestedCommandsOrder(studentId, profileId);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }


    @Test
    void shouldNotExecuteUndoCommand_UndoNestedCommandsThrowsException() {
        Student newStudent = makeClearStudent(11);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        command.doCommand(context);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        assertStudentEquals(findStudentEntity(studentId), newStudent, false);
        assertThat(findProfileEntity(profileId)).isNotNull();
        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(command).rollbackNested(any(Deque.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));

        assertThat(findStudentEntity(studentId)).isNotNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
    }

    @Test
    void shouldNotExecuteUndoCommand_StudentUndoThrowsException() {
        Student newStudent = makeClearStudent(9);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        command.doCommand(context);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        Optional<StudentProfile> profileResult = profileContext.getResult().orElseThrow();
        Optional<Student> studentResult = studentContext.getResult().orElseThrow();
        Long profileId = profileResult.orElseThrow().getId();
        Long studentId = studentResult.orElseThrow().getId();
        assertThat(profileId).isEqualTo(studentResult.orElseThrow().getProfileId());
        assertStudentEquals(findStudentEntity(studentId), newStudent, false);
        assertThat(findProfileEntity(profileId)).isNotNull();
        RuntimeException exception = new RuntimeException("Cannot process student undo command");
        doThrow(exception).when(persistence).deleteStudent(studentId);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());

        parameter = context.<MacroCommandParameter>getRedoParameter().value();
        nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(command).executeUndoNested(any(Context.class));
        verifyStudentUndoCommand(studentId);

        assertThat(findStudentEntity(studentId)).isNotNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
    }

    @Test
    void shouldNotExecuteUndoCommand_ProfileUndoThrowsException() {
        Student newStudent = makeClearStudent(10);
        Context<Optional<Student>> context = command.createContext(Input.of(newStudent));
        command.doCommand(context);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Optional<StudentProfile>> profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        Context<Optional<Student>> studentContext = (Context<Optional<Student>>) nestedContexts.pop();
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
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(exception.getMessage());
        parameter = context.<MacroCommandParameter>getRedoParameter().value();
        nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        profileContext = (Context<Optional<StudentProfile>>) nestedContexts.pop();
        studentContext = (Context<Optional<Student>>) nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(exception.getClass());
        assertThat(profileContext.getException().getMessage()).isEqualTo(exception.getMessage());
        assertThat(studentContext.isUndone()).isFalse();
        assertThat(studentContext.isDone()).isTrue();

        verify(command).executeUndo(context);
        verify(command).rollbackNested(any(Deque.class));
        verify(command, times(command.fromNest().size())).executeUndoNested(any(Context.class));

        verifyProfileUndoCommand(profileId);
        verifyStudentUndoCommand(studentId);
        verifyStudentDoCommand();

        assertThat(findStudentEntity(studentId)).isNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
        Long resultPersonId = studentContext.getResult().orElseThrow().orElseThrow().getId();
        assertThat(findStudentEntity(resultPersonId)).isNotNull();
    }

    // private methods
    private <N extends RootCommand<?>>void assertEquals(N actual, N expected) {
        assertThat(actual.commandFamily()).isSameAs(expected.commandFamily());
        assertThat(actual.getId()).isEqualTo(expected.getId());
    }

    private StudentEntity findStudentEntity(Long id) {
        return findEntity(StudentEntity.class, id, student -> student.getCourseSet().size());
    }

    private StudentProfileEntity findProfileEntity(Long id) {
        return findEntity(StudentProfileEntity.class, id);
    }

    private void checkUndoNestedCommandsOrder(Long studentId, Long profileId) {
        // nested commands order
        InOrder inOrder = Mockito.inOrder(command);

        // creating profile and student (profile is first) avers nested commands order
        ArgumentCaptor<Context<Optional<StudentProfile>>> profileContextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Context<Optional<Student>>> personContextCaptor = ArgumentCaptor.forClass(Context.class);
        inOrder.verify(command).executeDoNested(profileContextCaptor.capture(), any(Context.StateChangedListener.class));
        assertContextOf(profileContextCaptor.getValue(), profileCommand);
        inOrder.verify(command).executeDoNested(personContextCaptor.capture(), any(Context.StateChangedListener.class));
        assertContextOf(personContextCaptor.getValue(), studentCommand);

        // undo creating profile and student (student is first) revers nested commands order
        personContextCaptor = ArgumentCaptor.forClass(Context.class);
        profileContextCaptor = ArgumentCaptor.forClass(Context.class);
        inOrder.verify(command).executeUndoNested(personContextCaptor.capture());
        assertContextOf(personContextCaptor.getValue(), studentCommand);
        inOrder.verify(command).executeUndoNested(profileContextCaptor.capture());
        assertContextOf(profileContextCaptor.getValue(), profileCommand);

        // persistence operations order
        inOrder = Mockito.inOrder(persistence);
        // creating profile and student (profile is first) avers operations order
        inOrder.verify(persistence).save(any(StudentProfile.class));
        inOrder.verify(persistence).save(any(Student.class));
        // undo creating profile and student (student is first) revers operations order
        inOrder.verify(persistence).deleteStudent(studentId);
        inOrder.verify(persistence).deleteProfileById(profileId);
    }

    private void assertContextOf(Context<?> context, RootCommand<?> command) {
        assertThat(context.getCommand().getId()).isEqualTo(command.getId());
    }

    private void verifyProfileUndoCommand(Long id) {
        String contextCommandId = profileCommand.getId();
        ArgumentCaptor<Context<Optional<StudentProfile>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(command, atLeastOnce()).executeUndoNested(contextCaptor.capture());
        boolean isProfile = contextCaptor.getAllValues().stream()
                .anyMatch(context -> contextCommandId.equals(context.getCommand().getId()));
        assertThat(isProfile).isTrue();
        contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).undoCommand(contextCaptor.capture());
        assertContextOf(contextCaptor.getValue(), profileCommand);
        verify(profileCommand).executeUndo(contextCaptor.getValue());
        verify(persistence).deleteProfileById(id);
    }

    private void verifyStudentUndoCommand(Long id) {
        String contextCommandId = studentCommand.getId();
        ArgumentCaptor<Context<Optional<Student>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(command, atLeastOnce()).executeUndoNested(contextCaptor.capture());
        boolean isPerson = contextCaptor.getAllValues().stream()
                .anyMatch(context -> contextCommandId.equals(context.getCommand().getId()));
        assertThat(isPerson).isTrue();
        contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(studentCommand).undoCommand(contextCaptor.capture());
        assertContextOf(contextCaptor.getValue(), studentCommand);
        verify(studentCommand).executeUndo(contextCaptor.getValue());
        verify(persistence).deleteStudent(id);
    }

    private void verifyProfileDoCommand() {
        ArgumentCaptor<Context<Optional<StudentProfile>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        Optional<StudentProfile> profile = nestedContext.getResult().orElseThrow();
        assertThat(profile).isNotEmpty();
        assertThat(nestedContext.getCommand().getId()).isEqualTo(profileCommand.getId());
        verify(profileCommand).doCommand(nestedContext);
        verify(profileCommand).executeDo(nestedContext);
        verify(persistence).save(any(StudentProfile.class));
    }

    private void verifyStudentDoCommand() {
        verifyPersonDoCommand(true);
    }

    private void verifyPersonDoCommand(boolean checkResult) {
        ArgumentCaptor<Context<Optional<Student>>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(studentCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        if (checkResult) {
            assertThat(nestedContext.getResult().orElseThrow()).isNotEmpty();
        }
        assertThat(nestedContext.getCommand().getId()).isEqualTo(studentCommand.getId());
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