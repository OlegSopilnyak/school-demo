package oleg.sopilnyak.test.end2end.facade.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.CreateOrUpdateFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.DeleteFacultyCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindAllFacultiesCommand;
import oleg.sopilnyak.test.service.command.executable.organization.faculty.FindFacultyCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.factory.organization.FacultyCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.JsonContextModule;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.FacultyFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collection;
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
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class FacultyFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_FACULTY_FIND_ALL = "organization.faculty.findAll";
    private static final String ORGANIZATION_FACULTY_FIND_BY_ID = "organization.faculty.findById";
    private static final String ORGANIZATION_FACULTY_CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    private static final String ORGANIZATION_FACULTY_DELETE = "organization.faculty.delete";

    @MockitoSpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @MockitoSpyBean
    @Autowired
    CommandThroughMessageService commandThroughMessageService;
    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    ApplicationContext applicationContext;

    @MockitoSpyBean
    @Autowired
    FacultyPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    CommandsFactoriesFarm farm;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;

    CommandsFactory<FacultyCommand<?>> factory;
    FacultyFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistence));
        facade = spy(new FacultyFacadeImpl(factory, payloadMapper, actionExecutor));
        farm.register(factory);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()).registerModule(new JsonContextModule<>(applicationContext, farm))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        commandThroughMessageService.shutdown();
        ReflectionTestUtils.setField(commandThroughMessageService, "objectMapper", objectMapper);
        commandThroughMessageService.initialize();
        ActionContext.setup("test-facade", "test-processing");
    }

    @AfterEach
    void tearDown() {
        deleteEntities(CourseEntity.class);
        deleteEntities(FacultyEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(applicationContext).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(actionExecutor).isNotNull();
        assertThat(commandThroughMessageService).isNotNull();
        assertThat(farm).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    void shouldFindAllFaculties_EmptySet() {

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).isEmpty();
        verifyAfterCommand(ORGANIZATION_FACULTY_FIND_ALL, Input.empty());
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldFindAllFaculties_NotEmptySet() {
        Faculty faculty = payloadMapper.toPayload(persistFaculty());

        Collection<Faculty> faculties = facade.findAllFaculties();

        assertThat(faculties).contains(faculty);
        verifyAfterCommand(ORGANIZATION_FACULTY_FIND_ALL, Input.empty());
        verify(persistence).findAllFaculties();
    }

    @Test
    void shouldFindFacultyById() {
        Faculty entity = persistFaculty();
        Long id = entity.getId();

        Optional<Faculty> faculty = facade.findFacultyById(id);

        assertThat(faculty).isPresent();
        assertFacultyEquals(faculty.get(), entity);
        verifyAfterCommand(ORGANIZATION_FACULTY_FIND_BY_ID, Input.of(id));
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldNotFindFacultyById() {
        Long id = 400L;

        Optional<Faculty> faculty = facade.findFacultyById(id);

        assertThat(faculty).isEmpty();
        verifyAfterCommand(ORGANIZATION_FACULTY_FIND_BY_ID, Input.of(id));
        verify(persistence).findFacultyById(id);
    }

    @Test
    void shouldCreateOrUpdateFaculty_Create() {
        Faculty facultySource = payloadMapper.toPayload(makeCleanFacultyNoDean(1));

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(facultySource);

        assertThat(faculty).isPresent();
        assertFacultyEquals(faculty.get(), facultySource, false);
        verifyAfterCommand(ORGANIZATION_FACULTY_CREATE_OR_UPDATE, Input.of(facultySource));
        verify(persistence).save(facultySource);
    }

    @Test
    void shouldCreateOrUpdateFaculty_Update() {
        Faculty facultySource = payloadMapper.toPayload(persistFaculty());
        Long id = facultySource.getId();
        reset(payloadMapper);

        Optional<Faculty> faculty = facade.createOrUpdateFaculty(facultySource);

        assertThat(faculty).isPresent();
        assertFacultyEquals(faculty.get(), facultySource);
        verifyAfterCommand(ORGANIZATION_FACULTY_CREATE_OR_UPDATE, Input.of(facultySource));
        verify(persistence).findFacultyById(id);
        verify(payloadMapper, times(2)).toPayload(any(FacultyEntity.class));
        verify(persistence).save(facultySource);
    }

    @Test
    void shouldNotCreateOrUpdateFaculty() {
        String commandId = ORGANIZATION_FACULTY_CREATE_OR_UPDATE;
        Long id = 401L;
        Faculty facultySource = payloadMapper.toPayload(makeCleanFacultyNoDean(1));
        reset(payloadMapper);
        if (facultySource instanceof FacultyPayload source) {
            source.setId(id);
        }

        var thrown = assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateFaculty(facultySource));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(commandId);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(FacultyNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("Faculty with ID:").endsWith(" is not exists.");
        verifyAfterCommand(commandId, Input.of(facultySource));
        verify(persistence).findFacultyById(id);
        verify(payloadMapper, never()).toPayload(any(FacultyEntity.class));
        verify(persistence, never()).save(any(Faculty.class));
    }

    @Test
    void shouldDeleteFacultyById() throws FacultyNotFoundException, FacultyIsNotEmptyException {
        Faculty facultySource = persistFaculty();
        Long id = facultySource.getId();

        facade.deleteFacultyById(id);

        verifyAfterCommand(ORGANIZATION_FACULTY_DELETE, Input.of(id));
        verify(persistence).findFacultyById(id);
        verify(persistence).deleteFaculty(id);
        assertThat(persistence.findFacultyById(id)).isEmpty();
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotExists() {
        Long id = 403L;

        var thrown = assertThrows(FacultyNotFoundException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).isEqualTo("Faculty with ID:403 is not exists.");
        verifyAfterCommand(ORGANIZATION_FACULTY_DELETE, Input.of(id));
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).deleteFaculty(id);
    }

    @Test
    void shouldNoDeleteFacultyById_FacultyNotEmpty() {
        Faculty facultySource = makeFacultyNoDean(3);
        if (facultySource instanceof FakeFaculty source) {
            source.setId(null);
        }
        Optional<Faculty> faculty = Optional.of(persist(facultySource));
        assertThat(faculty).isPresent();
        Long id = faculty.get().getId();

        var thrown = assertThrows(FacultyIsNotEmptyException.class, () -> facade.deleteFacultyById(id));

        assertThat(thrown.getMessage()).startsWith("Faculty with ID").endsWith(" has courses.");
        verifyAfterCommand(ORGANIZATION_FACULTY_DELETE, Input.of(id));
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).deleteFaculty(id);
    }

    // private methods
    private void verifyAfterCommand(String commandId, Input<?> commandInput) {
        verify(farm, times(2)).command(commandId);
        verify(factory, times(3)).command(commandId);
        verify(factory.command(commandId)).createContext(commandInput);
        verify(factory.command(commandId)).doCommand(any(Context.class));
    }

    private CommandsFactory<FacultyCommand<?>> buildFactory(FacultyPersistenceFacade persistence) {
        Map<FacultyCommand<?>, String> commands = Map.of(
                spy(new CreateOrUpdateFacultyCommand(persistence, payloadMapper)), "facultyUpdate",
                spy(new DeleteFacultyCommand(persistence, payloadMapper)), "facultyDelete",
                spy(new FindAllFacultiesCommand(persistence, payloadMapper)), "facultyFindAll",
                spy(new FindFacultyCommand(persistence, payloadMapper)), "facultyFind"
        );
        String acName = "applicationContext";
        commands.entrySet().forEach(entry -> {
            FacultyCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            FacultyCommand<?> transactionalCommand = (FacultyCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        return new FacultyCommandsFactory(commands.keySet());
    }

    private Faculty persistFaculty() {
        return  persist(makeCleanFacultyNoDean(0));
    }

    private Faculty persist(Faculty source) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            FacultyEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            entity = em.find(FacultyEntity.class, entity.getId());
            entity.getCourseEntitySet().size();
            transaction.commit();
            return entity;
        }
    }
}