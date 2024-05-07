package oleg.sopilnyak.test.end2end.facade.organization;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentsGroupException;
import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.StudentsGroupFacadeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class StudentsGroupFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "organization.students.group.findAll";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "organization.students.group.findById";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "organization.students.group.delete";

    @Autowired
    PersistenceFacade database;

    StudentsGroupPersistenceFacade persistence;
    CommandsFactory<StudentsGroupCommand> factory;
    StudentsGroupFacadeImpl facade;

    @BeforeEach
    void setUp() {
        persistence = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistence));
        facade = spy(new StudentsGroupFacadeImpl(factory));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(database).isNotNull();
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAllStudentsGroup() {
        StudentsGroup group = persistStudentsGroup();

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).contains(group);
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllStudentsGroups();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindAllStudentsGroup() {

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllStudentsGroups();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindStudentsGroupById() {
        StudentsGroup group = persistStudentsGroup();
        Long id = group.getId();

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindStudentsGroupById() {
        Long id = 510L;

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateStudentsGroup_Create() {
        Long id = 511L;
        StudentsGroup group = makeCleanStudentsGroup(3);
        if (group instanceof FakeStudentsGroup fake) {
            fake.setId(id);
        }

        UnableExecuteCommandException thrown =
                assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateStudentsGroup(group));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(NotExistStudentsGroupException.class);
        assertThat(cause.getMessage()).startsWith("Students Group with ID:").endsWith(" is not exists.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).createContext(group);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).save(any(StudentsGroup.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateStudentsGroup_Create() {
        StudentsGroup group = makeCleanStudentsGroup(3);

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(group);

        assertThat(faculty).isPresent();
        assertStudentsGroupEquals(group, faculty.get(), false);
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).createContext(group);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(group);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateStudentsGroup_Update() {
        StudentsGroup group = persistStudentsGroup();
        Long id = group.getId();

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(group);

        assertThat(faculty).isPresent();
        assertStudentsGroupEquals(group, faculty.get());
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).createContext(group);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence).toEntity(group);
        verify(persistence).save(group);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudentsGroupById() {
        StudentsGroup group = persistStudentsGroup();
        Long id = group.getId();

        facade.deleteStudentsGroupById(id);

        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence).toEntity(group);
        verify(persistence).deleteStudentsGroup(id);
        assertThat(persistence.findStudentsGroupById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudentsGroupById_GroupNotExists() {
        Long id = 503L;
        NotExistStudentsGroupException thrown =
                assertThrows(NotExistStudentsGroupException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("Students Group with ID:503 is not exists.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).deleteStudentsGroup(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, NotExistStudentsGroupException {
        StudentsGroup studentsGroup = makeCleanStudentsGroup(3);
        if (studentsGroup instanceof FakeStudentsGroup fake) {
            fake.setStudents(List.of(makeClearStudent(1)));
        }
        StudentsGroup group = database.save(studentsGroup).orElseThrow();
        Long id = group.getId();

        StudentGroupWithStudentsException thrown =
                assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).startsWith("Students Group with ID:").endsWith(" has students.");
        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).createContext(id);
        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).deleteStudentsGroup(anyLong());
    }

    // private methods
    private CommandsFactory<StudentsGroupCommand> buildFactory(StudentsGroupPersistenceFacade persistence) {
        return new StudentsGroupCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateStudentsGroupCommand(persistence)),
                        spy(new DeleteStudentsGroupCommand(persistence)),
                        spy(new FindAllStudentsGroupsCommand(persistence)),
                        spy(new FindStudentsGroupCommand(persistence))
                )

        );
    }

    private StudentsGroup persistStudentsGroup() {
        StudentsGroup group = makeCleanStudentsGroup(1);
        if (group instanceof FakeStudentsGroup fake) {
            fake.setLeader(null);
            fake.setStudents(List.of());
        }
        StudentsGroup entity = database.save(group).orElse(null);
        assertThat(entity).isNotNull();
        Optional<StudentsGroup> dbStudentsGroup = database.findStudentsGroupById(entity.getId());
        assertStudentsGroupEquals(dbStudentsGroup.orElseThrow(), group, false);
        assertThat(dbStudentsGroup).contains(entity);
        return database.toEntity(entity);
    }
}