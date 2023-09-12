package oleg.sopilnyak.test.service.command.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
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
class FindStudentsGroupCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Mock
    StudentsGroup instance;
    @Spy
    @InjectMocks
    FindStudentsGroupCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 506L;

        CommandResult<Optional<StudentsGroup>> result = command.execute(id);

        verify(persistenceFacade).findStudentsGroupById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldExecuteCommand_FacultyFound() {
        Long id = 507L;
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(instance));

        CommandResult<Optional<StudentsGroup>> result = command.execute(id);

        verify(persistenceFacade).findStudentsGroupById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isEqualTo(Optional.of(instance));
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 508L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findStudentsGroupById(id);

        CommandResult<Optional<StudentsGroup>> result = command.execute(id);

        verify(persistenceFacade).findStudentsGroupById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}