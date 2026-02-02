package oleg.sopilnyak.test.end2end.command.factory.profile;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local.LocalQueueCommandExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

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
        StudentProfileCommandsFactoryTest.FactoryConfiguration.class,
        PersistenceConfiguration.class,
        TestConfig.class
})
@Rollback
class StudentProfileCommandsFactoryTest extends MysqlTestModelFactory {
    private static final String FACTORY_NAME = "Student-Profiles";
    private static final String SPRING_NAME = "studentProfileCommandsFactory";
    private Collection<String> commandsId;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    CommandsFactory<StudentProfileCommand<?>> factory;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        commandsId = Set.of(
        "school::person::profile::student:find.By.Id",
        "school::person::profile::student:create.Or.Update",
        "school::person::profile::student:delete.By.Id"
//                "profile.student.findById",
//                "profile.student.deleteById",
//                "profile.student.createOrUpdate"
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
        public CommandsFactory<StudentProfileCommand<?>> commandsFactory(final Collection<StudentProfileCommand<?>> commands) {
            return new StudentProfileCommandsFactory(commands);
        }

        @Bean
        public CommandActionExecutor actionExecutor() {
            return new LocalQueueCommandExecutor();
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