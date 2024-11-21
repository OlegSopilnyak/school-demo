package oleg.sopilnyak.test.service.command.factory.organization;

import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AuthorityPersonCommandsFactorySpringTest.FactoryConfiguration.class)
class AuthorityPersonCommandsFactorySpringTest {
    private static final String FACTORY_NAME = "Organization.AuthorityPersons";
    private static final String SPRING_NAME = "authorityCommandsFactory";
    private Collection<String> commandsId;
    @MockBean
    PersistenceFacade persistenceFacade;
    @MockBean
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    CommandsFactory<AuthorityPersonCommand<?>> factory;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        commandsId = Set.of(
                "organization.authority.person.login",
                "organization.authority.person.logout",
                "organization.authority.person.findAll",
                "organization.authority.person.findById",
                "organization.authority.person.createOrUpdate",
                "organization.authority.person.create.macro",
                "organization.authority.person.delete",
                "organization.authority.person.delete.macro"

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