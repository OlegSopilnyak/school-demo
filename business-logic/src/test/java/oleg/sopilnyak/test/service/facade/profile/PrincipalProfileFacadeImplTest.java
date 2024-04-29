package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.FindPrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrincipalProfileFacadeImplTest {
    private static final String PROFILE_FIND_BY_ID = "profile.principal.findById";
    private static final String PROFILE_CREATE_OR_UPDATE = "profile.principal.createOrUpdate";
    private static final String PROFILE_DELETE = "profile.principal.deleteById";
    ProfilePersistenceFacade persistence = mock(ProfilePersistenceFacade.class);
    @Spy
    CommandsFactory<?> factory = buildFactory();

    @Spy
    @InjectMocks
    PrincipalProfileFacadeImpl facade;
    @Mock
    PrincipalProfile mock;

    @Test
    void shouldFindProfileById() {
        Long id = 600L;
        when(persistence.findPrincipalProfileById(id)).thenReturn(Optional.of(mock));

        Optional<PrincipalProfile> faculty = facade.findPrincipalProfileById(id);

        assertThat(faculty).isPresent();
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
    }

    @Test
    void shouldNotFindProfileById() {
        Long id = 610L;

        Optional<PrincipalProfile> faculty = facade.findPrincipalProfileById(id);

        assertThat(faculty).isEmpty();
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile() {
        when(persistence.save(mock)).thenReturn(Optional.of(mock));

        Optional<PrincipalProfile> faculty = facade.createOrUpdateProfile(mock);

        assertThat(faculty).isPresent();
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(mock);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(mock);
    }

    @Test
    void shouldDeleteProfileById() {
        Long id = 602L;
        when(persistence.findPrincipalProfileById(id)).thenReturn(Optional.of(mock));
        when(persistence.toEntity(mock)).thenReturn(mock);

        facade.deleteById(id);

        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 603L;
        NotExistProfileException thrown =
                assertThrows(NotExistProfileException.class, () -> facade.deleteById(id));

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:603 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    private CommandsFactory<?> buildFactory() {
        return new PrincipalProfileCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdatePrincipalProfileCommand(persistence)),
                        spy(new FindPrincipalProfileCommand(persistence)),
                        spy(new DeletePrincipalProfileCommand(persistence))
                )

        );
    }
}