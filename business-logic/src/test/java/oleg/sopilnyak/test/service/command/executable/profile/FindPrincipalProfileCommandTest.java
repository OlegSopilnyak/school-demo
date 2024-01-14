package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FindPrincipalProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @Mock
    PrincipalProfile profile;
    @Spy
    @InjectMocks
    FindPrincipalProfileCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 406L;

        CommandResult<Optional<PrincipalProfile>> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 407L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findProfileById(id);

        CommandResult<Optional<PrincipalProfile>> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}