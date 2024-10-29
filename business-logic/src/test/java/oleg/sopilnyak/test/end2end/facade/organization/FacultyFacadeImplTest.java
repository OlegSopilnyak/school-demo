package oleg.sopilnyak.test.end2end.facade.organization;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.FacultyFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.FacultyPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class FacultyFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_FACULTY_FIND_ALL = "organization.faculty.findAll";
    private static final String ORGANIZATION_FACULTY_FIND_BY_ID = "organization.faculty.findById";
    private static final String ORGANIZATION_FACULTY_CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    private static final String ORGANIZATION_FACULTY_DELETE = "organization.faculty.delete";

    @Autowired
    PersistenceFacade database;

    FacultyPersistenceFacade persistence;
    CommandsFactory<FacultyCommand> factory;
    FacultyFacadeImpl facade;
    BusinessMessagePayloadMapper payloadMapper;

    @BeforeEach
    void setUp() {
        payloadMapper = spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
        persistence = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistence));
        facade = spy(new FacultyFacadeImpl(factory, payloadMapper));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(database).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllFaculties_EmptySet() {

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllFaculties();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllFaculties_NotEmptySet() {
        Faculty faculty = payloadMapper.toPayload(persistFaculty());

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).contains(faculty);
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllFaculties();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindFacultyById() {
        Faculty entity = persistFaculty();
        Long id = entity.getId();

        Optional<Faculty> faculty = facade.findFacultyById(id);

        assertThat(faculty).isPresent();
        assertFacultyEquals(faculty.get(), entity);
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindFacultyById() {
        Long id = 400L;

        Optional<Faculty> faculty = facade.findFacultyById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateFaculty_Create() {
        Faculty facultySource = payloadMapper.toPayload(makeCleanFacultyNoDean(1));

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(facultySource);

        assertThat(faculty).isPresent();
        assertFacultyEquals(faculty.get(), facultySource, false);
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).createContext(facultySource);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(facultySource);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateFaculty_Update() {
        Faculty facultySource = payloadMapper.toPayload(persistFaculty());
        Long id = facultySource.getId();
        reset(payloadMapper);

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(facultySource);

        assertThat(faculty).isPresent();
        assertFacultyEquals(faculty.get(), facultySource);
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).createContext(facultySource);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(payloadMapper, times(2)).toPayload(any(FacultyEntity.class));
        verify(persistence).save(facultySource);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateFaculty() {
        Long id = 401L;
        Faculty facultySource = payloadMapper.toPayload(makeCleanFacultyNoDean(1));
        reset(payloadMapper);
        if (facultySource instanceof FacultyPayload source) {
            source.setId(id);
        }

        UnableExecuteCommandException thrown =
                assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateFaculty(facultySource));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(FacultyNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("Faculty with ID:").endsWith(" is not exists.");
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).createContext(facultySource);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(payloadMapper, never()).toPayload(any(FacultyEntity.class));
        verify(persistence, never()).save(any(Faculty.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteFacultyById() throws FacultyNotFoundException, FacultyIsNotEmptyException {
        Faculty facultySource = persistFaculty();
        Long id = facultySource.getId();

        facade.deleteFacultyById(id);

        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(persistence).deleteFaculty(id);
        assertThat(persistence.findFacultyById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNoDeleteFacultyById_FacultyNotExists() {
        Long id = 403L;

        FacultyNotFoundException thrown = assertThrows(FacultyNotFoundException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:403 is not exists.");
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).deleteFaculty(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNoDeleteFacultyById_FacultyNotEmpty() {
        Faculty facultySource = makeFacultyNoDean(3);
        if (facultySource instanceof FakeFaculty source) {
            source.setId(null);
        }
        Optional<Faculty> faculty = database.save(facultySource);
        assertThat(faculty).isPresent();
        Long id = faculty.get().getId();

        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).startsWith("Faculty with ID").endsWith(" has courses.");
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).deleteFaculty(id);
    }

    // private methods
    private CommandsFactory<FacultyCommand> buildFactory(FacultyPersistenceFacade persistence) {
        return new FacultyCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateFacultyCommand(persistence, payloadMapper)),
                        spy(new DeleteFacultyCommand(persistence, payloadMapper)),
                        spy(new FindAllFacultiesCommand(persistence)),
                        spy(new FindFacultyCommand(persistence))
                )
        );
    }

    private Faculty persistFaculty() {
        Faculty faculty = makeCleanFacultyNoDean(1);
        Faculty entity = database.save(faculty).orElse(null);
        assertThat(entity).isNotNull();
        Optional<Faculty> dbAuthorityPerson = database.findFacultyById(entity.getId());
        assertFacultyEquals(dbAuthorityPerson.orElseThrow(), faculty, false);
        assertThat(dbAuthorityPerson).contains(entity);
        return entity;
    }
}