package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.StudentProfilePayload;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteAuthorityPersonMacroCommandTest {
    final int maxPoolSize = 10;
    @Mock
    PersistenceFacade persistence;
    @Spy
    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);
    @Spy
    @InjectMocks
    DeletePrincipalProfileCommand profileCommand;
    @Spy
    @InjectMocks
    DeleteAuthorityPersonCommand personCommand;
    DeleteAuthorityPersonMacroCommand command;

    @Mock
    AuthorityPerson person;
    @Mock
    PrincipalProfile profile;

    @BeforeEach
    void setUp() {
        command = spy(new DeleteAuthorityPersonMacroCommand(personCommand, profileCommand, persistence, maxPoolSize));
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
        assertThat(personCommand).isNotNull();
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(command, "maxPoolSize")).isSameAs(maxPoolSize);
        assertThat(ReflectionTestUtils.getField(personCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(personCommand, "payloadMapper")).isSameAs(payloadMapper);
        assertThat(ReflectionTestUtils.getField(profileCommand, "persistence")).isSameAs(persistence);
        assertThat(ReflectionTestUtils.getField(profileCommand, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    void shouldCreateMacroCommandContexts() {
        Long personId = 1L;
        Long profileId = 2L;
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        MacroCommandParameter<Void> redoParameter = context.getRedoParameter();
        assertThat(redoParameter).isNotNull();
        assertThat(redoParameter.getInput()).isSameAs(personId);
        Context<Void> personContext = redoParameter.getNestedContexts().pop();
        Context<Void> profileContext = redoParameter.getNestedContexts().pop();
        assertThat(personContext.isReady()).isTrue();
        assertThat(profileContext.isReady()).isTrue();
        assertThat(personContext.<Long>getRedoParameter()).isSameAs(personId);
        assertThat(profileContext.<Long>getRedoParameter()).isSameAs(profileId);

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(profileId);
    }

    @Test
    void shouldNotCreateMacroCommandContext_StudentNotFound() {
        Long personId = 3L;

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(profileCommand, never()).createContext(any());
        verify(profileCommand).createContextInit();
    }

    @Test
    void shouldNotCreateMacroCommandContext_WrongInputType() {
        Object wrongTypeInput = "something";

        Context<Student> context = command.createContext(wrongTypeInput);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(context.getException().getMessage()).contains(PrincipalProfileCommand.DELETE_BY_ID);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(personCommand, wrongTypeInput);
        verify(personCommand).createContext(wrongTypeInput);

        verify(profileCommand).acceptPreparedContext(command, wrongTypeInput);
        verify(command).prepareContext(profileCommand, wrongTypeInput);
        verify(command, never()).createPrincipalProfileContext(eq(profileCommand), any());
        verify(profileCommand).createContextInit();
        verify(profileCommand, never()).createContext(any());
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentProfileContextThrows() {
        Long personId = 4L;
        Long profileId = 5L;
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        String errorMessage = "Cannot create nested profile context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(profileCommand).createContext(profileId);

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(profileId);
    }

    @Test
    void shouldNotCreateMacroCommandContext_CreateStudentContextThrows() {
        Long personId = 6L;
        Long profileId = 7L;
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        String errorMessage = "Cannot create nested person context";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(personCommand).createContext(personId);

        Context<Void> context = command.createContext(personId);

        assertThat(context).isNotNull();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        assertThat(context.<Object>getRedoParameter()).isNull();

        verify(personCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(personCommand, personId);
        verify(personCommand).createContext(personId);

        verify(profileCommand).acceptPreparedContext(command, personId);
        verify(command).prepareContext(profileCommand, personId);
        verify(command).createPrincipalProfileContext(profileCommand, personId);
        verify(persistence).findAuthorityPersonById(personId);
        verify(profileCommand).createContext(profileId);
    }

    @Test
    void shouldExecuteDoCommand() {
        Long profileId = 8L;
        Long personId = 9L;
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(personId);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();
        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(personContext.<Long>getRedoParameter()).isEqualTo(personId);
        assertThat(profileContext.<Long>getRedoParameter()).isEqualTo(profileId);

        verifyStudentDoCommand(personContext);
        verifyProfileDoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteDoCommand_StudentNotFound() {
        Long personId = 10L;
        Context<Boolean> context = command.createContext(personId);
        assertThat(context.isReady()).isFalse();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("AuthorityPerson with ID:" + personId + " is not exists.");
        assertThat(context.<Object>getRedoParameter()).isNull();
        verify(command, never()).executeDo(any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_ProfileNotFound() {
        Long profileId = 12L;
        Long personId = 11L;
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        Context<Boolean> context = command.createContext(personId);
        assertThat(context.isReady()).isTrue();

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(profileContext.getException().getMessage()).isEqualTo("Profile with ID:" + profileId + " is not exists.");
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        verifyStudentDoCommand(personContext);
        verifyStudentUndoCommand(personContext);
        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteStudentThrows() {
        Long profileId = 14L;
        Long personId = 13L;
        when(person.getId()).thenReturn(personId);
        when(profile.getId()).thenReturn(profileId);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(persistence.save(any(PrincipalProfile.class))).thenReturn(Optional.of(profile));
        Context<Boolean> context = command.createContext(personId);
        String errorMessage = "Cannot delete person";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.deleteAuthorityPerson(personId)).thenThrow(exception);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isInstanceOf(RuntimeException.class);
        assertThat(personContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(personContext.<PrincipalProfile>getUndoParameter()).isNull();
        assertThat(personContext.getResult()).isEmpty();

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        verifyStudentDoCommand(personContext);
        verifyProfileDoCommand(profileContext);
        verifyProfileUndoCommand(profileContext);
        verify(personCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldNotExecuteDoCommand_DeleteProfileThrows() {
        Long profileId = 16L;
        Long personId = 15L;
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(personId);
        String errorMessage = "Cannot delete profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).deleteProfileById(profileId);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        assertThat(context.getException().getMessage()).isEqualTo(errorMessage);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();

        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.getException().getMessage()).isEqualTo(errorMessage);
        assertThat(profileContext.<StudentProfilePayload>getUndoParameter()).isNull();
        assertThat(profileContext.getResult()).isEmpty();

        verify(command).executeDo(context);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verifyStudentDoCommand(personContext);
        verifyProfileDoCommand(profileContext);

        verifyStudentUndoCommand(personContext);
        verify(profileCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
    }

    @Test
    void shouldExecuteUndoCommand() {
        Long profileId = 18L;
        Long personId = 17L;
        Context<Boolean> context = createStudentAndProfileFor(profileId, personId);
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        when(persistence.save(any(PrincipalProfile.class))).thenReturn(Optional.of(profile));

        command.undoCommand(context);

        assertThat(context.isUndone()).isTrue();
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();
        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isUndone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isUndone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));
        verifyStudentUndoCommand(personContext);
        verifyProfileUndoCommand(profileContext);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileThrows() {
        Long profileId = 20L;
        Long personId = 19L;
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        Context<Boolean> context = createStudentAndProfileFor(profileId, personId);
        when(persistence.save(any(AuthorityPerson.class))).thenReturn(Optional.of(person));
        String errorMessage = "Cannot restore profile";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(persistence.save(any(PrincipalProfile.class))).thenThrow(exception);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();
        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isDone()).isTrue();
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isFailed()).isTrue();
        assertThat(profileContext.getException()).isSameAs(exception);
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));
        verifyStudentUndoCommand(personContext);
        verifyProfileUndoCommand(profileContext);
        verifyStudentDoCommand(personContext, 2);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveStudentThrows() {
        Long profileId = 22L;
        Long personId = 21L;
        when(profile.getId()).thenReturn(profileId);
        Context<Boolean> context = createStudentAndProfileFor(profileId, personId);
        when(persistence.save(any(PrincipalProfile.class))).thenReturn(Optional.of(profile));
        String errorMessage = "Cannot restore person";
        RuntimeException exception = new RuntimeException(errorMessage);
        doThrow(exception).when(persistence).save(any(AuthorityPerson.class));

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isSameAs(exception);
        MacroCommandParameter<Boolean> parameter = context.getRedoParameter();
        Context<Boolean> personContext = parameter.getNestedContexts().pop();
        assertThat(personContext.isFailed()).isTrue();
        assertThat(personContext.getException()).isSameAs(exception);
        assertThat(personContext.<AuthorityPersonPayload>getUndoParameter().getOriginal()).isSameAs(person);
        assertThat(personContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        Context<Boolean> profileContext = parameter.getNestedContexts().pop();
        assertThat(profileContext.isDone()).isTrue();
        assertThat(profileContext.<PrincipalProfilePayload>getUndoParameter().getOriginal()).isSameAs(profile);
        assertThat(profileContext.getResult().orElseThrow()).isSameAs(Boolean.TRUE);

        verify(command).executeUndo(context);
        verify(command).undoNestedCommands(any(Deque.class));
        verifyStudentUndoCommand(personContext);
        verifyProfileUndoCommand(profileContext);
        verifyProfileDoCommand(profileContext, 2);
    }


    // private methods
    private @NotNull Context<Boolean> createStudentAndProfileFor(Long profileId, Long personId) {
        when(person.getId()).thenReturn(personId);
        when(person.getProfileId()).thenReturn(profileId);
        when(profile.getId()).thenReturn(profileId);
        when(persistence.findAuthorityPersonById(personId)).thenReturn(Optional.of(person));
        when(persistence.findPrincipalProfileById(profileId)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(personId);
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
        verify(persistence, times(i)).findPrincipalProfileById(id);
        verify(persistence, times(i)).deleteProfileById(id);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext) {
        verifyStudentDoCommand(nestedContext, 1);
    }

    private void verifyStudentDoCommand(Context<Boolean> nestedContext, int i) {
        verify(personCommand, times(i)).doAsNestedCommand(eq(command), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(command, times(i)).doNestedCommand(eq(personCommand), eq(nestedContext), any(Context.StateChangedListener.class));
        verify(personCommand, times(i)).doCommand(nestedContext);
        verify(personCommand, times(i)).executeDo(nestedContext);
        Long id = nestedContext.getRedoParameter();
        verify(persistence, times(i + 1)).findAuthorityPersonById(id);
        verify(persistence, times(i)).deleteAuthorityPerson(id);
    }

    private void verifyStudentUndoCommand(Context<Boolean> nestedContext) {
        verify(personCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(personCommand, nestedContext);
        verify(personCommand).undoCommand(nestedContext);
        verify(personCommand).executeUndo(nestedContext);
        verify(persistence).save(any(AuthorityPerson.class));
    }

    private void verifyProfileUndoCommand(Context<Boolean> nestedContext) {
        verify(profileCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(profileCommand, nestedContext);
        verify(profileCommand).undoCommand(nestedContext);
        verify(profileCommand).executeUndo(nestedContext);
        verify(persistence).save(any(PrincipalProfile.class));
    }
}