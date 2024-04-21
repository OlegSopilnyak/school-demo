package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Disabled;
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
    @Disabled
    void shouldExecuteCommand() {
        Long id = 306L;

        CommandResult<Optional<AuthorityPerson>> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(AuthorityPerson.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
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
    @Disabled
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

    @Test
    void shouldDoCommand_PersonNotFound() {
        Long id = 306L;
        Context<Optional<AuthorityPerson>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<AuthorityPerson> result = (Optional<AuthorityPerson>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistenceFacade).findAuthorityPersonById(id);
    }
}