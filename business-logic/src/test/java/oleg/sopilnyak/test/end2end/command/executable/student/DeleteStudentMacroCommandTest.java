package oleg.sopilnyak.test.end2end.command.executable.student;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.student.DeleteStudentCommand;
import oleg.sopilnyak.test.service.command.executable.student.DeleteStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.facade.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class,
        StudentsFacadeImpl.class,
        DeleteStudentProfileCommand.class,
        DeleteStudentCommand.class,
        DeleteStudentMacroCommand.class,
        SchoolCommandsConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.parallel.max.pool.size=10",
        "school.spring.jpa.show-sql=true",
        "school.hibernate.hbm2ddl.auto=update"
})
@Rollback
class DeleteStudentMacroCommandTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    StudentsFacadeImpl facade;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    DeleteStudentProfileCommand profileCommand;
    @SpyBean
    @Autowired
    DeleteStudentCommand studentCommand;
    @SpyBean
    @Autowired
    DeleteStudentMacroCommand command;

    final int maxPoolSize = 10;

    @BeforeEach
    void setUp() {
        Assertions.setMaxStackTraceElementsDisplayed(1000);
        command.runThreadPoolExecutor();
    }

    @AfterEach
    void tearDown() {
        command.stopThreadPoolExecutor();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeValidCommand() {
        assertThat(facade).isNotNull();
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateMacroCommandContexts() {
        StudentPayload student = createStudent(makeClearStudent(1));
        Long studentId = student.getId();
        Long profileId = student.getProfileId();
        assertThat(persistence.findStudentById(studentId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        reset(persistence);

        Input<Long> inputStudentId = Input.of(studentId);
        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter redoParameter = context.<MacroCommandParameter>getRedoParameter().value();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getInputParameter().value()).isSameAs(studentId);
        Context<?> studentContext = redoParameter.getNestedContexts().pop();
        Context<?> profileContext = redoParameter.getNestedContexts().pop();
        assertThat(studentContext.isReady()).isTrue();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(studentContext.<Long>getRedoParameter().value()).isSameAs(studentId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isSameAs(profileId);

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_StudentNotFound() {
        StudentPayload student = createStudent(makeClearStudent(3));
        assertThat(persistence.findStudentById(student.getId())).isPresent();
        Long studentId = 3L;

        Input<Long> inputStudentId = Input.of(studentId);
        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + studentId + " is not exists.");
        assertThat(context.getRedoParameter()).isNull();

        verify(studentCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(studentCommand, inputStudentId);
        verify(studentCommand).createContext(inputStudentId);

        verify(profileCommand).acceptPreparedContext(command, inputStudentId);
        verify(command).prepareContext(profileCommand, inputStudentId);
        verify(command).createStudentProfileContext(profileCommand, studentId);
        verify(persistence).findStudentById(studentId);
        verify(profileCommand, never()).createContext(any());
        verify(profileCommand).createContextInit();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";

        Input<?> wrongInput = Input.of(wrongTypeInput);
        Context<Boolean> context = command.createContext(wrongInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(StudentProfileCommand.DELETE_BY_ID);
        assertThat(context.getRedoParameter()).isNull();

        verify(studentCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(studentCommand, wrongInput);
        verify(studentCommand).createContext(wrongInput);

        verify(profileCommand).acceptPreparedContext(command, wrongInput);
        verify(command).prepareContext(profileCommand, wrongInput);
        verify(command, never()).createStudentProfileContext(eq(profileCommand), any());
        verify(profileCommand).createContextInit();
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_CreateStudentProfileContextThrows() {
        StudentPayload student = createStudent(makeClearStudent(5));
        Long studentId = student.getId();
        Long profileId = student.getProfileId();
        assertThat(persistence.findStudentById(studentId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        reset(persistence);
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(Input.of(profileId));

        Input<Long> inputStudentId = Input.of(studentId);
        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter()).isNull();

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateMacroCommandContext_CreateStudentContextThrows() {
        StudentPayload student = createStudent(makeClearStudent(7));
        Long studentId = student.getId();
        Long profileId = student.getProfileId();
        assertThat(persistence.findStudentById(studentId)).isPresent();
        assertThat(persistence.findStudentProfileById(profileId)).isPresent();
        reset(persistence);
        String errorMessage = "Cannot create nested student context";
        RuntimeException exception = new RuntimeException(errorMessage);
        Input<Long> inputStudentId = Input.of(studentId);
        doThrow(exception).when(studentCommand).createContext(inputStudentId);

        Context<Boolean> context = command.createContext(inputStudentId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getRedoParameter()).isNull();

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteDoCommand() {
        StudentPayload studentPayload = createStudent(makeClearStudent(9));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();
        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isDone()).isTrue();
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(studentContext.<Long>getRedoParameter().value()).isEqualTo(studentId);
        assertThat(profileContext.<Long>getRedoParameter().value()).isEqualTo(profileId);

        verifyStudentDoCommand(studentContext);
        verifyProfileDoCommand(profileContext);
        assertThat(persistence.findStudentById(studentId)).isEmpty();
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_StudentNotFound() {
        Long studentId = 10L;
        Context<Boolean> context = command.createContext(Input.of(studentId));
        assertThat(context.isReady()).isFalse();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Student with ID:" + studentId + " is not exists.");
        assertThat(context.getRedoParameter()).isNull();
        verify(command, never()).executeDo(any(Context.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_ProfileNotFound() {
        StudentPayload studentPayload = createStudent(makeClearStudent(11));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        persistence.deleteProfileById(profileId);
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        assertThat(context.isReady()).isTrue();
        reset(persistence);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyStudentDoCommand(studentContext);
        verify(persistence).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(any(StudentPayload.class));
        Long savedStudentId = studentContext.<Long>getRedoParameter().value();
        assertThat(persistence.findStudentById(savedStudentId)).isPresent();

        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));

        assertThat(persistence.isNoStudents()).isFalse();
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_DeleteStudentThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(13));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        assertThat(persistence.findStudentById(studentId)).isPresent();
        reset(persistence);
        Context<Boolean> context = command.createContext(Input.of(studentId));
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
        assertThat(studentContext.<StudentPayload>getUndoParameter()).isNull();
        assertThat(studentContext.getResult()).isEmpty();

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        StudentProfilePayload savedProfile = profileContext.<StudentProfilePayload>getUndoParameter().value();
        assertThat(savedProfile.getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyStudentDoCommand(studentContext);
        verify(persistence, times(2)).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findStudentProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(savedProfile);

        verify(studentCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        assertThat(persistence.findStudentById(studentContext.<Long>getRedoParameter().value())).isPresent();
        assertThat(persistence.findStudentProfileById(profileContext.<Long>getRedoParameter().value())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(15));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        reset(persistence);
        Context<Boolean> context = command.createContext(Input.of(studentId));
        String errorMessage = "Cannot delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyStudentDoCommand(studentContext);
        verify(persistence, times(2)).findStudentById(studentId);
        verify(persistence).deleteStudent(studentId);

        verifyProfileDoCommand(profileContext);
        verify(persistence).findStudentProfileById(profileId);
        verify(persistence).deleteProfileById(profileId);

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(studentContext.<Student>getUndoParameter().value());

        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        assertThat(persistence.findStudentById(studentContext.<Long>getRedoParameter().value())).isPresent();
        assertThat(persistence.findStudentProfileById(profileContext.<Long>getRedoParameter().value())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldExecuteUndoCommand() {
        StudentPayload studentPayload = createStudent(makeClearStudent(17));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        reset(persistence);

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();

        MacroCommandParameter parameter = context.<MacroCommandParameter>getRedoParameter().value();

        Context<Boolean> studentContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(studentContext.isUndone()).isTrue();
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal()).isEqualTo(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Input.class));

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(studentContext.<Student>getUndoParameter().value());

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<StudentProfile>getUndoParameter().value());

        assertThat(persistence.findStudentById(studentContext.<Long>getRedoParameter().value())).isPresent();
        assertThat(persistence.findStudentProfileById(profileContext.<Long>getRedoParameter().value())).isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(17));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        reset(persistence);
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
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Input.class));

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(any(StudentPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(profileContext.<StudentProfile>getUndoParameter().value());

        verifyStudentDoCommand(studentContext, 2);
        Long newStudentId = studentContext.<Long>getRedoParameter().value();
        verify(persistence).findStudentById(newStudentId);
        verify(persistence).deleteStudent(newStudentId);

        assertThat(persistence.findStudentById(newStudentId)).isEmpty();
        assertThat(persistence.findStudentById(studentId)).isEmpty();
        assertThat(persistence.findStudentProfileById(profileContext.<Long>getRedoParameter().value())).isEmpty();
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotExecuteUndoCommand_SaveStudentThrows() {
        StudentPayload studentPayload = createStudent(makeClearStudent(21));
        Long studentId = studentPayload.getId();
        Long profileId = studentPayload.getProfileId();
        Student student = persistence.findStudentById(studentId).orElseThrow();
        StudentProfile profile = persistence.findStudentProfileById(profileId).orElseThrow();
        Context<Boolean> context = command.createContext(Input.of(studentId));
        command.doCommand(context);
        reset(persistence);
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
        assertThat(studentContext.<StudentPayload>getUndoParameter().value().getOriginal()).isSameAs(student);
        assertThat(studentContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = (Context<Boolean>) parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertProfilesEquals(profileContext.<StudentProfilePayload>getUndoParameter().value().getOriginal(), profile, false);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Input.class));

        verifyStudentUndoCommand(studentContext);
        verify(persistence).save(any(StudentPayload.class));

        verifyProfileUndoCommand(profileContext);
        verify(persistence).save(any(StudentProfilePayload.class));

        verifyProfileDoCommand(profileContext, 2);
        Long newProfileId = profileContext.<Long>getRedoParameter().value();
        verify(persistence).findStudentProfileById(newProfileId);
        verify(persistence).deleteProfileById(newProfileId);

        assertThat(persistence.findStudentById(studentContext.<Long>getRedoParameter().value())).isEmpty();
        assertThat(persistence.findStudentById(studentId)).isEmpty();
        assertThat(persistence.findStudentProfileById(newProfileId)).isEmpty();
        assertThat(persistence.findStudentProfileById(profileId)).isEmpty();
    }


    // private methods
    private StudentPayload createStudent(Student newStudent) {
        return (StudentPayload) facade.create(newStudent).orElseThrow();
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext) {
        verifyProfileDoCommand(nestedContext, 1);
    }

    private void verifyProfileDoCommand(Context<Boolean> nestedContext, int i) {
        verify(profileCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command, times(i)).doNestedCommand(eq(profileCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(profileCommand, times(i)).doCommand(nestedContext);
        verify(profileCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext) {
        verifyStudentDoCommand(nestedContext, 1);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext, int i) {
        verify(studentCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command, times(i)).doNestedCommand(eq(studentCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(studentCommand, times(i)).doCommand(nestedContext);
        verify(studentCommand, times(i)).executeDo(nestedContext);
    }

    private void verifyStudentUndoCommand(Context<Boolean> nestedContext) {
        verify(studentCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(studentCommand, nestedContext);
        verify(studentCommand).undoCommand(nestedContext);
        verify(studentCommand).executeUndo(nestedContext);
    }

    private void verifyProfileUndoCommand(Context<Boolean> nestedContext) {
        verify(profileCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(profileCommand, nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
    }
}