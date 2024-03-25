package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
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
class FindAuthorityPersonCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Mock
    AuthorityPerson instance;
    @Spy
    @InjectMocks
    FindAuthorityPersonCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 306L;

        CommandResult<Optional<AuthorityPerson>> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(AuthorityPerson.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldExecuteCommand_PersonFound() {
        Long id = 307L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(instance));

        CommandResult<Optional<AuthorityPerson>> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(AuthorityPerson.class)))).contains(instance);
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 308L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findAuthorityPersonById(id);

        CommandResult<Optional<AuthorityPerson>> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Optional.of(mock(AuthorityPerson.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}