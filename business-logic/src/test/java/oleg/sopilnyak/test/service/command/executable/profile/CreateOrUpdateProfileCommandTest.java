package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    CreateOrUpdateProfileCommand command;
    @Mock
    PersonProfile input;
    @Mock
    StudentProfile profile;

    @Test
    void shouldExecuteCommand() {

        CommandResult<Optional<? extends PersonProfile>> result = command.execute(input);

        verify(persistenceFacade).saveProfile(input);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistenceFacade).saveProfile(input);


        CommandResult<Optional<? extends PersonProfile>> result = command.execute(input);

        verify(persistenceFacade).saveProfile(input);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldExecuteDoCommandCommand_ExistsProfileId() {
        Long id = 700L;
        when(profile.getId()).thenReturn(id);
        Context<Optional<? extends PersonProfile>> context = command.createContext(profile);
        when(persistenceFacade.toEntity(profile)).thenReturn(profile);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistenceFacade.save(profile)).thenReturn(Optional.of(profile));

        command.doCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(profile);
        verify(command).executeDo(context);
    }

    @Test
    void shouldExecuteDoCommandCommand_NotExistsProfileId() {
        Long id = -700L;
        when(profile.getId()).thenReturn(id);
        Context<Optional<? extends PersonProfile>> context = command.createContext(profile);
        when(persistenceFacade.save(profile)).thenReturn(Optional.of(profile));

        command.doCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(id);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotExecuteDoCommandCommand_WrongState() {
        Context<Optional<? extends PersonProfile>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldNotExecuteDoCommandCommand_NotSaved() {
        Context<Optional<? extends PersonProfile>> context = command.createContext(input);

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.getState()).isEqualTo(Context.State.FAIL);

        verify(command).executeDo(context);
    }

    @Test
    void shouldNotExecuteDoCommandCommand_SaveExceptionThrown() {
        Context<Optional<? extends PersonProfile>> context = command.createContext(profile);
        when(persistenceFacade.save(profile)).thenThrow(new RuntimeException());

        command.doCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotExecuteDoCommandCommand_FindEmptyResult() {
        Long id = 701L;
        when(input.getId()).thenReturn(id);
        Context<Optional<? extends PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.empty());

        command.doCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(ProfileNotExistsException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotExecuteDoCommandCommand_FindExceptionThrown() {
        Long id = 702L;
        when(input.getId()).thenReturn(id);
        Context<Optional<? extends PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.findProfileById(id)).thenThrow(new RuntimeException());

        command.doCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotExecuteDoCommandCommand_WrongParameterType() {
        Context<Optional<? extends PersonProfile>> context = command.createContext("input");

        command.doCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldExecuteUndoCommandCommand_ExistsProfile() {
        Long id = 700L;
        when(profile.getId()).thenReturn(id);
        Context<Optional<? extends PersonProfile>> context = command.createContext(profile);
        when(persistenceFacade.toEntity(profile)).thenReturn(profile);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistenceFacade.save(profile)).thenReturn(Optional.of(profile));
        command.doCommand(context);
        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(profile);

        command.undoCommand(context);

        verify(command).executeUndo(context);
        verify(persistenceFacade, atLeastOnce()).saveProfile(profile);
        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
    }

    @Test
    void shouldExecuteUndoCommandCommand_NotWrongProfileId() throws ProfileNotExistsException {
        Long id = -700L;
        when(profile.getId()).thenReturn(id);
        Context<Optional<? extends PersonProfile>> context = command.createContext(profile);
        when(persistenceFacade.save(profile)).thenReturn(Optional.of(profile));
        command.doCommand(context);
        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(id);

        command.undoCommand(context);

        verify(command).executeUndo(context);
        verify(persistenceFacade, atLeastOnce()).save(profile);
        verify(persistenceFacade).deleteProfileById(id);
        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
    }

    @Test
    void shouldNotExecuteUndoCommandCommand_WrongState() {
        Context<Optional<? extends PersonProfile>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotExecuteUndoCommand_WrongUndoCommandParameter() {
        Context<Optional<? extends PersonProfile>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotExecuteUndoCommandCommand_WrongParameterType() {
        Context<Optional<? extends PersonProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter("param");
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotExecuteUndoCommandCommand_DeleteByIdExceptionThrown() throws ProfileNotExistsException {
        Long id = 800L;
        Context<Optional<? extends PersonProfile>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);
        doThrow(new RuntimeException()).when(persistenceFacade).deleteProfileById(id);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotExecuteUndoCommandCommand_SaveProfileExceptionThrown() {
        Context<Optional<? extends PersonProfile>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(input);
        doThrow(new RuntimeException()).when(persistenceFacade).saveProfile(input);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
    }
}