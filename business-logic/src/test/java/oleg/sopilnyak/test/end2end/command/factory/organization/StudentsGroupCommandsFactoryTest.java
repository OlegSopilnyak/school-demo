package oleg.sopilnyak.test.end2end.command.factory.organization;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.StudentsGroupCommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.facade.impl.ActionExecutorImpl;
import oleg.sopilnyak.test.service.facade.impl.command.message.service.local.CommandThroughMessageServiceLocalImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        StudentsGroupCommandsFactoryTest.FactoryConfiguration.class,
        PersistenceConfiguration.class,
        TestConfig.class
})
@Rollback
class StudentsGroupCommandsFactoryTest extends MysqlTestModelFactory {
    private static final String FACTORY_NAME = "Organization.StudentsGroups";
    private static final String SPRING_NAME = "groupCommandsFactory";
    private Collection<String> commandsId;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    CommandsFactory<StudentsGroupCommand<?>> factory;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        commandsId = Set.of(
                "organization.students.group.findAll",
                "organization.students.group.findById",
                "organization.students.group.createOrUpdate",
                "organization.students.group.delete"
        );
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeValidComponents() {
        assertThat(context).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldGetTheCommandsFactory() {
        assertThat(factory).isEqualTo(context.getBean(CommandsFactory.class)).isEqualTo(context.getBean(SPRING_NAME));
        assertThat(factory.getName()).isEqualTo(FACTORY_NAME);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

        @Bean
        public CommandActionExecutor actionExecutor() {
            return new ActionExecutorImpl(commandThroughMessageService());
        }

        @Bean
        public CommandThroughMessageService commandThroughMessageService() {
            return new CommandThroughMessageServiceLocalImpl();
        }

        @Bean
        public SchedulingTaskExecutor parallelCommandNestedCommandsExecutor(
                @Value("${school.parallel.max.pool.size:100}") final int maxPoolSize
        ) {
            final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            final int operationalPoolSize = Math.max(maxPoolSize, Runtime.getRuntime().availableProcessors());
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(operationalPoolSize);
            executor.setThreadNamePrefix("ParallelCommandThread-");
            executor.initialize();
            return executor;
        }
    }
}