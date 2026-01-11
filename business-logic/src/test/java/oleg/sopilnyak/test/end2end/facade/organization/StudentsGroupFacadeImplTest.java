package oleg.sopilnyak.test.end2end.facade.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.organization.group.CreateOrUpdateStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.DeleteStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindAllStudentsGroupsCommand;
import oleg.sopilnyak.test.service.command.executable.organization.group.FindStudentsGroupCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.JsonContextModule;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.organization.impl.StudentsGroupFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
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
class StudentsGroupFacadeImplTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "organization.students.group.findAll";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "organization.students.group.findById";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "organization.students.group.delete";

    @MockitoSpyBean
    @Autowired
    CommandActionExecutor actionExecutor;
    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    ApplicationContext applicationContext;

    @MockitoSpyBean
    @Autowired
    StudentsGroupPersistenceFacade persistence;
    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    CommandsFactoriesFarm farm;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;

    CommandsFactory<StudentsGroupCommand<?>> factory;
    StudentsGroupFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistence));
        facade = spy(new StudentsGroupFacadeImpl(factory, payloadMapper, actionExecutor));
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
        deleteEntities(CourseEntity.class);
        deleteEntities(StudentEntity.class);
        deleteEntities(StudentsGroupEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(applicationContext).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(actionExecutor).isNotNull();
        assertThat(farm).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }


    @Test
    void shouldFindAllStudentsGroup() {
        StudentsGroup group = payloadMapper.toPayload(persistStudentsGroup());

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).contains(group);
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_FIND_ALL, Input.empty());
        verify(persistence).findAllStudentsGroups();
    }

    @Test
    void shouldNotFindAllStudentsGroup() {

        Collection<StudentsGroup> groups = facade.findAllStudentsGroups();

        assertThat(groups).isEmpty();
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_FIND_ALL, Input.empty());
        verify(persistence).findAllStudentsGroups();
    }

    @Test
    void shouldFindStudentsGroupById() {
        StudentsGroup group = persistStudentsGroup();
        Long id = group.getId();

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isPresent();
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID, Input.of(id));
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    void shouldNotFindStudentsGroupById() {
        Long id = 510L;

        Optional<StudentsGroup> faculty = facade.findStudentsGroupById(id);

        assertThat(faculty).isEmpty();
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID, Input.of(id));
        verify(persistence).findStudentsGroupById(id);
    }

    @Test
    void shouldNotCreateOrUpdateStudentsGroup_Create() {
        Long id = 511L;
        StudentsGroup group = payloadMapper.toPayload(makeTestStudentsGroup(id));

        UnableExecuteCommandException thrown =
                assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateStudentsGroup(group));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(StudentsGroupNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("Students Group with ID:").endsWith(" is not exists.");
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE, Input.of(group));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).save(any(StudentsGroup.class));
    }

    @Test
    void shouldCreateOrUpdateStudentsGroup_Create() {
        StudentsGroup group = payloadMapper.toPayload(makeCleanStudentsGroup(3));

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(group);

        assertThat(faculty).isPresent();
        assertStudentsGroupEquals(group, faculty.get(), false);
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE, Input.of(group));
        verify(persistence).save(group);
    }

    @Test
    void shouldCreateOrUpdateStudentsGroup_Update() {
        StudentsGroup group = payloadMapper.toPayload(persistStudentsGroup());
        reset(payloadMapper);
        Long id = group.getId();

        Optional<StudentsGroup> faculty = facade.createOrUpdateStudentsGroup(group);

        assertThat(faculty).isPresent();
        assertStudentsGroupEquals(group, faculty.get());
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE, Input.of(group));
        verify(persistence).findStudentsGroupById(id);
        verify(payloadMapper, times(2)).toPayload(any(StudentsGroupEntity.class));
        verify(persistence).save(group);
    }

    @Test
    void shouldDeleteStudentsGroupById() {
        StudentsGroup group = persistStudentsGroup();
        Long id = group.getId();

        facade.deleteStudentsGroupById(id);

        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_DELETE, Input.of(id));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence).deleteStudentsGroup(id);
        assertThat(findStudentsGroupById(id)).isNull();
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotExists() {
        Long id = 503L;
        StudentsGroupNotFoundException thrown =
                assertThrows(StudentsGroupNotFoundException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).isEqualTo("Students Group with ID:503 is not exists.");
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_DELETE, Input.of(id));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).deleteStudentsGroup(anyLong());
    }

    @Test
    void shouldNotDeleteStudentsGroupById_GroupNotEmpty() throws StudentGroupWithStudentsException, StudentsGroupNotFoundException {
        StudentsGroup studentsGroup = makeCleanStudentsGroup(3);
        if (studentsGroup instanceof FakeStudentsGroup fake) {
            fake.setStudents(List.of(makeClearStudent(1)));
        }
        StudentsGroup group = persist(studentsGroup);
        Long id = group.getId();

        var thrown = assertThrows(StudentGroupWithStudentsException.class, () -> facade.deleteStudentsGroupById(id));

        assertThat(thrown.getMessage()).startsWith("Students Group with ID:").endsWith(" has students.");
        verifyAfterCommand(ORGANIZATION_STUDENTS_GROUP_DELETE, Input.of(id));
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).deleteStudentsGroup(anyLong());
    }

    // private methods
    private void verifyAfterCommand(String commandId, Input<?> commandInput) {
        verify(farm, times(2)).command(commandId);
        verify(factory, times(3)).command(commandId);
        verify(factory.command(commandId)).createContext(commandInput);
        verify(factory.command(commandId)).doCommand(any(Context.class));
    }

    private CommandsFactory<StudentsGroupCommand<?>> buildFactory(StudentsGroupPersistenceFacade persistence) {
        Map<StudentsGroupCommand<?>, String> commands = Map.of(
                spy(new CreateOrUpdateStudentsGroupCommand(persistence, payloadMapper)),"studentsGroupUpdate",
                spy(new DeleteStudentsGroupCommand(persistence, payloadMapper)),"studentsGroupDelete",
                spy(new FindAllStudentsGroupsCommand(persistence, payloadMapper)),"studentsGroupFindAll",
                spy(new FindStudentsGroupCommand(persistence, payloadMapper)),"studentsGroupFind"
        );
        String acName = "applicationContext";
        commands.entrySet().forEach(entry -> {
            StudentsGroupCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            StudentsGroupCommand<?> transactionalCommand = (StudentsGroupCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        return new StudentsGroupCommandsFactory(commands.keySet());
    }

    private StudentsGroup findStudentsGroupById(Long id) {
        return findEntity(StudentsGroupEntity.class, id, entity -> entity.getStudentEntitySet().size());
    }

    private StudentsGroup persistStudentsGroup() {
        StudentsGroup source = makeCleanStudentsGroup(10);
        if (source instanceof FakeStudentsGroup fake) {
            fake.setStudents(List.of());
            fake.setLeader(null);
        }
        return persist(source);
    }

    private StudentsGroup persist(StudentsGroup source) {
        try(EntityManager em = entityManagerFactory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            StudentsGroupEntity entity = entityMapper.toEntity(source);
            transaction.begin();
            em.persist(entity);
            em.flush();
            em.clear();
            transaction.commit();
            return payloadMapper.toPayload(em.find(StudentsGroupEntity.class, entity.getId()));
        } finally {
            reset(payloadMapper);
        }
    }
}