package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static oleg.sopilnyak.test.service.command.type.core.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
@Rollback
@SuppressWarnings("unchecked")
class LogoutAuthorityPersonCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @MockitoSpyBean
    @Autowired
    @Qualifier("authorityPersonLogout")
    AuthorityPersonCommand command;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDoCommand() {
        String token = "logged_in_person_token";
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(token));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUndoCommand_NothingToDo() {
        String token = "logged_in_person_token";
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(token));
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}