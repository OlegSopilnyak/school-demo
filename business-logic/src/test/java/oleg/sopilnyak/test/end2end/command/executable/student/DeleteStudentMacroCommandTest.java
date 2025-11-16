package oleg.sopilnyak.test.end2end.command.executable.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.facade.education.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {StudentsFacadeImpl.class,
        PersistenceConfiguration.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {
        "school.parallel.max.pool.size=10",
        "school.spring.jpa.show-sql=true",
        "school.hibernate.hbm2ddl.auto=update"
})
class DeleteStudentMacroCommandTest extends MysqlTestModelFactory {
    @Autowired
    ApplicationContext applicationContext;
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    StudentsFacade facade;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    @Qualifier("profileStudentDelete")
    StudentProfileCommand profileCommand;
    @SpyBean
    @Autowired
    @Qualifier("studentDelete")
    StudentCommand personCommand;
    @SpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @SpyBean
    @Autowired
    SchedulingTaskExecutor schedulingTaskExecutor;
    @Autowired
    CommandThroughMessageService messagesExchangeService;

    DeleteStudentMacroCommand command;

    @Captor
    ArgumentCaptor<StudentCommand> personCaptor;
    @Captor
    ArgumentCaptor<StudentProfileCommand> profileCaptor;

    @BeforeEach
    void setUp() {
        Assertions.setMaxStackTraceElementsDisplayed(1000);
        command = spy(new DeleteStudentMacroCommand(
                personCommand, profileCommand, schedulingTaskExecutor, persistence, actionExecutor
        ));
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        ActionContext.setup("test-facade", "test-action");
        messagesExchangeService.initialize();
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentProfileEntity.class);
    }

    @AfterEach
    void tearDown() {
        reset(command, profileCommand, personCommand, persistence, payloadMapper);
        messagesExchangeService.shutdown();
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentProfileEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(facade).isNotNull();
        assertThat(profileCommand).isNotNull();
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        StudentPayload student = createStudent(makeClearStudent(1));
        Long personId = student.getId();
        Long profileId = student.getProfileId();
        assertThat(persistence.findStudentById(personId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        reset(persistence);

        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter redoParameter = context.<MacroCommandParameter>getRedoParameter().value();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getRootInput().value()).isSameAs(personId);
        Context<?> studentContext = redoParameter.getNestedContexts().pop();
        Context<?> profileContext = redoParameter.getNestedContexts().pop();
        assertThat(studentContext.isReady()).isTrue();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(studentContext.<Long>getRedoParameter().value()).isSameAs(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isSameAs(profileId);

        verify(command).self();
        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findStudentById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_StudentNotFound() {
        StudentPayload student = createStudent(makeClearStudent(3));
        assertThat(persistence.findStudentById(student.getId())).isPresent();
        Long personId = 3L;

        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + personId + " is not exists.");
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(command).self();
        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findStudentById(personId);
        verify(profileCommand, never()).createContext(any());
        verify(profileCommand).createFailedContext(any(StudentNotFoundException.class));
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Boolean> context = command.createContext(wrongInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.DELETE_BY_ID);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(personCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(personCaptor.capture(), eq(wrongInput));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(wrongInput);

        verify(profileCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(profileCaptor.capture(), eq(wrongInput));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(command, never()).createStudentProfileContext(any(), any());
        verify(profileCommand).createFailedContext(any(CannotCreateCommandContextException.class));
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentProfileContextThrows() {
        StudentPayload student = createStudent(makeClearStudent(5));
        Long personId = student.getId();
        Long profileId = student.getProfileId();
        assertThat(persistence.findStudentById(personId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        reset(persistence);
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(Input.of(profileId));

        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(command).self();
        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findStudentById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentContextThrows() {
        StudentPayload student = createStudent(makeClearStudent(7));
        Long personId = student.getId();
        Long profileId = student.getProfileId();
        assertThat(persistence.findStudentById(personId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        reset(persistence);
        String errorMessage = "Cannot create nested student context";
        RuntimeException exception = new RuntimeException(errorMessage);
        Input<Long> inputId = Input.of(personId);
        doThrow(exception).when(personCommand).createContext(inputId);

        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

        verify(command).self();
        verify(personCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(personCaptor.capture(), eq(inputId));
        assertThat(personCommand.getId()).isEqualTo(personCaptor.getValue().getId());
        verify(personCommand).createContext(inputId);

        verify(profileCommand).acceptPreparedContext(command, inputId);
        verify(command).prepareContext(profileCaptor.capture(), eq(inputId));
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        assertThat(profileCommand.getId()).isEqualTo(profileCaptor.getValue().getId());
        verify(persistence).findStudentById(personId);
        verify(profileCommand).createContext(Input.of(profileId));
    }

    @Test
    void shouldExecuteDoCommand() {
        StudentPayload studentPayload = createStudent(makeClearStudent(9));
        Long personId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student person = persistence.findStudentById(personId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(personId));
        reset(persistence, command);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertThat(studentContext.getResult().orElseThrow()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isEqualTo(person);
        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);

        verify(command).self();
        assertThat(studentContext.<Long>getRedoParameter().value()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isEqualTo(profileId);

        verifyStudentDoCommand(studentContext);
        verifyProfileDoCommand(profileContext);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }

    @Test
    void shouldNotExecuteDoCommand_StudentNotFound() {
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
        StudentPayload studentPayload = createStudent(makeClearStudent(11));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        Student student = findStudentEntity(studentId);
        assertThat(deleteProfileEntity(profileId)).isNull();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        assertThat(context.isReady()).isTrue();
        reset(persistence, command);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isEqualTo(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).self();

        verifyStudentDoCommand(studentContext);
        verify(persistence).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(any(StudentPayload.class));

        verify(profileCommand, never()).undoCommand(any(Context.class));
        Long undoStudentId = studentContext.<Long>getRedoParameter().value();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(undoStudentId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteStudentThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(13));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        assertThat(persistence.findStudentById(studentId)).isPresent();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        reset(persistence, command);
        String errorMessage = "Cannot delete student";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteStudent(studentId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(studentContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(studentContext.<StudentPayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(studentContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        StudentProfilePayload savedProfile = profileContext.<StudentProfilePayload>getUndoParameter().value();
        assertThat(savedProfile.getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).self();

        verifyStudentDoCommand(studentContext);
        verify(persistence).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findStudentProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(savedProfile);

        verify(personCommand, never()).undoCommand(any(Context.class));
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentContext.<Long>getRedoParameter().value()) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileContext.<Long>getRedoParameter().value()) != null);
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(15));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        reset(persistence, command);
        String errorMessage = "Don't want to delete profile. Bad guy.";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isEqualTo(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).self();

        verifyStudentDoCommand(studentContext);
        verify(persistence).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findStudentProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(studentContext.<Student>getUndoParameter().value());

        verify(profileCommand, never()).undoCommand(any(Context.class));
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentContext.<Long>getRedoParameter().value()) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileContext.<Long>getRedoParameter().value()) != null);
    }

    @Test
    void shouldExecuteUndoCommand() {
        StudentPayload studentPayload = createStudent(makeClearStudent(17));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) != null);
        Student student = persistence.findStudentById(studentId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        reset(persistence, command);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isEqualTo(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).self();

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(studentContext.<Student>getUndoParameter().value());

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<StudentProfile>getUndoParameter().value());

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentContext.<Long>getRedoParameter().value()) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileContext.<Long>getRedoParameter().value()) != null);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(17));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        reset(persistence, command);
        String errorMessage = "Cannot restore profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(StudentProfile.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertStudentEquals(studentContext.<StudentPayload>getUndoParameter().value().getOriginal(), student, false);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).self();

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(any(StudentPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<StudentProfile>getUndoParameter().value());

        verifyStudentDoCommand(studentContext, 2);
        Long newStudentId = studentContext.<Long>getRedoParameter().value();
        verify(persistence).findStudentById(newStudentId);
        verify(persistence).deleteStudent(newStudentId);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentContext.<Long>getRedoParameter().value()) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileContext.<Long>getRedoParameter().value()) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveStudentThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(21));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        reset(persistence, command);
        String errorMessage = "Cannot restore student";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(Student.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isSameAs(exception);
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isEqualTo(student);
        assertThat(studentContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertProfilesEquals(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal(), profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).self();

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(any(StudentPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(any(StudentProfilePayload.class));

        verifyProfileDoCommand(profileContext, 2);
        Long newProfileId = profileContext.<Long>getRedoParameter().value();
        verify(persistence).findStudentProfileById(newProfileId);
        verify(persistence).deleteProfileById(newProfileId);

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentContext.<Long>getRedoParameter().value()) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileContext.<Long>getRedoParameter().value()) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);
    }


    // private methods
    private StudentEntity findStudentEntity(Long id) {
        return findEntity(StudentEntity.class, id, student -> student.getCourseSet().size());
    }

    private StudentProfileEntity findProfileEntity(Long id) {
        return findEntity(StudentProfileEntity.class, id);
    }

    private StudentProfileEntity deleteProfileEntity(Long id) {
        return deleteEntity(StudentProfileEntity.class, id);
    }

    private StudentPayload createStudent(Student newStudent) {
        return (StudentPayload) facade.create(newStudent).orElseThrow();
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext) {
        verifyProfileDoCommand(nestedContext, 1);
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext, int i) {
        verify(profileCommand, times(i)).doCommand(nestedContext);
        verify(profileCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext) {
        verifyStudentDoCommand(nestedContext, 1);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext, int i) {
        verify(personCommand, times(i)).doCommand(nestedContext);
        verify(personCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyStudentUndoCommand(Context<Boolean> nestedContext) {
        verify(personCommand).undoCommand(nestedContext);
        verify(personCommand).executeUndo(nestedContext);
    }

    private void verifyProfileUndoCommand(Context<Boolean> nestedContext) {
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
    }
}