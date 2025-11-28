package oleg.sopilnyak.test.end2end.facade.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.FindStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
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

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class StudentProfileFacadeImplTest extends MysqlTestModelFactory {
    private static final String PROFILE_FIND_BY_ID = "profile.student.findById";
    private static final String PROFILE_CREATE_OR_UPDATE = "profile.student.createOrUpdate";
    private static final String PROFILE_DELETE = "profile.student.deleteById";

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

    CommandsFactory<StudentProfileCommand<?>> factory;
    StudentProfileFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistence));
        facade = spy(new StudentProfileFacadeImpl(factory, payloadMapper));
    }

    @Test
    void shouldAllPartsBeReady() {
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    void shouldFindProfileById_ProfileExists() {
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        Optional<StudentProfile> entity = facade.findStudentProfileById(id);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profile, true);
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_ProfileNotExist() {
        Long id = 710L;

        Optional<StudentProfile> faculty = facade.findStudentProfileById(id);

        assertThat(faculty).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_WrongProfileType() {
        Long id = persistPrincipal().getId();

        Optional<StudentProfile> profile = facade.findStudentProfileById(id);

        assertThat(profile).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile_Create() {
        StudentProfile profileSource = makeStudentProfile(null);

        Optional<StudentProfile> entity = facade.createOrUpdateProfile(profileSource);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profileSource, false);
        verify(facade).createOrUpdate(profileSource);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(any(Input.class));
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(any(StudentProfilePayload.class));
        verify(persistence).saveProfile(any(StudentProfilePayload.class));
    }

    @Test
    void shouldCreateOrUpdateProfile_Update() {
        StudentProfile profile = payloadMapper.toPayload(persistStudent());
        Long id = profile.getId();

        Optional<StudentProfile> entity = facade.createOrUpdateProfile(profile);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.orElseThrow(), profile, true);
        verify(facade).createOrUpdate(profile);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(Input.of(profile));
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(any(StudentProfileEntity.class));
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotCreateOrUpdateProfile_Create() {
        Long id = 711L;
        StudentProfile profileSource = payloadMapper.toPayload(makeStudentProfile(id));

        var thrown = assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateProfile(profileSource));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(PROFILE_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(ProfileNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(Input.of(profileSource));
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(StudentProfile.class));
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    void shouldDeleteProfileById_ProfileExists() {
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        facade.deleteById(id);

        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
    }

    @Test
    void shouldDeleteProfile_ProfileExists() {
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        facade.delete(profile);

        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).deleteProfileById(id);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
    }

    @Test
    void shouldNotDeleteProfile_ProfileNotExists() {
        Long id = 715L;
        StudentProfile profile = makeStudentProfile(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        verify(facade).deleteById(id);
        assertThat(exception.getMessage()).isEqualTo("Profile with ID:715 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 703L;

        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class, () -> facade.deleteById(id));

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:703 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        StudentProfile profile = makeStudentProfile(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        StudentProfile profile = makeStudentProfile(null);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    private CommandsFactory<StudentProfileCommand<?>> buildFactory(ProfilePersistenceFacade persistence) {
        Map<StudentProfileCommand<?>, String> commands = Map.of(
                spy(new CreateOrUpdateStudentProfileCommand(persistence, payloadMapper)), "profileStudentUpdate",
                spy(new FindStudentProfileCommand(persistence, payloadMapper)), "profileStudentFind",
                spy(new DeleteStudentProfileCommand(persistence, payloadMapper)), "profileStudentDelete"
        );
        String acName = "applicationContext";
        commands.entrySet().forEach(entry -> {
            StudentProfileCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
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