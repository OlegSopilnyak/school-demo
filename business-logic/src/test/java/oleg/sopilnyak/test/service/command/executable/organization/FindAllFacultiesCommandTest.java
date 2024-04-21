package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
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
class FindAllFacultiesCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    FindAllFacultiesCommand command;

    @Test
    void shouldExecuteCommand() {
        CommandResult<Set<Faculty>> result = command.execute(null);

        verify(persistenceFacade).findAllFaculties();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Faculty.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findAllFaculties();

        CommandResult<Set<Faculty>> result = command.execute(null);

        verify(persistenceFacade).findAllFaculties();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Set.of(mock(Faculty.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}