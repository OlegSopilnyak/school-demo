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
class CreatePrincipalProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    CreatePrincipalProfileCommand command;
    @Mock
    PrincipalProfile input;

    @Test
    void shouldExecuteCommand() {

        CommandResult<Optional<PrincipalProfile>> result = command.execute(input);

        verify(persistenceFacade).save(input);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistenceFacade).save(input);


        CommandResult<Optional<PrincipalProfile>> result = command.execute(input);

        verify(persistenceFacade).save(input);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}