package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import oleg.sopilnyak.test.service.command.student.*;
import oleg.sopilnyak.test.service.facade.student.StudentsFacadeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class PersonProfileFacadeImplTest {
    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    @Spy
    CommandsFactory factory = buildFactory();

    @Spy
    @InjectMocks
    PersonProfileFacadeImpl facade;

    @Test
    void findById() {
    }

    @Test
    void findStudentProfileById() {
    }

    @Test
    void findPrincipalProfileById() {
    }
    private CommandsFactory buildFactory() {
        return new SchoolCommandsFactory(
                Set.of(
                )
        );
    }
}