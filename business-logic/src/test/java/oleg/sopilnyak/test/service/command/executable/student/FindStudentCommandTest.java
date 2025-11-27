package oleg.sopilnyak.test.service.command.executable.student;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.education.student.FindStudentCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FindStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Mock
    Student instance;
    @Mock
    StudentPayload payload;
    @Spy
    @InjectMocks
    FindStudentCommand command;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        doReturn(command).when(applicationContext).getBean("studentFind", StudentCommand.class);
    }

    @Test
    void shouldDoCommand_StudentNotFound() {
        Long id = 106L;
        Context<Optional<Student>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
    }

    @Test
    void shouldDoCommand_StudentFound() {
        Long id = 107L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(instance));
        when(payloadMapper.toPayload(instance)).thenReturn(payload);
        Context<Optional<Student>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = context.getResult().orElseThrow();
        assertThat(result).contains(payload);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 108L;
        RuntimeException cannotExecute = new RuntimeException();
        doThrow(cannotExecute).when(persistence).findStudentById(id);
        Context<Optional<Student>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }
}