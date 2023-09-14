package oleg.sopilnyak.test.service.command.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.CommandResult;
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
class CreateOrUpdateAuthorityPersonCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    CreateOrUpdateAuthorityPersonCommand command;
    @Mock
    AuthorityPerson person;

    @Test
    void shouldExecuteCommand() {
        Optional<AuthorityPerson> updated = Optional.of(person);
        when(persistenceFacade.save(person)).thenReturn(updated);

        CommandResult<Optional<AuthorityPerson>> result = command.execute(person);

        verify(persistenceFacade).save(person);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isEqualTo(updated);
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot save");
        doThrow(cannotExecute).when(persistenceFacade).save(person);

        CommandResult<Optional<AuthorityPerson>> result = command.execute(person);

        verify(persistenceFacade).save(person);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}