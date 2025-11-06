package oleg.sopilnyak.test.end2end.facade.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.CreateOrUpdateAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.DeleteAuthorityPersonMacroCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.LoginAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.organization.authority.LogoutAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class AuthorityPersonFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGIN = "organization.authority.person.login";
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGOUT = "organization.authority.person.logout";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "organization.authority.person.findAll";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "organization.authority.person.findById";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW = "organization.authority.person.create.macro";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL = "organization.authority.person.delete.macro";

    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    ApplicationContext applicationContext;

    @SpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @SpyBean
    @Autowired
    SchedulingTaskExecutor schedulingTaskExecutor;
    @Autowired
    PersistenceFacade database;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @SpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;

    CommandsFactory<AuthorityPersonCommand<?>> factory;
    AuthorityPersonFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistence));
        facade = spy(new AuthorityPersonFacadeImpl(factory, payloadMapper));
        ActionContext.setup("test-facade", "test-action");
    }

    @AfterEach
    void tearDown() {
        deleteEntities(FacultyEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(database).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    void shouldLogoutAuthorityPerson() {
        String token = "logged_in_person_token";

        facade.logout(token);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT)).createContext(Input.of(token));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGOUT)).doCommand(any(Context.class));
    }

    @Test
    void shouldLoginAuthorityPerson() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload person = createAuthorityPerson();
        assertThat(findAuthorityPersonById(person.getId())).isEqualTo(person.getOriginal());
        assertThat(findAuthorityPersonByProfileId(person.getProfileId())).isEqualTo(person.getOriginal());
        setPersonPermissions(person, username, password);

        Optional<AuthorityPerson> loggedIn = facade.login(username, password);

        assertThat(loggedIn).isPresent().contains(person);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGIN);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).createContext(Input.of(username, password));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(person.getProfileId());
    }

    @Test
    void shouldNotLoginAuthorityPerson_WrongPassword() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload person = createAuthorityPerson();
        assertThat(findAuthorityPersonById(person.getId())).isEqualTo(person.getOriginal());
        assertThat(findAuthorityPersonByProfileId(person.getProfileId())).isEqualTo(person.getOriginal());
        setPersonPermissions(person, username, password);

        Exception thrown = assertThrows(SchoolAccessDeniedException.class, () -> facade.login(username, "password"));

        assertThat(thrown).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(thrown.getMessage()).isEqualTo("Login authority person command failed for username:" + username);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_LOGIN);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).createContext(Input.of(username, "password"));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_LOGIN)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(person.getProfileId());
    }

    @Test
    void shouldFindAllAuthorityPersons_Empty() {

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldFindAllAuthorityPersons_ExistsInstance() {
        AuthorityPerson person = persistAuthorityPerson();
        assertThat(database.findAuthorityPersonById(person.getId())).contains(person);

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).contains(payloadMapper.toPayload(person));
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).createContext(null);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL)).doCommand(any(Context.class));
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldNotFindAuthorityPersonById_NotFound() {
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldFindAuthorityPersonById() {
        AuthorityPerson authorityPerson = createAuthorityPerson();
        Long id = authorityPerson.getId();

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isPresent().contains(authorityPerson);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Create() {
        AuthorityPerson authorityPerson = payloadMapper.toPayload(makeCleanAuthorityPerson(2));

        Optional<AuthorityPerson> person = facade.create(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), false);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).createContext(Input.of(authorityPerson));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW)).doCommand(any(Context.class));
        verify(persistence).save(authorityPerson);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Update() {
        AuthorityPerson authorityPerson = createAuthorityPerson();

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), true);
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(Input.of(authorityPerson));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(authorityPerson);
    }

    @Test
    void shouldNotCreateOrUpdateFaculty() {
        Long id = 301L;
        AuthorityPersonPayload authorityPersonSource = payloadMapper.toPayload(makeCleanAuthorityPerson(2));
        authorityPersonSource.setId(id);
        reset(payloadMapper);


        UnableExecuteCommandException thrown = assertThrows(
                UnableExecuteCommandException.class,
                () -> facade.createOrUpdateAuthorityPerson(authorityPersonSource)
        );

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(AuthorityPersonNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).createContext(Input.of(authorityPersonSource));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        Long id = createAuthorityPerson().getId();

        facade.deleteAuthorityPersonById(id);

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistence, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() {
        Long id = 303L;

        AuthorityPersonNotFoundException thrown =
                assertThrows(AuthorityPersonNotFoundException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).isEqualTo("AuthorityPerson with ID:303 is not exists.");

        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        AuthorityPerson authorityPersonSource = makeCleanAuthorityPerson(2);
        if (authorityPersonSource instanceof FakeAuthorityPerson person) {
            person.setFaculties(List.of(makeCleanFacultyNoDean(2)));
        }
        Optional<AuthorityPerson> authorityPerson = facade.create(authorityPersonSource);
        assertThat(authorityPerson).isPresent();
        Long id = authorityPerson.orElseThrow().getId();

        AuthorityPersonManagesFacultyException thrown =
                assertThrows(AuthorityPersonManagesFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is managing faculties.");
        verify(factory).command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL);
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).createContext(Input.of(id));
        verify(factory.command(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL)).doCommand(any(Context.class));
        verify(persistence, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    // private methods
    private AuthorityPerson findAuthorityPersonByProfileId(Long profileId) {
        PrincipalProfileEntity profile = findEntity(PrincipalProfileEntity.class, profileId, e -> e.getExtras().size());
        assertThat(profile).isNotNull();
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<AuthorityPersonEntity> criteriaQuery = criteriaBuilder.createQuery(AuthorityPersonEntity.class);
            Root<AuthorityPersonEntity> entityRoot = criteriaQuery.from(AuthorityPersonEntity.class);
            criteriaQuery.select(entityRoot);
            criteriaQuery.where(criteriaBuilder.equal(entityRoot.get("profileId"), profileId));
            TypedQuery<AuthorityPersonEntity> query = em.createQuery(criteriaQuery);
            List<AuthorityPersonEntity> employees = query.getResultList();
            AuthorityPersonEntity person = ObjectUtils.isEmpty(employees) ? null : employees.get(0);
            if (person != null) {
                person.getFacultyEntitySet().size();
            }
            return person;
        } finally {
            em.close();
        }
    }

    private AuthorityPerson findAuthorityPersonById(Long id) {
        return findEntity(AuthorityPersonEntity.class, id, e -> e.getFacultyEntitySet().size());
    }

    private CommandsFactory<AuthorityPersonCommand<?>> buildFactory(PersistenceFacade persistenceFacade) {
        CreateOrUpdateAuthorityPersonCommand createOrUpdateAuthorityPersonCommand =
                spy(new CreateOrUpdateAuthorityPersonCommand(persistenceFacade, payloadMapper));
        CreateOrUpdatePrincipalProfileCommand createOrUpdatePrincipalProfileCommand =
                spy(new CreateOrUpdatePrincipalProfileCommand(persistenceFacade, payloadMapper));
        CreateAuthorityPersonMacroCommand createAuthorityPersonMacroCommand =
                spy(new CreateAuthorityPersonMacroCommand(
                        createOrUpdateAuthorityPersonCommand, createOrUpdatePrincipalProfileCommand, payloadMapper, actionExecutor
                ));
        DeleteAuthorityPersonCommand deleteAuthorityPersonCommand =
                spy(new DeleteAuthorityPersonCommand(persistenceFacade, payloadMapper));
        DeletePrincipalProfileCommand deletePrincipalProfileCommand =
                spy(new DeletePrincipalProfileCommand(persistenceFacade, payloadMapper));
        DeleteAuthorityPersonMacroCommand deleteAuthorityPersonMacroCommand =
                spy(new DeleteAuthorityPersonMacroCommand(
                        deleteAuthorityPersonCommand, deletePrincipalProfileCommand, schedulingTaskExecutor, persistenceFacade, actionExecutor
                ));

        Map<PrincipalProfileCommand<?>, String> profiles = Map.of(
                createOrUpdatePrincipalProfileCommand, "profilePrincipalUpdate",
                deletePrincipalProfileCommand, "profilePrincipalDelete"
        );
        Map<AuthorityPersonCommand<?>, String> commands = Map.of(
                spy(new LoginAuthorityPersonCommand(persistenceFacade, payloadMapper)), "authorityPersonLogin",
                spy(new LogoutAuthorityPersonCommand()), "authorityPersonLogout",
                createOrUpdateAuthorityPersonCommand, "authorityPersonUpdate",
                createAuthorityPersonMacroCommand, "authorityPersonMacroCreate",
                deleteAuthorityPersonCommand, "authorityPersonDelete",
                deleteAuthorityPersonMacroCommand, "authorityPersonMacroDelete",
                spy(new FindAllAuthorityPersonsCommand(persistenceFacade, payloadMapper)), "authorityPersonFindAll",
                spy(new FindAuthorityPersonCommand(persistenceFacade, payloadMapper)), "authorityPersonFind"
        );
        String acName = "applicationContext";
        profiles.entrySet().forEach(entry -> {
            PrincipalProfileCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            PrincipalProfileCommand<?> transactionalCommand = (PrincipalProfileCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        commands.entrySet().forEach(entry -> {
            AuthorityPersonCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            AuthorityPersonCommand<?> transactionalCommand = (AuthorityPersonCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        return new AuthorityPersonCommandsFactory(commands.keySet());
    }

    private AuthorityPerson persistAuthorityPerson() {
        AuthorityPerson authorityPerson = makeCleanAuthorityPerson(1);
        AuthorityPerson entity = database.save(authorityPerson).orElse(null);
        assertThat(entity).isNotNull();
        Optional<AuthorityPerson> dbAuthorityPerson = database.findAuthorityPersonById(entity.getId());
        assertAuthorityPersonEquals(dbAuthorityPerson.orElseThrow(), authorityPerson, false);
        assertThat(dbAuthorityPerson).contains(entity);
        return entity;
    }

    private void setPersonPermissions(AuthorityPersonPayload person, String username, String password) {
        PrincipalProfileEntity profile = findEntity(PrincipalProfileEntity.class, person.getProfileId());
        profile.setLogin(username);
        try {
            profile.setSignature(profile.makeSignatureFor(password));
            merge(profile);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
        } finally {
            reset(persistence);
        }
    }

    private AuthorityPersonPayload createAuthorityPerson() {
        try {
            PrincipalProfile profile = makePrincipalProfile(null);
            PrincipalProfile profileEntity = persist(profile);
            AuthorityPerson person = makeCleanAuthorityPerson(0);
            if (person instanceof FakeAuthorityPerson fake) {
                fake.setProfileId(profileEntity.getId());
            }
            return payloadMapper.toPayload(persist(person));
        } finally {
            reset(payloadMapper);
        }
    }

    private void merge(PrincipalProfile instance) {
        PrincipalProfileEntity entity = instance instanceof PrincipalProfileEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private AuthorityPerson persist(AuthorityPerson newInstance) {
        AuthorityPersonEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
        }
    }

    private PrincipalProfile persist(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
        }
    }
}