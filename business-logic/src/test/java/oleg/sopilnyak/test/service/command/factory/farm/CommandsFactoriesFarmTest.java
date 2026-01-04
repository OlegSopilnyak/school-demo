package oleg.sopilnyak.test.service.command.factory.farm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandsFactoriesFarmTest<T extends RootCommand<?>> {
    @Mock
    CommandsFactory<T> factory1;
    @Mock
    CommandsFactory<T> factory2;
    @Mock
    CommandsFactory<T> factory3;

    CommandsFactoriesFarm<T> farm;

    @BeforeEach
    void setUp() {
        doReturn(RootCommand.class).when(factory1).commandFamily();
        doReturn(StudentCommand.class).when(factory2).commandFamily();
        doReturn(CourseCommand.class).when(factory3).commandFamily();
        farm = new CommandsFactoriesFarm<>(Set.of(factory1, factory2, factory3));
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
        T cmd1 = (T) mock(RootCommand.class);
        T cmd2 = (T) mock(RootCommand.class);
        when(factory1.command("cmd1")).thenReturn(cmd1);
        when(factory1.command("cmd2")).thenReturn(cmd2);
        assertThat(farm.command("cmd1")).isEqualTo(cmd1);
        assertThat(farm.command("cmd2")).isEqualTo(cmd2);
    }
}