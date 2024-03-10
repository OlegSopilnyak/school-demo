package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
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
class CreateOrUpdateStudentsGroupCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentsGroupCommand command;
    @Mock
    StudentsGroup group;

    @Test
    void shouldExecuteCommand() {
        Optional<StudentsGroup> updated = Optional.of(group);
        when(persistenceFacade.save(group)).thenReturn(updated);

        CommandResult<Optional<StudentsGroup>> result = command.execute(group);

        verify(persistenceFacade).save(group);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(StudentsGroup.class)))).contains(updated.get());
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot save");
        doThrow(cannotExecute).when(persistenceFacade).save(group);

        CommandResult<Optional<StudentsGroup>> result = command.execute(group);

        verify(persistenceFacade).save(group);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Optional.of(mock(StudentsGroup.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}