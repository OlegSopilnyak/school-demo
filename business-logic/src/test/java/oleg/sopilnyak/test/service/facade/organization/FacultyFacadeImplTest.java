package oleg.sopilnyak.test.service.facade.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.facade.organization.impl.FacultyFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class FacultyFacadeImplTest {
    private static final String ORGANIZATION_FACULTY_FIND_ALL = "organization.faculty.findAll";
    private static final String ORGANIZATION_FACULTY_FIND_BY_ID = "organization.faculty.findById";
    private static final String ORGANIZATION_FACULTY_CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    private static final String ORGANIZATION_FACULTY_DELETE = "organization.faculty.delete";
    FacultyPersistenceFacade persistence = mock(FacultyPersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);

    CommandsFactory<FacultyCommand<?>> factory;
    FacultyFacadeImpl facade;
    @Mock
    CommandActionExecutor actionExecutor;
    @Mock
    ApplicationContext applicationContext;

    @Mock
    Faculty mockFaculty;
    @Mock
    FacultyPayload mockFacultyPayload;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory());
        facade = spy(new FacultyFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-doingMainLoop");
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @Test
    void shouldFindAllFaculties_EmptySet() {
        String commandId = ORGANIZATION_FACULTY_FIND_ALL;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFindAll", FacultyCommand.class);

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).createContext(Input.empty());
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldFindAllFaculties_NotEmptySet() {
        String commandId = ORGANIZATION_FACULTY_FIND_ALL;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFindAll", FacultyCommand.class);
        when(persistence.findAllFaculties()).thenReturn(Set.of(mockFaculty));

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).hasSize(1);
        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).createContext(Input.empty());
        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldNotFindFacultyById() {
        String commandId = ORGANIZATION_FACULTY_FIND_BY_ID;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFind", FacultyCommand.class);
        Long id = 400L;

        Optional<Faculty> faculty = facade.findFacultyById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldFindFacultyById() {
        String commandId = ORGANIZATION_FACULTY_FIND_BY_ID;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFind", FacultyCommand.class);
        Long id = 410L;
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);
        when(persistence.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));

        Optional<Faculty> faculty = facade.findFacultyById(id);

        assertThat(faculty).contains(mockFacultyPayload);
        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldCreateOrUpdateFaculty() {
        String commandId = ORGANIZATION_FACULTY_CREATE_OR_UPDATE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyUpdate", FacultyCommand.class);
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);
        when(payloadMapper.toPayload(mockFacultyPayload)).thenReturn(mockFacultyPayload);
        when(persistence.save(mockFacultyPayload)).thenReturn(Optional.of(mockFacultyPayload));

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).createContext(Input.of(mockFacultyPayload));
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(mockFacultyPayload);
    }

    @Test
    void shouldNotCreateOrUpdateFaculty() {
        String commandId = ORGANIZATION_FACULTY_CREATE_OR_UPDATE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyUpdate", FacultyCommand.class);
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(mockFaculty);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).createContext(Input.of(mockFacultyPayload));
        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(mockFacultyPayload);
    }

    @Test
    void shouldDeleteFacultyById() throws FacultyNotFoundException, FacultyIsNotEmptyException {
        String commandId = ORGANIZATION_FACULTY_DELETE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyDelete", FacultyCommand.class);
        Long id = 402L;
        when(persistence.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);

        facade.deleteFacultyById(id);

        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(persistence).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotExists() throws FacultyNotFoundException, FacultyIsNotEmptyException {
        String commandId = ORGANIZATION_FACULTY_DELETE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyDelete", FacultyCommand.class);
        Long id = 403L;

        FacultyNotFoundException thrown = assertThrows(FacultyNotFoundException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:403 is not exists.");
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotEmpty() throws FacultyNotFoundException, FacultyIsNotEmptyException {
        String commandId = ORGANIZATION_FACULTY_DELETE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyDelete", FacultyCommand.class);
        Long id = 404L;
        when(persistence.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);
        when(mockFacultyPayload.getCourses()).thenReturn(List.of(mock(Course.class)));

        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:404 has courses.");
        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).deleteFaculty(id);
    }

    private CommandsFactory<FacultyCommand<?>> buildFactory() {
        List<FacultyCommand<?>> commands = List.of(
                spy(new CreateOrUpdateFacultyCommand(persistence, payloadMapper)),
                spy(new DeleteFacultyCommand(persistence, payloadMapper)),
                spy(new FindAllFacultiesCommand(persistence, payloadMapper)),
                spy(new FindFacultyCommand(persistence, payloadMapper))
        );
        String acName = "applicationContext";
        commands.forEach(command -> {
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
        });
        return new FacultyCommandsFactory(commands);
    }
}