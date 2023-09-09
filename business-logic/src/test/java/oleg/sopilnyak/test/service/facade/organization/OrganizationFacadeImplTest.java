package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.organization.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.organization.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.organization.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.facade.organization.entity.AuthorityPersonCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.FacultyCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.StudentsGroupCommandFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrganizationFacadeImplTest {
    OrganizationPersistenceFacade persistenceFacade = mock(OrganizationPersistenceFacade.class);
    @Spy
    CommandsFactory factory = buildFactory();

    @Spy
    @InjectMocks
    OrganizationFacadeImpl facade;

    @Test
    void shouldFindAllAuthorityPersons() {
        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(AuthorityPersonCommandFacade.FIND_ALL);
        verify(persistenceFacade).findAllAuthorityPersons();
    }

    @Test
    void getAuthorityPersonById() {
    }

    @Test
    void createOrUpdateAuthorityPerson() {
    }

    @Test
    void deleteAuthorityPersonById() {
    }

    @Test
    void shouldFindAllFaculties() {
        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verify(factory).command(FacultyCommandFacade.FIND_ALL);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void getFacultyById() {
    }

    @Test
    void createOrUpdateFaculty() {
    }

    @Test
    void deleteFacultyById() {
    }

    @Test
    void findAllStudentsGroup() {
        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verify(factory).command(StudentsGroupCommandFacade.FIND_ALL);
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void getStudentsGroupById() {
    }

    @Test
    void createOrUpdateStudentsGroup() {
    }

    @Test
    void deleteStudentsGroupById() {
    }
    private CommandsFactory buildFactory() {
        return new SchoolCommandsFactory(
                Map.of(
                        AuthorityPersonCommandFacade.FIND_ALL, new FindAllAuthorityPersonsCommand(persistenceFacade),
                        FacultyCommandFacade.FIND_ALL, new FindAllFacultiesCommand(persistenceFacade),
                        StudentsGroupCommandFacade.FIND_ALL, new FindAllStudentsGroupsCommand(persistenceFacade)
                )
        );
    }
}