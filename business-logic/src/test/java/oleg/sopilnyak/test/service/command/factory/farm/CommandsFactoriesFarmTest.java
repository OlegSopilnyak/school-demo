package oleg.sopilnyak.test.service.command.factory.farm;

import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandsFactoriesFarmTest<T extends SchoolCommand> {
    @Mock
    CommandsFactory<T> factory1;
    @Mock
    CommandsFactory<T> factory2;
    @Mock
    CommandsFactory<T> factory3;

    CommandsFactoriesFarm farm;

    @BeforeEach
    void setUp() {
        farm = new CommandsFactoriesFarm(Set.of(factory1, factory2, factory3));
    }

    @Test
    void shouldFindCommandFactory() {
        when(factory1.getName()).thenReturn("factory1");
        when(factory2.getName()).thenReturn("factory2");
        when(factory3.getName()).thenReturn("factory3");
        assertThat(farm.findCommandFactory("factory1")).isEqualTo(Optional.of(factory1));
        assertThat(farm.findCommandFactory("factory2")).isEqualTo(Optional.of(factory2));
        assertThat(farm.findCommandFactory("factory3")).isEqualTo(Optional.of(factory3));
    }

    @Test
    void shouldNotFindCommandFactory() {
        when(factory1.getName()).thenReturn("factory1");
        when(factory2.getName()).thenReturn("factory2");
        when(factory3.getName()).thenReturn("factory3");
        assertThat(farm.findCommandFactory("factory4")).isEmpty();
        assertThat(farm.findCommandFactory(null)).isEmpty();
    }

    @Test
    void shouldGetCommandFromFarm() {
        T cmd1 = (T) mock(SchoolCommand.class);
        T cmd2 = (T) mock(SchoolCommand.class);
        when(factory1.command("cmd1")).thenReturn(cmd1);
        when(factory1.command("cmd2")).thenReturn(cmd2);
        assertThat(farm.command("cmd1")).isEqualTo(cmd1);
        assertThat(farm.command("cmd2")).isEqualTo(cmd2);
    }
}