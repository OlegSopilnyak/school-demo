package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CreateStudentMacroCommandTest {
    @Mock
    CreateOrUpdateStudentProfileCommand profileCommand;
    @Mock
    CreateOrUpdateStudentCommand studentCommand;
    @Spy
    @InjectMocks
    CreateStudentMacroCommand command;

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
    }
}