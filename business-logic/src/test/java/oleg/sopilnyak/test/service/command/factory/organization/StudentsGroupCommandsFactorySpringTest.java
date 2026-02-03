package oleg.sopilnyak.test.service.command.factory.organization;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StudentsGroupCommandsFactorySpringTest.FactoryConfiguration.class)
class StudentsGroupCommandsFactorySpringTest {
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_ALL = "school::organization::student::groups:find.All";
    private static final String ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID = "school::organization::student::groups:find.By.Id";
    private static final String ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE = "school::organization::student::groups:create.Or.Update";
    private static final String ORGANIZATION_STUDENTS_GROUP_DELETE = "school::organization::student::groups:delete";
    private static final String FACTORY_NAME = "Organization.StudentsGroups";
    private static final String SPRING_NAME = "groupCommandsFactory";
    private Collection<String> commandsId;
    @MockitoBean
    CommandActionExecutor actionExecutor;
    @MockitoBean(name = "parallelCommandNestedCommandsExecutor")
    SchedulingTaskExecutor executor;
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoBean
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    CommandsFactory<StudentsGroupCommand<?>> factory;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        commandsId = Set.of(
                ORGANIZATION_STUDENTS_GROUP_FIND_ALL,
                ORGANIZATION_STUDENTS_GROUP_FIND_BY_ID,
                ORGANIZATION_STUDENTS_GROUP_CREATE_OR_UPDATE,
                ORGANIZATION_STUDENTS_GROUP_DELETE
//                "organization.students.group.findAll",
//                "organization.students.group.findById",
//                "organization.students.group.createOrUpdate",
//                "organization.students.group.delete"
        );
    }

    @Test
    void shouldBeValidComponents() {
        assertThat(context).isNotNull();
        assertThat(factory).isNotNull();
    }

    @Test
    void shouldGetTheCommandsFactory() {
        assertThat(factory).isEqualTo(context.getBean(CommandsFactory.class)).isEqualTo(context.getBean(SPRING_NAME));
        assertThat(factory.getName()).isEqualTo(FACTORY_NAME);
    }

    @Test
    void shouldGetTheCommandsByCommandId() {
        assertThat(factory.getSize()).isEqualTo(commandsId.size());
        commandsId.forEach(cmdId -> assertThat(factory.command(cmdId)).isNotNull());
    }

    @Configuration
    @ComponentScan("oleg.sopilnyak.test.service.command.executable")
    static class FactoryConfiguration {
        @Bean(name = SPRING_NAME)
        public CommandsFactory<StudentsGroupCommand<?>> commandsFactory(final Collection<StudentsGroupCommand<?>> commands) {
            return new StudentsGroupCommandsFactory(commands);
        }
    }
}