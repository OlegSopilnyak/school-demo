package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.SchoolCommandsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PersonProfileFacadeImplTest {
    PersistenceFacade persistenceFacade = mock(PersistenceFacade.class);
    @Spy
    CommandsFactory factory = buildFactory();

    @Spy
    @InjectMocks
    PersonProfileFacadeImpl facade;

    @Test
    void shouldFindById() {
        String commandId = ProfileCommandFacade.FIND_BY_ID;
        Long profileId = 400L;
//
//        Optional<PersonProfile> profile = facade.findById(profileId);
//
//        assertThat(profile).isEmpty();
//        verify(factory).command(commandId);
//        verify(factory.command(commandId)).execute(profileId);
//        verify(persistenceFacade).findStudentById(profileId);
    }

    @Test
    void findStudentProfileById() {
    }

    @Test
    void findPrincipalProfileById() {
    }

    private CommandsFactory buildFactory() {
        return new SchoolCommandsFactory("person-profile",
                Set.of(
                )
        );
    }
}