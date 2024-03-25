package oleg.sopilnyak.test.service.command.executable.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
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
class FindAllStudentsGroupsCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    FindAllStudentsGroupsCommand command;

    @Test
    void shouldExecuteCommand() {
        CommandResult<Set<StudentsGroup>> result = command.execute(null);

        verify(persistenceFacade).findAllStudentsGroups();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(StudentsGroup.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findAllStudentsGroups();

        CommandResult<Set<StudentsGroup>> result = command.execute(null);

        verify(persistenceFacade).findAllStudentsGroups();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Set.of(mock(StudentsGroup.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}