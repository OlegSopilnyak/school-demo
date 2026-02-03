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
import oleg.sopilnyak.test.service.command.type.core.Context;
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
    private static final String ORGANIZATION_FACULTY_FIND_ALL = "school::organization::faculties:find.All";
    private static final String ORGANIZATION_FACULTY_FIND_BY_ID = "school::organization::faculties:find.By.Id";
    private static final String ORGANIZATION_FACULTY_CREATE_OR_UPDATE = "school::organization::faculties:create.Or.Update";
    private static final String ORGANIZATION_FACULTY_DELETE = "school::organization::faculties:delete";
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
        ActionContext.setup("test-facade", "test-action");
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @Test
    void shouldFindAllFaculties_EmptySet_Unified() {
        String commandId = ORGANIZATION_FACULTY_FIND_ALL;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFindAll", FacultyCommand.class);

        Collection<Faculty> result = facade.doActionAndResult(commandId);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldFindAllFaculties_EmptySet() {
        String commandId = ORGANIZATION_FACULTY_FIND_ALL;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFindAll", FacultyCommand.class);

        Collection<Faculty> result = ReflectionTestUtils.invokeMethod(facade, "internalFindAll");

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldFindAllFaculties_NotEmptySet() {
        String commandId = ORGANIZATION_FACULTY_FIND_ALL;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFindAll", FacultyCommand.class);
        when(persistence.findAllFaculties()).thenReturn(Set.of(mockFaculty));

        Collection<Faculty> result = ReflectionTestUtils.invokeMethod(facade, "internalFindAll");

        assertThat(result).hasSize(1);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.empty());
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldNotFindFacultyById_Unified() {
        String commandId = ORGANIZATION_FACULTY_FIND_BY_ID;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFind", FacultyCommand.class);
        Long id = 400L;

        Optional<Faculty> result = facade.doActionAndResult(commandId, id);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldNotFindFacultyById() {
        String commandId = ORGANIZATION_FACULTY_FIND_BY_ID;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyFind", FacultyCommand.class);
        Long id = 401L;

        Optional<Faculty> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
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

        Optional<Faculty> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).contains(mockFacultyPayload);
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldCreateOrUpdateFaculty_Unified() {
        String commandId = ORGANIZATION_FACULTY_CREATE_OR_UPDATE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyUpdate", FacultyCommand.class);
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);
        when(payloadMapper.toPayload(mockFacultyPayload)).thenReturn(mockFacultyPayload);
        when(persistence.save(mockFacultyPayload)).thenReturn(Optional.of(mockFacultyPayload));

        Optional<Faculty> result = facade.doActionAndResult(commandId, mockFaculty);

        assertThat(result).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(mockFacultyPayload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).save(mockFacultyPayload);
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

        Optional<Faculty> result = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", mockFaculty);

        assertThat(result).isPresent();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(mockFacultyPayload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).save(mockFacultyPayload);
    }

    @Test
    void shouldNotCreateOrUpdateFaculty() {
        String commandId = ORGANIZATION_FACULTY_CREATE_OR_UPDATE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyUpdate", FacultyCommand.class);
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);

        Optional<Faculty> result = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", mockFaculty);

        assertThat(result).isEmpty();
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(mockFacultyPayload));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).save(mockFacultyPayload);
    }

    @Test
    void shouldDeleteFacultyById_Unified() throws FacultyNotFoundException, FacultyIsNotEmptyException {
        String commandId = ORGANIZATION_FACULTY_DELETE;
        FacultyCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("facultyDelete", FacultyCommand.class);
        Long id = 402L;
        when(persistence.findFacultyById(id)).thenReturn(Optional.of(mockFaculty));
        when(payloadMapper.toPayload(mockFaculty)).thenReturn(mockFacultyPayload);

        facade.doActionAndResult(commandId, id);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
        verify(persistence).findFacultyById(id);
        verify(persistence).deleteFaculty(id);
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

        ReflectionTestUtils.invokeMethod(facade, "internalDeleteById", id);

        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
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

        FacultyNotFoundException thrown = assertThrows(FacultyNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDeleteById", id)
        );

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:403 is not exists.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
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

        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDeleteById", id)
        );

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:404 has courses.");
        verify(factory).command(commandId);
        verify(factory.command(commandId)).createContext(Input.of(id));
        verify(factory.command(commandId)).doCommand(any(Context.class));
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