package oleg.sopilnyak.test.service.command.factory.organization;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
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
@ContextConfiguration(classes = AuthorityPersonCommandsFactorySpringTest.FactoryConfiguration.class)
class AuthorityPersonCommandsFactorySpringTest {
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
    @MockitoBean
    CommandActionExecutor actionExecutor;
    @MockitoBean(name = "parallelCommandNestedCommandsExecutor")
    SchedulingTaskExecutor executor;
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoBean
    AuthenticationFacade authenticationFacade;
    @MockitoBean
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
        public CommandsFactory<AuthorityPersonCommand<?>> commandsFactory(final Collection<AuthorityPersonCommand<?>> commands) {
            return new AuthorityPersonCommandsFactory(commands);
        }
    }
}