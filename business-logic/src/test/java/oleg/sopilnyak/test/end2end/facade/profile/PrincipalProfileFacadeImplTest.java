package oleg.sopilnyak.test.end2end.facade.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.FindPrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.JsonContextModule;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
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
@ContextConfiguration(classes = {
        SchoolCommandsConfiguration.class,
        PersistenceConfiguration.class,
        TestConfig.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
class PrincipalProfileFacadeImplTest extends MysqlTestModelFactory {
    private static final String FIND_BY_ID = "school::person::profile::principal:find.By.Id";
    private static final String CREATE_OR_UPDATE = "school::person::profile::principal:create.Or.Update";
    private static final String DELETE_BY_ID = "school::person::profile::principal:delete.By.Id";

    @MockitoSpyBean
    @Autowired
    CommandActionExecutor actionExecutor;
    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    ApplicationContext applicationContext;

    @MockitoSpyBean
    @Autowired
    ProfilePersistenceFacade persistence;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    CommandsFactoriesFarm farm;

    CommandsFactory<PrincipalProfileCommand<?>> factory;
    PrincipalProfileFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistence));
        facade = spy(new PrincipalProfileFacadeImpl(factory, payloadMapper, actionExecutor));
        farm.register(factory);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()).registerModule(new JsonContextModule<>(applicationContext, farm))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        actionExecutor.shutdown();
        ReflectionTestUtils.setField(actionExecutor, "objectMapper", objectMapper);
        actionExecutor.initialize();
        ActionContext.setup("test-facade", "test-action");
    }

    @AfterEach
    void tearDown() {
        deleteEntities(StudentProfileEntity.class);
        deleteEntities(PrincipalProfileEntity.class);
    }

    @Test
    void shouldAllPartsBeReady() {
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(actionExecutor).isNotNull();
        assertThat(farm).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    void shouldFindProfileById_ProfileExists() {
        PrincipalProfile profile = persistPrincipal();
        Long id = profile.getId();

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).isPresent();
        assertProfilesEquals(result.orElseThrow(), profile, true);
        verifyAfterCommand(FIND_BY_ID, Input.of(id));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_ProfileNotExist() {
        Long id = 610L;

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).isEmpty();
        verifyAfterCommand(FIND_BY_ID, Input.of(id));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_WrongProfileType() {
        Long id = persistStudent().getId();

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalFindById", id);

        assertThat(result).isEmpty();
        verifyAfterCommand(FIND_BY_ID, Input.of(id));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile_Create() {
        PrincipalProfile profileSource = payloadMapper.toPayload(makePrincipalProfile(null));

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", profileSource);

        assertThat(result).isPresent();
        assertProfilesEquals(result.orElseThrow(), profileSource, false);
        verifyAfterCommand(CREATE_OR_UPDATE, Input.of(profileSource));
        verify(persistence).save(profileSource);
        verify(persistence).saveProfile(profileSource);
    }

    @Test
    void shouldCreateOrUpdateProfile_Update() {
        PrincipalProfile profile = payloadMapper.toPayload(persistPrincipal());
        Long id = profile.getId();

        Optional<PrincipalProfile> result = ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", profile);

        assertThat(result).isPresent();
        assertProfilesEquals(result.orElseThrow(), profile, false);
        verifyAfterCommand(CREATE_OR_UPDATE, Input.of(profile));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(any(PrincipalProfileEntity.class));
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotCreateOrUpdateProfile_Create() {
        String commandId = CREATE_OR_UPDATE;
        Long id = 611L;
        PrincipalProfile profileSource = payloadMapper.toPayload(makePrincipalProfile(id));

        var thrown = assertThrows(UnableExecuteCommandException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalCreateOrUpdate", profileSource)
        );

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(commandId);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(ProfileNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verifyAfterCommand(commandId, Input.of(profileSource));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(PrincipalProfile.class));
    }

    @Test
    void shouldDeleteProfileById_ProfileExists() {
        PrincipalProfile profile = persistPrincipal();
        Long id = profile.getId();

        ReflectionTestUtils.invokeMethod(facade, "internalDelete", id);

        verifyAfterCommand(DELETE_BY_ID, Input.of(id));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
        assertThat(findEntity(PrincipalProfileEntity.class, id)).isNull();
    }

    @Test
    void shouldDeleteProfile_ProfileExists() {
        PrincipalProfile profile = persistPrincipal();
        Long id = profile.getId();

        ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile);

        verifyAfterCommand(DELETE_BY_ID, Input.of(id));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findPrincipalProfileById(id)).isEmpty();
    }

    @Test
    void shouldNotDeleteProfile_ProfileNotExists() throws ProfileNotFoundException {
        Long id = 615L;
        PrincipalProfile profile = makePrincipalProfile(id);

        var exception = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile)
        );

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:615 is not exists.");
        verifyAfterCommand(DELETE_BY_ID, Input.of(id));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(PrincipalProfile.class));
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 603L;

        var thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", id)
        );

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:603 is not exists.");
        verifyAfterCommand(DELETE_BY_ID, Input.of(id));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(PrincipalProfile.class));
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        PrincipalProfile profile = makePrincipalProfile(id);

        var thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile)
        );

        assertThat(thrown.getMessage()).startsWith("Wrong ");
        verify(farm, never()).command(anyString());
        verify(factory, never()).command(anyString());
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        PrincipalProfile profile = makePrincipalProfile(null);

        var thrown = assertThrows(ProfileNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(facade, "internalDelete", profile)
        );

        assertThat(thrown.getMessage()).startsWith("Wrong ");
        verify(farm, never()).command(anyString());
        verify(factory, never()).command(anyString());
    }

    // private methods
    private void verifyAfterCommand(String commandId, Input<?> commandInput) {
        verify(farm, times(2)).command(commandId);
        verify(factory, times(3)).command(commandId);
        verify(factory.command(commandId)).createContext(commandInput);
        verify(factory.command(commandId)).doCommand(any(Context.class));
    }

    private CommandsFactory<PrincipalProfileCommand<?>> buildFactory(ProfilePersistenceFacade persistence) {
        Map<PrincipalProfileCommand<?>, String> commands = Map.of(
                spy(new CreateOrUpdatePrincipalProfileCommand(persistence, payloadMapper)), "profilePrincipalUpdate",
                spy(new FindPrincipalProfileCommand(persistence, payloadMapper)), "profilePrincipalFind",
                spy(new DeletePrincipalProfileCommand(persistence, payloadMapper)), "profilePrincipalDelete"
        );
        String acName = "applicationContext";
        commands.forEach((command, beanName) -> {
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            PrincipalProfileCommand<?> transactionalCommand = (PrincipalProfileCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        return new PrincipalProfileCommandsFactory(commands.keySet());
    }

    private StudentProfile persist(StudentProfile newInstance) {
        StudentProfileEntity entity = entityMapper.toEntity(newInstance);
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

    private StudentProfilePayload persistStudent() {
        try {
            return payloadMapper.toPayload(persist(makeStudentProfile(null)));
        } finally {
            reset(payloadMapper);
        }
    }

    private PrincipalProfilePayload persistPrincipal() {
        try {
            return payloadMapper.toPayload(persist(makePrincipalProfile(null)));
        } finally {
            reset(payloadMapper);
        }
    }
}