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
class FindFacultyCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Mock
    Faculty instance;
    @Spy
    @InjectMocks
    FindFacultyCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 406L;

        CommandResult<Optional<Faculty>> result = command.execute(id);

        verify(persistenceFacade).findFacultyById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(Faculty.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldExecuteCommand_FacultyFound() {
        Long id = 407L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(instance));

        CommandResult<Optional<Faculty>> result = command.execute(id);

        verify(persistenceFacade).findFacultyById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(Faculty.class)))).contains(instance);
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 408L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findFacultyById(id);

        CommandResult<Optional<Faculty>> result = command.execute(id);

        verify(persistenceFacade).findFacultyById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Optional.of(mock(Faculty.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}