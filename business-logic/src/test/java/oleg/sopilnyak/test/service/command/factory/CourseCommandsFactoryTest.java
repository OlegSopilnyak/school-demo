package oleg.sopilnyak.test.service.command.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.service.command.type.education.CourseCommand;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseCommandsFactoryTest {
    @Mock
    CourseCommand<?> command1;
    @Mock
    CourseCommand<?> command2;
    @Mock
    CourseCommand<?> command3;
    CourseCommandsFactory factory;

    @BeforeEach
    void setUp() {
        when(command1.getId()).thenReturn("cmd1");
        when(command2.getId()).thenReturn("cmd2");
        when(command3.getId()).thenReturn("cmd3");
        factory = new CourseCommandsFactory(List.of(command1, command2, command3));
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