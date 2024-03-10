package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllAuthorityPersonsCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    FindAllAuthorityPersonsCommand command;

    @Test
    void shouldExecuteCommand() {
        CommandResult<Set<AuthorityPerson>> result = command.execute(null);

        verify(persistenceFacade).findAllAuthorityPersons();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(AuthorityPerson.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findAllAuthorityPersons();

        CommandResult<Set<AuthorityPerson>> result = command.execute(null);

        verify(persistenceFacade).findAllAuthorityPersons();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Set.of(mock(AuthorityPerson.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}