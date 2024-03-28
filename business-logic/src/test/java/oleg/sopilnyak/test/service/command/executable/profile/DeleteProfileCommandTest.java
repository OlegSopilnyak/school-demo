package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @InjectMocks
    DeleteProfileCommand command;

    @Mock
    PersonProfile input;

    @Test
    void shouldExecuteCommand() throws ProfileNotExistsException {
        long id = 404L;
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(input));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade).deleteProfileById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand_ProfileNotExists() throws ProfileNotExistsException {
        long id = 405L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade, never()).deleteProfileById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(ProfileNotExistsException.class);
    }

    @Test
    void shouldNotExecuteCommand_WrongIdType() {

        CommandResult<Boolean> result = command.execute("id");

        verify(persistenceFacade, never()).findProfileById(anyLong());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(ClassCastException.class);
    }

    @Test
    void shouldNotExecuteCommand_NullId() throws ProfileNotExistsException {
        when(persistenceFacade.findProfileById(null)).thenReturn(Optional.of(input));
        doThrow(new RuntimeException()).when(persistenceFacade).deleteProfileById(null);

        CommandResult<Boolean> result = command.execute(null);

        verify(persistenceFacade).findProfileById(null);
        verify(persistenceFacade).deleteProfileById(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldNotExecuteCommand_ExceptionThrown() throws ProfileNotExistsException {
        long id = 405L;
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(input));
        doThrow(new UnsupportedOperationException()).when(persistenceFacade).deleteProfileById(id);

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade).deleteProfileById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(UnsupportedOperationException.class);
    }
}