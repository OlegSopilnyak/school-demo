package oleg.sopilnyak.test.end2end.facade.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
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
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.JsonContextModule;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.AuthorityPersonFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
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

    @MockitoSpyBean
    @Autowired
    CommandActionExecutor actionExecutor;
    @Autowired
    @Qualifier("parallelCommandNestedCommandsExecutor")
    Executor schedulingTaskExecutor;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    CommandsFactoriesFarm farm;

    CommandsFactory<AuthorityPersonCommand<?>> factory;
    AuthorityPersonFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistence));
        facade = spy(new AuthorityPersonFacadeImpl(factory, payloadMapper, actionExecutor));
        farm.register(factory);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()).registerModule(new JsonContextModule<>(applicationContext, farm))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        actionExecutor.shutdown();
        ReflectionTestUtils.setField(actionExecutor, "objectMapper", objectMapper);
        actionExecutor.initialize();
        ActionContext.setup("test-facade", "test-doingMainLoop");
    }

    @AfterEach
    void tearDown() {
        deleteEntities(FacultyEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(emf).isNotNull();
        assertThat(applicationContext).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(actionExecutor).isNotNull();
        assertThat(farm).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    void shouldLogoutAuthorityPerson() {
        String token = "logged_in_person_token";

        facade.logout(token);

        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_LOGOUT, Input.of(token));
    }

    @Test
    void shouldLoginAuthorityPerson() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload person = createPerson();
        assertThat(findAuthorityPersonById(person.getId())).isEqualTo(person.getOriginal());
        assertThat(findAuthorityPersonByProfileId(person.getProfileId())).isEqualTo(person.getOriginal());
        setPersonPermissions(person, username, password);

        Optional<AuthorityPerson> loggedIn = facade.login(username, password);

        assertThat(loggedIn).isPresent().contains(person);
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_LOGIN, Input.of(username, password));
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(person.getProfileId());
    }

    @Test
    void shouldNotLoginAuthorityPerson_WrongPassword() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload person = createPerson();
        assertThat(findAuthorityPersonById(person.getId())).isEqualTo(person.getOriginal());
        assertThat(findAuthorityPersonByProfileId(person.getProfileId())).isEqualTo(person.getOriginal());
        setPersonPermissions(person, username, password);

        var thrown = assertThrows(SchoolAccessDeniedException.class, () -> facade.login(username, "password"));

        assertThat(thrown).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(thrown.getMessage()).isEqualTo("Login authority person command failed for username:" + username);

        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_LOGIN, Input.of(username, "password"));
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(person.getProfileId());
    }

    @Test
    void shouldFindAllAuthorityPersons_Empty() {

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).isEmpty();
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL, Input.empty());
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldFindAllAuthorityPersons_ExistsInstance() {
        AuthorityPersonPayload person = createPerson();
        assertThat(findAuthorityPersonById(person.getId())).isEqualTo(person.getOriginal());

        Collection<AuthorityPerson> persons = facade.findAllAuthorityPersons();

        assertThat(persons).contains(payloadMapper.toPayload(person));
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_FIND_ALL, Input.empty());
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldNotFindAuthorityPersonById_NotFound() {
        Long id = 300L;

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isEmpty();
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID, Input.of(id));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldFindAuthorityPersonById() {
        AuthorityPerson authorityPerson = createPerson();
        Long id = authorityPerson.getId();

        Optional<AuthorityPerson> person = facade.findAuthorityPersonById(id);

        assertThat(person).isPresent().contains(authorityPerson);
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID, Input.of(id));
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Create() {
        AuthorityPerson authorityPerson = payloadMapper.toPayload(makeCleanAuthorityPerson(2));

        Optional<AuthorityPerson> person = facade.create(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), false);
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW, Input.of(authorityPerson));
        ArgumentCaptor<AuthorityPerson> captor = ArgumentCaptor.forClass(AuthorityPerson.class);
        verify(persistence).save(captor.capture());
        assertAuthorityPersonEquals(captor.getValue(), authorityPerson);
    }

    @Test
    void shouldCreateOrUpdateAuthorityPerson_Update() {
        AuthorityPerson authorityPerson = createPerson();

        Optional<AuthorityPerson> person = facade.createOrUpdateAuthorityPerson(authorityPerson);

        assertThat(person).isPresent();
        assertAuthorityPersonEquals(authorityPerson, person.get(), true);
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE, Input.of(authorityPerson));
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
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE, Input.of(authorityPersonSource));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).save(any(AuthorityPerson.class));
    }

    @Test
    void shouldDeleteAuthorityPersonById() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        Long id = createPerson().getId();

        facade.deleteAuthorityPersonById(id);

        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL, Input.of(id));
        verify(persistence, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonNotExists() {
        Long id = 303L;

        var thrown = assertThrows(AuthorityPersonNotFoundException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).isEqualTo("AuthorityPerson with ID:303 is not exists.");

        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL, Input.of(id));
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDeleteAuthorityPersonById_PersonManageFaculty() throws AuthorityPersonManagesFacultyException, AuthorityPersonNotFoundException {
        AuthorityPerson authorityPersonSource = makeCleanAuthorityPerson(2);
        if (authorityPersonSource instanceof FakeAuthorityPerson person) {
            person.setFaculties(List.of(makeCleanFacultyNoDean(2)));
        }
        AuthorityPerson authorityPerson = createPerson(authorityPersonSource);
        Long id = authorityPerson.getId();

        var thrown = assertThrows(AuthorityPersonManagesFacultyException.class, () -> facade.deleteAuthorityPersonById(id));

        assertThat(thrown.getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is managing faculties.");
        verifyAfterCommand(ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL, Input.of(id));
        verify(persistence, atLeastOnce()).findAuthorityPersonById(id);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    // private methods
    private void verifyAfterCommand(String commandId, Input<?> commandInput) {
        verify(farm, times(2)).command(commandId);
        verify(factory, times(3)).command(commandId);
        verify(factory.command(commandId)).createContext(commandInput);
        verify(factory.command(commandId)).doCommand(any(Context.class));
    }

    private AuthorityPerson findAuthorityPersonByProfileId(Long profileId) {
        String fkAttributeName = "profileId";
        return findEntityByFK(AuthorityPersonEntity.class, fkAttributeName, profileId, e -> e.getFacultyEntitySet().size());
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
        //
        // configure commands for StudentProfileCommand family
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
        // register in the farm command-profiles factory
        farm.register(new PrincipalProfileCommandsFactory(profiles.keySet()));
        //
        // configure commands for StudentCommand family
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

    private AuthorityPersonPayload createPerson() {
        return createPerson(makeCleanAuthorityPerson(0));
    }

    private AuthorityPersonPayload createPerson(AuthorityPerson person) {
        try {
            PrincipalProfile profile = persist(makePrincipalProfile(null));
            if (person instanceof FakeAuthorityPerson fake) {
                fake.setProfileId(profile.getId());
            }
            return payloadMapper.toPayload(persist(person));
        } finally {
            reset(payloadMapper);
        }
    }

    private void merge(PrincipalProfile instance) {
        PrincipalProfileEntity entity = instance instanceof PrincipalProfileEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }
    }

    private AuthorityPerson persist(AuthorityPerson newInstance) {
        AuthorityPersonEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private PrincipalProfile persist(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }
}