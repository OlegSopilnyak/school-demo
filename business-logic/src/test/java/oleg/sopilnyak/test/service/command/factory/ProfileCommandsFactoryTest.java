package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCommandsFactoryTest<T> {
    @Mock
    ProfileCommand<T> command1;
    @Mock
    ProfileCommand<T> command2;
    @Mock
    ProfileCommand<T> command3;
    ProfileCommandsFactory<T> factory;

    @BeforeEach
    void setUp() {
        when(command1.getId()).thenReturn("cmd1");
        when(command2.getId()).thenReturn("cmd2");
        when(command3.getId()).thenReturn("cmd3");
//        factory = new ProfileCommandsFactory<>(List.of(command1, command2, command3));
    }

    @Test
    void shouldGetTheCommandByCommandId() {
        assertThat(factory.command("cmd1")).isEqualTo(command1);
        assertThat(factory.command("cmd2")).isEqualTo(command2);
        assertThat(factory.command("cmd3")).isEqualTo(command3);
    }

    @Test
    void shouldNotGetTheCommandByCommandId() {
        assertThat(factory.command(null)).isNull();
        assertThat(factory.command("cmd4")).isNull();
    }

}