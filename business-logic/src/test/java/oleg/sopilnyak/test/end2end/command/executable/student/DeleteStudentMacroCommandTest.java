package oleg.sopilnyak.test.end2end.command.executable.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
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
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import jakarta.persistence.EntityManager;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        PersistenceConfiguration.class, SchoolCommandsConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {
        "school.parallel.max.pool.size=10",
        "school.spring.jpa.show-sql=true",
        "school.hibernate.hbm2ddl.auto=update"
})
@SuppressWarnings("unchecked")
class DeleteStudentMacroCommandTest extends MysqlTestModelFactory {
    private static final String PROFILE_DELETE_BY_ID = "school::person::profile::student:delete.By.Id";
    @Autowired
    ApplicationContext applicationContext;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    @Qualifier("profileStudentDelete")
    StudentProfileCommand profileCommand;
    @MockitoSpyBean
    @Autowired
    @Qualifier("studentDelete")
    StudentCommand personCommand;
    @MockitoSpyBean
    @Autowired
    CommandActionExecutor actionExecutor;
    @MockitoSpyBean
    @Autowired
    @Qualifier("parallelCommandNestedCommandsExecutor")
    Executor schedulingTaskExecutor;
    // delete student macro command
    DeleteStudentMacroCommand command;

    @Captor
    ArgumentCaptor<StudentCommand> personCaptor;
    @Captor
    ArgumentCaptor<StudentProfileCommand> profileCaptor;

    @BeforeEach
    void setUp() {
        command = spy(new DeleteStudentMacroCommand(
                personCommand, profileCommand, schedulingTaskExecutor, persistence, actionExecutor
        ));
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        ActionContext.setup("test-facade", "test-action");
    }

    @AfterEach
    void tearDown() {
        reset(command, profileCommand, personCommand, persistence, payloadMapper);
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentProfileEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
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
        assertThat(findStudentEntity(personId)).isNotNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
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
        assertThat(findStudentEntity(student.getId())).isNotNull();
        Long personId = student.getId() + 3L;

        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + personId + " is not exists.");
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

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
        assertThat(context.getException().getMessage()).contains(PROFILE_DELETE_BY_ID);
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
        assertThat(findStudentEntity(personId)).isNotNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(Input.of(profileId));

        Input<Long> inputId = Input.of(personId);
        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

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
        assertThat(findStudentEntity(personId)).isNotNull();
        assertThat(findProfileEntity(profileId)).isNotNull();
        String errorMessage = "Cannot create nested student context";
        Input<Long> inputId = Input.of(personId);
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(personCommand).createContext(inputId);

        Context<Boolean> context = command.createContext(inputId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter().isEmpty()).isTrue();

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
        Student person = findStudentEntity(personId);
        StudentProfile profile = findProfileEntity(profileId);
        Context<Boolean> context = command.createContext(Input.of(personId));

        command.doCommand(context);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(personId) == null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileId) == null);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertThat(studentContext.getResult().orElseThrow()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginalType()).isEqualTo(person.getClass().getName());
        assertStudentEquals(studentContext.<StudentPayload>getUndoParameter().value(), person, false);
        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(profileContext.<StudentProfilePayload>getUndoParameter().value(), profile, false);

        verify(command).self();
        assertThat(studentContext.<Long>getRedoParameter().value()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isEqualTo(profileId);

        verifyStudentDoCommand();
        verifyProfileDoCommand();
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
        Context<Boolean> context = command.createContext(Input.of(studentId));
        assertThat(context.isReady()).isTrue();
        assertThat(deleteProfileEntity(profileId)).isNull();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginalType()).isEqualTo(student.getClass().getName());
        assertStudentEquals(studentContext.<StudentPayload>getUndoParameter().value(), student, false);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verifyStudentDoCommand();
        verify(persistence, times(2)).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyStudentUndoCommand();
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
        StudentProfile profile = findProfileEntity(profileId);
        assertThat(findStudentEntity(studentId)).isNotNull();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        String errorMessage = "Cannot delete student";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteStudent(studentId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Boolean> studentContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(studentContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(studentContext.<StudentPayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(studentContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isUndone()).isTrue();
        StudentProfilePayload savedProfile = profileContext.<StudentProfilePayload>getUndoParameter().value();
        assertThat(savedProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(savedProfile, profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verifyPersonDoCommand(false);
        verify(persistence, times(2)).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyProfileDoCommand();
        verify(persistence).findStudentProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyProfileUndoCommand();
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
        Student student = findStudentEntity(studentId);
        Context<Boolean> context = command.createContext(Input.of(studentId));
        String errorMessage = "Don't want to delete profile. Bad guy.";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Boolean> studentContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(studentContext.isUndone()).isTrue();
        StudentPayload saveStudent = studentContext.<StudentPayload>getUndoParameter().value();
        assertThat(saveStudent.getOriginalType()).isEqualTo(student.getClass().getName());
        assertStudentEquals(saveStudent, student, false);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().isEmpty()).isTrue();
        assertThat(profileContext.getResult()).isEmpty();

        verifyStudentDoCommand();
        verify(persistence, times(2)).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyProfileDoCommand(false);
        verify(persistence).findStudentProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyStudentUndoCommand();
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
        Student student = findStudentEntity(studentId);
        StudentProfile profile = findProfileEntity(profileId);
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        reset(command);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Boolean> studentContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(studentContext.isUndone()).isTrue();
        StudentPayload savedStudent = studentContext.<StudentPayload>getUndoParameter().value();
        assertThat(savedStudent.getOriginalType()).isEqualTo(student.getClass().getName());
        assertStudentEquals(savedStudent, student, false);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isUndone()).isTrue();
        StudentProfilePayload savedProfile = profileContext.<StudentProfilePayload>getUndoParameter().value();
        assertThat(savedProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(savedProfile, profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verifyStudentUndoCommand();
        verify(persistence).save(studentContext.<Student>getUndoParameter().value());

        verifyProfileUndoCommand();
        verify(persistence).save(profileContext.<StudentProfile>getUndoParameter().value());

        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findStudentEntity(studentContext.<Long>getRedoParameter().value()) != null);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findProfileEntity(profileContext.<Long>getRedoParameter().value()) != null);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(17));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = findStudentEntity(studentId);
        StudentProfile profile = findProfileEntity(profileId);
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        String errorMessage = "Cannot restore profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(StudentProfile.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Boolean> studentContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(studentContext.isDone()).isTrue();
        StudentPayload savedStudent = studentContext.<StudentPayload>getUndoParameter().value();
        assertThat(savedStudent.getOriginalType()).isEqualTo(student.getClass().getName());
        assertStudentEquals(savedStudent, student, false);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(exception.getClass());
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        StudentProfilePayload savedProfile = profileContext.<StudentProfilePayload>getUndoParameter().value();
        assertThat(savedProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(savedProfile, profile, false);
        assertThat(profileContext.getResult()).isEmpty();

        verifyStudentUndoCommand();
        verify(persistence).save(any(StudentPayload.class));

        verifyProfileUndoCommand();
        verify(persistence).save(profileContext.<StudentProfile>getUndoParameter().value());

        verifyStudentDoCommand(2);
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
        Student student = findStudentEntity(studentId);
        StudentProfile profile = findProfileEntity(profileId);
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        String errorMessage = "Cannot restore student";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(Student.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(exception.getClass());
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Deque<Context<?>> nestedContexts = new LinkedList<>(parameter.getNestedContexts());
        Context<Boolean> studentContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(studentContext.isFailed()).isTrue();
        assertThat(studentContext.getException()).isInstanceOf(exception.getClass());
        assertThat(studentContext.getException().getMessage()).isEqualTo(errorMessage);
        StudentPayload savedStudent = studentContext.<StudentPayload>getUndoParameter().value();
        assertThat(savedStudent.getOriginalType()).isEqualTo(student.getClass().getName());
        assertStudentEquals(savedStudent, student, false);
        assertThat(studentContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) nestedContexts.pop();
        assertThat(profileContext.isDone()).isTrue();
        StudentProfilePayload savedProfile = profileContext.<StudentProfilePayload>getUndoParameter().value();
        assertThat(savedProfile.getOriginalType()).isEqualTo(profile.getClass().getName());
        assertProfilesEquals(savedProfile, profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verifyStudentUndoCommand();
        verify(persistence).save(any(StudentPayload.class));

        verifyProfileUndoCommand();
        verify(persistence).save(any(StudentProfilePayload.class));

        verifyProfileDoCommand( 2);
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
        try {
            StudentProfile profile = persist(makeStudentProfile(null));
            if (newStudent instanceof FakeStudent fake) {
                fake.setProfileId(profile.getId());
            } else {
                fail("Not a fake person type");
            }
            return payloadMapper.toPayload(persist(newStudent));
        } finally {
            reset(payloadMapper);
        }
    }

    private Student persist(Student newInstance) {
        Student entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private StudentProfile persist(StudentProfile newInstance) {
        StudentProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private void verifyProfileDoCommand(int times) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand, times(times)).doCommand(contextCaptor.capture());
        contextCaptor.getAllValues().forEach(context -> verify(profileCommand).executeDo(context));
    }

    private void verifyProfileDoCommand() {
        verifyProfileDoCommand(true);
    }

    private void verifyProfileDoCommand(boolean checkResult) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(profileCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        if (checkResult) {
            assertThat(nestedContext.getResult().orElseThrow()).isTrue();
        }
        assertThat(nestedContext.getCommand().getId()).isEqualTo(profileCommand.getId());
        verify(profileCommand).executeDo(nestedContext);
    }

    private void verifyProfileUndoCommand() {
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
    }


    private void verifyStudentDoCommand(int times) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand, times(times)).doCommand(contextCaptor.capture());
        contextCaptor.getAllValues().forEach(context -> verify(personCommand).executeDo(context));
    }

    private void verifyStudentDoCommand() {
        verifyPersonDoCommand(true);
    }

    private void verifyPersonDoCommand(boolean checkResult) {
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand).doCommand(contextCaptor.capture());
        var nestedContext = contextCaptor.getValue();
        if (checkResult) {
            assertThat(nestedContext.getResult().orElseThrow()).isTrue();
        }
        assertThat(nestedContext.getCommand().getId()).isEqualTo(personCommand.getId());
        verify(personCommand).executeDo(nestedContext);
    }

    private void verifyStudentUndoCommand() {
        String contextCommandId = personCommand.getId();
        ArgumentCaptor<Context<Boolean>> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(command, atLeastOnce()).executeUndoNested(contextCaptor.capture());
        boolean isPerson = contextCaptor.getAllValues().stream()
                .anyMatch(context -> contextCommandId.equals(context.getCommand().getId()));
        assertThat(isPerson).isTrue();
        contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(personCommand).undoCommand(contextCaptor.capture());
        assertContextOf(contextCaptor.getValue(), personCommand);
        verify(personCommand).executeUndo(contextCaptor.getValue());
    }

    private void assertContextOf(Context<?> context, RootCommand<?> command) {
        assertThat(context.getCommand().getId()).isEqualTo(command.getId());
    }

}