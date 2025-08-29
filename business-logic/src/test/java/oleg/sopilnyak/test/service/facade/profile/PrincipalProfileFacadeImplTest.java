package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.FindPrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrincipalProfileFacadeImplTest {
    private static final String PROFILE_FIND_BY_ID = "profile.principal.findById";
    private static final String PROFILE_CREATE_OR_UPDATE = "profile.principal.createOrUpdate";
    private static final String PROFILE_DELETE = "profile.principal.deleteById";
    ProfilePersistenceFacade persistence = mock(ProfilePersistenceFacade.class);
    BusinessMessagePayloadMapper payloadMapper = mock(BusinessMessagePayloadMapper.class);
    @Spy
    CommandsFactory<PrincipalProfileCommand<?>> factory = buildFactory();

    @Spy
    @InjectMocks
    PrincipalProfileFacadeImpl facade;
    @Mock
    PrincipalProfile profile;
    @Mock
    PrincipalProfilePayload payload;

    @Test
    void shouldFindProfileById() {
        Long id = 600L;
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));

        Optional<PrincipalProfile> result = facade.findPrincipalProfileById(id);

        assertThat(result).contains(payload);
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(payloadMapper).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldNotFindProfileById() {
        Long id = 610L;

        Optional<PrincipalProfile> faculty = facade.findPrincipalProfileById(id);

        assertThat(faculty).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
    }

    @Test
    void shouldNotFindProfileById_WrongProfileType() {
        Long id = 611L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(mock(StudentProfile.class)));

        Optional<PrincipalProfile> profileFromDb = facade.findPrincipalProfileById(id);

        assertThat(profileFromDb).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(Input.of(id));
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
    }

    @Test
    void shouldCreateOrUpdateProfile() {
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);
        when(persistence.save(payload)).thenReturn(Optional.of(payload));

        Optional<PrincipalProfile> result = facade.createOrUpdateProfile(profile);

        assertThat(result).contains(payload);
        verify(facade).createOrUpdate(profile);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(Input.of(payload));
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(payload);
        verify(payloadMapper).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldNotCreateOrUpdateProfile() {
        when(payloadMapper.toPayload(any(PersonProfile.class))).thenReturn(payload);

        Optional<PrincipalProfile> result = facade.createOrUpdateProfile(profile);

        assertThat(result).isEmpty();
        verify(facade).createOrUpdate(profile);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(Input.of(payload));
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(payload);
        verify(payloadMapper).toPayload(any(PersonProfile.class));
    }

    @Test
    void shouldDeleteProfileById() {
        Long id = 602L;
        when(persistence.findPrincipalProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        facade.deleteById(id);

        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfile_ProfileNotExists() throws ProfileNotFoundException {
        Long id = 615L;

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.deleteById(id));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:615 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 603L;
        ProfileNotFoundException thrown = assertThrows(ProfileNotFoundException.class, () -> facade.deleteById(id));

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:603 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldDeleteProfileInstance() throws ProfileNotFoundException {
        Long id = 614L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);

        facade.delete(profile);

        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_ProfileNotExists() throws ProfileNotFoundException {
        Long id = 716L;
        when(profile.getId()).thenReturn(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).isEqualTo("Profile with ID:716 is not exists.");
        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(Input.of(id));
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        when(profile.getId()).thenReturn(id);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        when(profile.getId()).thenReturn(null);

        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    private CommandsFactory<PrincipalProfileCommand<?>> buildFactory() {
        return new PrincipalProfileCommandsFactory(
                Set.of(
                        spy(new FindPrincipalProfileCommand(persistence, payloadMapper)),
                        spy(new CreateOrUpdatePrincipalProfileCommand(persistence, payloadMapper)),
                        spy(new DeletePrincipalProfileCommand(persistence, payloadMapper))
                )

        );
    }
}