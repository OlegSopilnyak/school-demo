package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @InjectMocks
    CreateProfileCommand command;
    @Mock
    PersonProfile input;

    @Test
    void shouldExecuteCommand() {

        CommandResult<Optional<PersonProfile>> result = command.execute(input);

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


        CommandResult<Optional<PersonProfile>> result = command.execute(input);

        verify(persistenceFacade).saveProfile(input);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}