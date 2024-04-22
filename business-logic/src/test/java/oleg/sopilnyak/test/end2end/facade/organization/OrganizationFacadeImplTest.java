package oleg.sopilnyak.test.end2end.facade.organization;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.base.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.facade.impl.OrganizationFacadeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class OrganizationFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE = "organization.authority.person.delete";
    private static final String ORGANIZATION_FACULTY_FIND_ALL = "organization.faculty.findAll";
    private static final String ORGANIZATION_FACULTY_FIND_BY_ID = "organization.faculty.findById";
    private static final String ORGANIZATION_FACULTY_CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    private static final String ORGANIZATION_FACULTY_DELETE = "organization.faculty.delete";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "organization.students.group.findAll";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "organization.students.group.findById";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "organization.students.group.delete";
    @Autowired
    PersistenceFacade database;
    PersistenceFacade persistenceFacade;
    CommandsFactory<?> factory;
    OrganizationFacadeImpl facade;

    @Mock
    StudentsGroup mockGroup;


    @BeforeEach
    void setUp() {
        persistenceFacade = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new OrganizationFacadeImpl(factory));
    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindAllAuthorityPersons() {
//        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();
//
//        assertThat(persons).isEmpty();
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).execute(null);
//        verify(persistenceFacade).findAllAuthorityPersons();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldFindAllAuthorityPersons() {
//        AuthorityPerson person = makeCleanAuthorityPerson(0);
//        getPersistent(person);
//
//        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();
//
//        assertThat(persons).hasSize(1);
//        assertAuthorityPersonEquals(person, persons.iterator().next(), false);
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).execute(null);
//        verify(persistenceFacade).findAllAuthorityPersons();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotGetAuthorityPersonById() {
//        Long id = 300L;
//
//        Optional<AuthorityPerson> person = facade.getAuthorityPersonById(id);
//
//        assertThat(person).isEmpty();
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).execute(id);
//        verify(persistenceFacade).findAuthorityPersonById(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldGetAuthorityPersonById() {
//        AuthorityPerson person = makeCleanAuthorityPerson(0);
//        Long id = getPersistent(person).getId();
//
//        Optional<AuthorityPerson> foundPerson = facade.getAuthorityPersonById(id);
//
//        assertThat(foundPerson).isPresent();
//        assertAuthorityPersonEquals(person, foundPerson.get(), false);
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).execute(id);
//        verify(persistenceFacade).findAuthorityPersonById(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldCreateOrUpdateAuthorityPerson() {
//        AuthorityPerson person = makeCleanAuthorityPerson(0);
//
//        Optional<AuthorityPerson> authorityPerson = facade.createOrUpdateAuthorityPerson(person);
//
//        assertThat(authorityPerson).isPresent();
//        assertAuthorityPersonEquals(person, authorityPerson.get(), false);
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).execute(person);
//        verify(persistenceFacade).save(person);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
//        AuthorityPerson person = makeCleanAuthorityPerson(0);
//        if (person instanceof FakeAuthorityPerson f) {
//            f.setFaculties(List.of());
//        }
//        Long id = getPersistent(person).getId();
//        assertThat(database.findAuthorityPersonById(id)).isPresent();
//
//        facade.deleteAuthorityPersonById(id);
//
//        assertThat(database.findAuthorityPersonById(id)).isEmpty();
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
//        verify(persistenceFacade).findAuthorityPersonById(id);
//        verify(persistenceFacade).deleteAuthorityPerson(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeleteAuthorityPersonById_PersonNotExists() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
//        Long id = 303L;
//
//        NotExistAuthorityPersonException thrown =
//                assertThrows(NotExistAuthorityPersonException.class, () -> facade.deleteAuthorityPersonById(id));
//
//        assertEquals("AuthorityPerson with ID:303 is not exists.", thrown.getMessage());
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
//        verify(persistenceFacade).findAuthorityPersonById(id);
//        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManageFacultyException, NotExistAuthorityPersonException {
//        AuthorityPerson person = makeCleanAuthorityPerson(0);
//        Long id = getPersistent(person).getId();
//        assertThat(database.findAuthorityPersonById(id)).isPresent();
//
//        AuthorityPersonManageFacultyException thrown =
//                assertThrows(AuthorityPersonManageFacultyException.class, () -> facade.deleteAuthorityPersonById(id));
//
//        assertEquals("AuthorityPerson with ID:" + id + " is managing faculties.", thrown.getMessage());
//
//        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE);
//        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE)).execute(id);
//        verify(persistenceFacade).findAuthorityPersonById(id);
//        verify(persistenceFacade, never()).deleteAuthorityPerson(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindAllFaculties() {
//        Collection<Faculty> faculties = facade.findAllFaculties();
//
//        assertThat(faculties).isEmpty();
//        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
//        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).execute(null);
//        verify(persistenceFacade).findAllFaculties();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldFindAllFaculties() {
//        Faculty faculty = makeCleanFaculty(0);
//        getPersistent(faculty);
//
//        Collection<Faculty> faculties = facade.findAllFaculties();
//
//        assertThat(faculties).hasSize(1);
//        assertFacultyEquals(faculty, faculties.iterator().next(), false);
//        verify(factory).command(ORGANIZATION_FACULTY_FIND_ALL);
//        verify(factory.command(ORGANIZATION_FACULTY_FIND_ALL)).execute(null);
//        verify(persistenceFacade).findAllFaculties();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotGetFacultyById() {
//        Long id = 400L;
//
//        Optional<Faculty> faculty = facade.getFacultyById(id);
//
//        assertThat(faculty).isEmpty();
//        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
//        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).execute(id);
//        verify(persistenceFacade).findFacultyById(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldGetFacultyById() {
//        Faculty cleanFaculty = makeCleanFaculty(0);
//        Long id = getPersistent(cleanFaculty).getId();
//
//        Optional<Faculty> faculty = facade.getFacultyById(id);
//
//        assertThat(faculty).isPresent();
//        assertFacultyEquals(cleanFaculty, faculty.get(), false);
//        verify(factory).command(ORGANIZATION_FACULTY_FIND_BY_ID);
//        verify(factory.command(ORGANIZATION_FACULTY_FIND_BY_ID)).execute(id);
//        verify(persistenceFacade).findFacultyById(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldCreateOrUpdateFaculty() {
//        Faculty cleanFaculty = makeCleanFaculty(0);
//
//        Optional<Faculty> faculty = facade.createOrUpdateFaculty(cleanFaculty);
//
//        assertThat(faculty).isPresent();
//        assertFacultyEquals(cleanFaculty, faculty.get(), false);
//        verify(factory).command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE);
//        verify(factory.command(ORGANIZATION_FACULTY_CREATE_OR_UPDATE)).execute(cleanFaculty);
//        verify(persistenceFacade).save(cleanFaculty);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDeleteFacultyById() throws FacultyNotExistsException, FacultyIsNotEmptyException {
//        Faculty cleanFaculty = makeCleanFaculty(0);
//        if (cleanFaculty instanceof FakeFaculty f) {
//            f.setCourses(List.of());
//        }
//        Long id = getPersistent(cleanFaculty).getId();
//        assertThat(database.findFacultyById(id)).isPresent();
//
//        facade.deleteFacultyById(id);
//
//        assertThat(database.findFacultyById(id)).isEmpty();
//        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
//        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
//        verify(persistenceFacade).findFacultyById(id);
//        verify(persistenceFacade).deleteFaculty(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNoDeleteFacultyById_FacultyNotExists() throws FacultyNotExistsException, FacultyIsNotEmptyException {
//        Long id = 403L;
//
//        FacultyNotExistsException thrown = assertThrows(FacultyNotExistsException.class, () -> facade.deleteFacultyById(id));
//
//        assertEquals("Faculty with ID:403 is not exists.", thrown.getMessage());
//        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
//        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
//        verify(persistenceFacade).findFacultyById(id);
//        verify(persistenceFacade, never()).deleteFaculty(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNoDeleteFacultyById_FacultyNotEmpty() throws FacultyNotExistsException, FacultyIsNotEmptyException {
//        Faculty cleanFaculty = makeCleanFaculty(0);
//        Long id = getPersistent(cleanFaculty).getId();
//        assertThat(database.findFacultyById(id)).isPresent();
//
//        FacultyIsNotEmptyException thrown = assertThrows(FacultyIsNotEmptyException.class, () -> facade.deleteFacultyById(id));
//
//        assertEquals("Faculty with ID:" + id + " has courses.", thrown.getMessage());
//        verify(factory).command(ORGANIZATION_FACULTY_DELETE);
//        verify(factory.command(ORGANIZATION_FACULTY_DELETE)).execute(id);
//        verify(persistenceFacade).findFacultyById(id);
//        verify(persistenceFacade, never()).deleteFaculty(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotFindAllStudentsGroup() {
//        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();
//
//        assertThat(groups).isEmpty();
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).execute(null);
//        verify(persistenceFacade).findAllStudentsGroups();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldFindAllStudentsGroup() {
//        StudentsGroup group = makeCleanStudentsGroup(0);
//        getPersistent(group);
//
//        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();
//
//        assertThat(groups).hasSize(1);
//        assertStudentsGroupEquals(group, groups.iterator().next(), false);
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_ALL)).execute(null);
//        verify(persistenceFacade).findAllStudentsGroups();
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotGetStudentsGroupById() {
//        Long id = 500L;
//
//        Optional<StudentsGroup> studentsGroup = facade.getStudentsGroupById(id);
//
//        assertThat(studentsGroup).isEmpty();
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).execute(id);
//        verify(persistenceFacade).findStudentsGroupById(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldGetStudentsGroupById() {
//        StudentsGroup group = makeCleanStudentsGroup(0);
//        Long id = getPersistent(group).getId();
//
//        Optional<StudentsGroup> studentsGroup = facade.getStudentsGroupById(id);
//
//        assertThat(studentsGroup).isPresent();
//        assertStudentsGroupEquals(group, studentsGroup.get(), false);
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID)).execute(id);
//        verify(persistenceFacade).findStudentsGroupById(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldCreateOrUpdateStudentsGroup_Concrete() {
//        Optional<StudentsGroup> studentsGroup = facade.createOrUpdateStudentsGroup(mockGroup);
//
//        assertThat(studentsGroup).isPresent();
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).execute(mockGroup);
//        verify(persistenceFacade).save(mockGroup);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldCreateOrUpdateStudentsGroup_Mock() {
//        StudentsGroup group = makeCleanStudentsGroup(0);
//
//        Optional<StudentsGroup> studentsGroup = facade.createOrUpdateStudentsGroup(group);
//
//        assertThat(studentsGroup).isPresent();
//        assertStudentsGroupEquals(group, studentsGroup.get(), false);
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE)).execute(group);
//        verify(persistenceFacade).save(group);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldDeleteStudentsGroupById() throws StudentGroupWithStudentsException, NotExistStudentsGroupException {
//        StudentsGroup group = makeCleanStudentsGroup(0);
//        if (group instanceof FakeStudentsGroup gr) {
//            gr.setStudents(List.of());
//        }
//        Long id = getPersistent(group).getId();
//        assertThat(database.findStudentsGroupById(id)).isPresent();
//
//        facade.deleteStudentsGroupById(id);
//
//        assertThat(database.findStudentsGroupById(id)).isEmpty();
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
//        verify(persistenceFacade).findStudentsGroupById(id);
//        verify(persistenceFacade).deleteStudentsGroup(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeleteStudentsGroupById_GroupNotExists() throws StudentGroupWithStudentsException, NotExistStudentsGroupException {
//        Long id = 503L;
//
//        NotExistStudentsGroupException thrown =
//                assertThrows(NotExistStudentsGroupException.class, () -> facade.deleteStudentsGroupById(id));
//
//        assertEquals("StudentsGroup with ID:503 is not exists.", thrown.getMessage());
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
//        verify(persistenceFacade).findStudentsGroupById(id);
//        verify(persistenceFacade, never()).deleteStudentsGroup(id);
//    }
//
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, NotExistStudentsGroupException {
//        Long id = getPersistent(makeCleanStudentsGroup(0)).getId();
//        assertThat(database.findStudentsGroupById(id)).isPresent();
//
//        StudentGroupWithStudentsException thrown =
//                assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));
//
//        assertEquals("StudentsGroup with ID:" + id + " has students.", thrown.getMessage());
//        verify(factory).command(ORGANIZATION_STUDENTS_GROUP_DELETE);
//        verify(factory.command(ORGANIZATION_STUDENTS_GROUP_DELETE)).execute(id);
//        verify(persistenceFacade).findStudentsGroupById(id);
//        verify(persistenceFacade, never()).deleteStudentsGroup(id);
//    }
//
//    private AuthorityPerson getPersistent(AuthorityPerson newInstance) {
//        Optional<AuthorityPerson> saved = database.save(newInstance);
//        assertThat(saved).isNotEmpty();
//        return saved.get();
//    }
//
//    private Faculty getPersistent(Faculty newInstance) {
//        Optional<Faculty> saved = database.save(newInstance);
//        assertThat(saved).isNotEmpty();
//        return saved.get();
//    }
//
//    private StudentsGroup getPersistent(StudentsGroup newInstance) {
//        Optional<StudentsGroup> saved = database.save(newInstance);
//        assertThat(saved).isNotEmpty();
//        return saved.get();
//    }
//
    private CommandsFactory<?> buildFactory(PersistenceFacade persistenceFacade) {
        return mock(OrganizationCommandsFactory.class);
//        return new OrganizationCommandsFactory(
//                Set.of(
//                        spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade)),
//                        spy(new CreateOrUpdateFacultyCommand(persistenceFacade)),
//                        spy(new CreateOrUpdateStudentsGroupCommand(persistenceFacade)),
//                        spy(new DeleteAuthorityPersonCommand(persistenceFacade)),
//                        spy(new DeleteFacultyCommand(persistenceFacade)),
//                        spy(new DeleteStudentsGroupCommand(persistenceFacade)),
//                        spy(new FindAllAuthorityPersonsCommand(persistenceFacade)),
//                        spy(new FindAllFacultiesCommand(persistenceFacade)),
//                        spy(new FindAllStudentsGroupsCommand(persistenceFacade)),
//                        spy(new FindAuthorityPersonCommand(persistenceFacade)),
//                        spy(new FindFacultyCommand(persistenceFacade)),
//                        spy(new FindStudentsGroupCommand(persistenceFacade))
//                )
//        );
    }
}