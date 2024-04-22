package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.FacultyNotExistsException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.factory.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.facade.impl.FacultyFacadeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyFacadeImplTest {
    private static final String ORGANIZATION_FACULTY_FIND_ALL = "organization.faculty.findAll";
    private static final String ORGANIZATION_FACULTY_FIND_BY_ID = "organization.faculty.findById";
    private static final String ORGANIZATION_FACULTY_CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    private static final String ORGANIZATION_FACULTY_DELETE = "organization.faculty.delete";
    FacultyPersistenceFacade persistenceFacade = mock(FacultyPersistenceFacade.class);
    @Spy
    CommandsFactory<?> factory = buildFactory();
    @Mock
    Faculty mockFaculty;

    @Spy
    @InjectMocks
    FacultyFacadeImpl facade;

    @Test
    void shouldFindAllFaculties() {

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void shouldFindAllFaculties2() {
        when(persistenceFacade.findAllFaculties()).thenReturn(Set.of(mockFaculty));

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).hasSize(1);
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).execute(null);
        verify(persistenceFacade).findAllFaculties();
    }

    @Test
    void shouldNotGetFacultyById() {
        Long id = 400L;

        Optional<Faculty> faculty = facade.getFacultyById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
    }

    @Test
    void shouldGetFacultyById() {
        Long id = 410L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        Optional<Faculty> faculty = facade.getFacultyById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
    }

    @Test
    void shouldCreateOrUpdateFaculty() {
        when(persistenceFacade.save(mockFaculty)).thenReturn(Optional.of(mockFaculty));

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).execute(mockFaculty);
        verify(persistenceFacade).save(mockFaculty);
    }

    @Test
    void shouldNotCreateOrUpdateFaculty() {

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).execute(mockFaculty);
        verify(persistenceFacade).save(mockFaculty);
    }

    @Test
    void shouldDeleteFacultyById() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 402L;
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        facade.deleteFacultyById(id);

        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotExists() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 403L;

        FacultyNotExistsException thrown = assertThrows(FacultyNotExistsException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:403 is not exists.");
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotEmpty() throws FacultyNotExistsException, FacultyIsNotEmptyException {
        Long id = 404L;
        when(mockFaculty.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:404 has courses.");
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
        verify(persistenceFacade).findFacultyById(id);
        verify(persistenceFacade, never()).deleteFaculty(id);
    }

    private CommandsFactory<?> buildFactory() {
        return new FacultyCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateFacultyCommand(persistenceFacade)),
                        spy(new DeleteFacultyCommand(persistenceFacade)),
                        spy(new FindAllFacultiesCommand(persistenceFacade)),
                        spy(new FindFacultyCommand(persistenceFacade))
                )
        );
    }
}