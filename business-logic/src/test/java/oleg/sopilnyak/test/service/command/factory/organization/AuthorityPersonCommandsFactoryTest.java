package oleg.sopilnyak.test.service.command.factory.organization;

import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorityPersonCommandsFactoryTest {
    @Mock
    AuthorityPersonCommand<?> command1;
    @Mock
    AuthorityPersonCommand<?> command2;
    @Mock
    AuthorityPersonCommand<?> command3;

    AuthorityPersonCommandsFactory factory;

    @BeforeEach
    void setUp() {
        when(command1.getId()).thenReturn("cmd1");
        when(command2.getId()).thenReturn("cmd2");
        when(command3.getId()).thenReturn("cmd3");
        factory = new AuthorityPersonCommandsFactory(Set.of(command1, command2, command3));
    }

    @Test
    void shouldGetTheCommandByCommandId() {
        assertThat(factory.command("cmd1")).isEqualTo(command1);
        assertThat(factory.command("cmd2")).isEqualTo(command2);
        assertThat(factory.command("cmd3")).isEqualTo(command3);
    }

    @Test
    void shouldDontGetTheCommandByCommandId() {
        assertThat(factory.command(null)).isNull();
        assertThat(factory.command("cmd4")).isNull();
    }
}