package oleg.sopilnyak.test.service.command.organization;

import oleg.sopilnyak.test.school.common.exception.AuthorityPersonIsNotExistsException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteAuthorityPersonCommandTest {
    @Mock
    OrganizationPersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    DeleteAuthorityPersonCommand command;
    @Mock
    AuthorityPerson person;

    @Test
    void shouldExecuteCommand() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Long id = 310L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(person));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);
        verify(persistenceFacade).deleteAuthorityPerson(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isTrue();
        assertThat(result.getException()).isNull();
    }
    @Test
    void shouldNotExecuteCommand_NoPerson() {
        Long id = 311L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(AuthorityPersonIsNotExistsException.class);
    }
    @Test
    void shouldNotExecuteCommand_PersonBusy() {
        Long id = 312L;
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(person));
        when(person.getFaculties()).thenReturn(List.of(mock(Faculty.class)));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(AuthorityPersonManageFacultyException.class);
    }
    @Test
    void shouldNotExecuteCommand_CannotDelete() throws AuthorityPersonManageFacultyException, AuthorityPersonIsNotExistsException {
        Long id = 313L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistenceFacade).deleteAuthorityPerson(id);
        when(persistenceFacade.findAuthorityPersonById(id)).thenReturn(Optional.of(person));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findAuthorityPersonById(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}