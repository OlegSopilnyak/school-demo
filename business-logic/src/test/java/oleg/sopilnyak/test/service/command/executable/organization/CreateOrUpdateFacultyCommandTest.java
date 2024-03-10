package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
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
class CreateOrUpdateFacultyCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    CreateOrUpdateFacultyCommand command;
    @Mock
    Faculty faculty;

    @Test
    void shouldExecuteCommand() {
        Optional<Faculty> updated = Optional.of(faculty);
        when(persistenceFacade.save(faculty)).thenReturn(updated);

        CommandResult<Optional<Faculty>> result = command.execute(faculty);

        verify(persistenceFacade).save(faculty);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(Faculty.class)))).contains(updated.get());
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot save");
        doThrow(cannotExecute).when(persistenceFacade).save(faculty);

        CommandResult<Optional<Faculty>> result = command.execute(faculty);

        verify(persistenceFacade).save(faculty);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Optional.of(mock(Faculty.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}