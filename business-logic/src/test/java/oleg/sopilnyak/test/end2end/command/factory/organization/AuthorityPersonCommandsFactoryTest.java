package oleg.sopilnyak.test.end2end.command.factory.organization;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.authentication.configuration.SchoolAuthenticationConfiguration;
import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local.LocalQueueCommandExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.AuthorityPersonCommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
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
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        AuthorityPersonCommandsFactoryTest.FactoryConfiguration.class,
        PersistenceConfiguration.class,
        TestConfig.class
})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
@Rollback
class AuthorityPersonCommandsFactoryTest extends MysqlTestModelFactory {
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGIN = "school::organization::authority::persons:login";
    private static final String ORGANIZATION_AUTHORITY_PERSON_LOGOUT = "school::organization::authority::persons:logout";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_ALL = "school::organization::authority::persons:find.All";
    private static final String ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID = "school::organization::authority::persons:find.By.Id";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW = "school::organization::authority::persons:create.Macro";
    private static final String ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE = "school::organization::authority::persons:create.Or.Update";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE = "school::organization::authority::persons:delete";
    private static final String ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL = "school::organization::authority::persons:delete.Macro";
    //
    private static final String FACTORY_NAME = "Organization.AuthorityPersons";
    private static final String SPRING_NAME = "authorityCommandsFactory";
    private Collection<String> commandsId;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    CommandsFactory<AuthorityPersonCommand<?>> factory;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        commandsId = Set.of(
                ORGANIZATION_AUTHORITY_PERSON_LOGIN,
                ORGANIZATION_AUTHORITY_PERSON_LOGOUT,
                ORGANIZATION_AUTHORITY_PERSON_FIND_ALL,
                ORGANIZATION_AUTHORITY_PERSON_FIND_BY_ID,
                ORGANIZATION_AUTHORITY_PERSON_CREATE_NEW,
                ORGANIZATION_AUTHORITY_PERSON_CREATE_OR_UPDATE,
                ORGANIZATION_AUTHORITY_PERSON_DELETE_ALL,
                ORGANIZATION_AUTHORITY_PERSON_DELETE
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
    @Import({SchoolAuthenticationConfiguration.class})
    @ComponentScan("oleg.sopilnyak.test.service.command.executable")
    static class FactoryConfiguration {
        @Bean(name = SPRING_NAME)
        public CommandsFactory<AuthorityPersonCommand<?>> commandsFactory(final Collection<AuthorityPersonCommand<?>> commands) {
            return new AuthorityPersonCommandsFactory(commands);
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