package oleg.sopilnyak.test.service.command.student;

import oleg.sopilnyak.test.school.common.facade.peristence.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

class CreateOrUpdateStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistenceFacade;
    @Mock
    Student student;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentCommand command;

    @Test
    void shouldExecuteCommand() {
    }
}