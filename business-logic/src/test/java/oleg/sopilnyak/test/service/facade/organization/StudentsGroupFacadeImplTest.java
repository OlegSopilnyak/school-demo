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
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.facade.organization.impl.StudentsGroupFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class StudentsGroupFacadeImplTest {
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "organization.students.group.findAll";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "organization.students.group.findById";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "organization.students.group.delete";

    StudentsGroupPersistenceFacade persistenceFacade = mock(StudentsGroupPersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    @Spy
    CommandsFactory<StudentsGroupCommand<?>> factory;
    @Spy
    @InjectMocks
    StudentsGroupFacadeImpl facade;
    @Mock
    ApplicationContext applicationContext;
    @Mock
    CommandActionExecutor actionExecutor;

    @Mock
    StudentsGroup mockGroup;
    @Mock
    StudentsGroupPayload mockGroupPayload;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory());
        facade = spy(new StudentsGroupFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-doingMainLoop");
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
    }

    @Test
    void shouldFindAllStudentsGroup() {
        String commandId = ORGANIZATION_STUDENTS_GROUP_FIND_ALL;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupFindAll", StudentsGroupCommand.class);
        when(persistenceFacade.findAllStudentsGroups()).thenReturn(Set.of(mockGroup));

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).hasSize(1);
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).createContext(Input.empty());
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldNotFindAllStudentsGroup() {
        String commandId = ORGANIZATION_STUDENTS_GROUP_FIND_ALL;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupFindAll", StudentsGroupCommand.class);

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).createContext(Input.empty());
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).doCommand(any(Context.class));
        verify(persistenceFacade).findAllStudentsGroups();
    }

    @Test
    void shouldFindStudentsGroupById() {
        String commandId = ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupFind", StudentsGroupCommand.class);
        Long id = 500L;
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldNotFindStudentsGroupById() {
        String commandId = ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupFind", StudentsGroupCommand.class);
        Long id = 510L;

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
    }

    @Test
    void shouldCreateOrUpdateStudentsGroup() {
        String commandId = ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupUpdate", StudentsGroupCommand.class);
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(payloadMapper.toPayload(mockGroupPayload)).thenReturn(mockGroupPayload);
        when(persistenceFacade.save(mockGroupPayload)).thenReturn(Optional.of(mockGroupPayload));

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(mockGroup);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).createContext(Input.of(mockGroupPayload));
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(mockGroupPayload);
    }

    @Test
    void shouldDeleteStudentsGroupById() throws StudentGroupWithStudentsException, StudentsGroupNotFoundException {
        String commandId = ORGANIZATION_STUDENTS_GROUP_DELETE;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupDelete", StudentsGroupCommand.class);
        Long id = 502L;
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        facade.deleteStudentsGroupById(id);

        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotExists() throws StudentGroupWithStudentsException, StudentsGroupNotFoundException {
        String commandId = ORGANIZATION_STUDENTS_GROUP_DELETE;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupDelete", StudentsGroupCommand.class);
        Long id = 503L;
        StudentsGroupNotFoundException thrown =
                assertThrows(StudentsGroupNotFoundException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("Students Group with ID:503 is not exists.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, StudentsGroupNotFoundException {
        String commandId = ORGANIZATION_STUDENTS_GROUP_DELETE;
        StudentsGroupCommand<?> command = factory.command(commandId);
        reset(factory);
        doReturn(command).when(applicationContext).getBean("studentsGroupDelete", StudentsGroupCommand.class);
        Long id = 504L;
        when(payloadMapper.toPayload(mockGroup)).thenReturn(mockGroupPayload);
        when(mockGroupPayload.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistenceFacade.findStudentsGroupById(id)).thenReturn(Optional.of(mockGroup));

        StudentGroupWithStudentsException thrown =
                assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("Students Group with ID:504 has students.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentsGroupById(id);
        verify(persistenceFacade, never()).deleteStudentsGroup(id);
    }

    private CommandsFactory<StudentsGroupCommand<?>> buildFactory() {
        List<StudentsGroupCommand<?>> commands = List.of(
                spy(new CreateOrUpdateStudentsGroupCommand(persistenceFacade, payloadMapper)),
                spy(new DeleteStudentsGroupCommand(persistenceFacade, payloadMapper)),
                spy(new FindAllStudentsGroupsCommand(persistenceFacade, payloadMapper)),
                spy(new FindStudentsGroupCommand(persistenceFacade, payloadMapper))
        );
        String acName = "applicationContext";
        commands.forEach(command -> {
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
        });
        return new StudentsGroupCommandsFactory(commands);
    }
}