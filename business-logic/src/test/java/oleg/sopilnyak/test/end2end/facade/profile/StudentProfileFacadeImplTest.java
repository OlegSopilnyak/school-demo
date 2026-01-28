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
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.FindStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.JsonContextModule;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@SuppressWarnings("unchecked")
class StudentProfileFacadeImplTest extends MysqlTestModelFactory {
    private static final String FIND_BY_ID = "profile.student.findById";
    private static final String CREATE_OR_UPDATE = "profile.student.createOrUpdate";
    private static final String DELETE_BY_ID = "profile.student.deleteById";

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

    CommandsFactory<StudentProfileCommand<?>> factory;
    StudentProfileFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistence));
        facade = spy(new StudentProfileFacadeImpl(factory, payloadMapper, actionExecutor));
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
        String commandId = FIND_BY_ID;
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        Optional<StudentProfile> entity = facade.doActionAndResult(commandId, id);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profile, true);
        verifyAfterCommand(commandId, Input.of(id));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_ProfileNotExist() {
        String commandId = FIND_BY_ID;
        Long id = 710L;

        Optional<StudentProfile> entity = facade.doActionAndResult(commandId, id);

        assertThat(entity).isEmpty();
        verifyAfterCommand(commandId, Input.of(id));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_WrongProfileType() {
        String commandId = FIND_BY_ID;
        Long id = persistPrincipal().getId();

        Optional<StudentProfile> entity = facade.doActionAndResult(commandId, id);

        assertThat(entity).isEmpty();
        verifyAfterCommand(commandId, Input.of(id));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile_Create() {
        String commandId = CREATE_OR_UPDATE;
        StudentProfile profileSource = makeStudentProfile(null);

        Optional<StudentProfile> entity = facade.doActionAndResult(commandId, profileSource);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profileSource, false);
        verifyAfterCommand(commandId, Input.of(profileSource));
        verify(persistence).save(any(StudentProfilePayload.class));
        verify(persistence).saveProfile(any(StudentProfilePayload.class));
    }

    @Test
    void shouldCreateOrUpdateProfile_Update() {
        String commandId = CREATE_OR_UPDATE;
        StudentProfile profile = payloadMapper.toPayload(persistStudent());
        Long id = profile.getId();

        Optional<StudentProfile> entity = facade.doActionAndResult(commandId, profile);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.orElseThrow(), profile, true);
        verifyAfterCommand(commandId, Input.of(profile));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(any(StudentProfileEntity.class));
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotCreateOrUpdateProfile_Create() {
        String commandId = CREATE_OR_UPDATE;
        Long id = 711L;
        StudentProfile profileSource = payloadMapper.toPayload(makeStudentProfile(id));

        var thrown = assertThrows(UnableExecuteCommandException.class,
                () -> facade.doActionAndResult(commandId, profileSource)
        );

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(commandId);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(ProfileNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verifyAfterCommand(commandId, Input.of(profileSource));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(StudentProfile.class));
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    void shouldDeleteProfileById_ProfileExists() {
        String commandId = DELETE_BY_ID;
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        Object result = facade.doActionAndResult(commandId, id);

        assertThat(result).isNull();
        verifyAfterCommand(commandId, Input.of(id));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
    }

    @Test
    void shouldDeleteProfile_ProfileExists() {
        String commandId = DELETE_BY_ID;
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        Object result = facade.doActionAndResult(commandId, id);

        assertThat(result).isNull();
        verifyAfterCommand(commandId, Input.of(id));
        verify(persistence).deleteProfileById(id);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
    }

    @Test
    void shouldNotDeleteProfile_ProfileNotExists() {
        String commandId = DELETE_BY_ID;
        Long id = 715L;
        StudentProfile profile = makeStudentProfile(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class,
                () -> facade.doActionAndResult(commandId, profile)
        );

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:715 is not exists.");
        verifyAfterCommand(commandId, Input.of(id));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        String commandId = DELETE_BY_ID;
        Long id = 703L;

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class,
                () -> facade.doActionAndResult(commandId, id)
        );

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:703 is not exists.");
        verifyAfterCommand(commandId, Input.of(id));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        StudentProfile profile = makeStudentProfile(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class,
                () -> facade.doActionAndResult(DELETE_BY_ID, profile)
        );

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(farm, never()).command(anyString());
        verify(factory, never()).command(anyString());
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        StudentProfile profile = makeStudentProfile(null);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class,
                () -> facade.doActionAndResult(DELETE_BY_ID, profile)
        );

        assertThat(exception.getMessage()).startsWith("Wrong ");
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

    private CommandsFactory<StudentProfileCommand<?>> buildFactory(ProfilePersistenceFacade persistence) {
        Map<StudentProfileCommand<?>, String> commands = Map.of(
                spy(new CreateOrUpdateStudentProfileCommand(persistence, payloadMapper)), "profileStudentUpdate",
                spy(new FindStudentProfileCommand(persistence, payloadMapper)), "profileStudentFind",
                spy(new DeleteStudentProfileCommand(persistence, payloadMapper)), "profileStudentDelete"
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
            StudentProfileCommand<?> transactionalCommand = (StudentProfileCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        return new StudentProfileCommandsFactory(commands.keySet());
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