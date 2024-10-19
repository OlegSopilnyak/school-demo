package oleg.sopilnyak.test.end2end.facade.profile;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.PrincipalProfileEntity;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.principal.FindPrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.PrincipalProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.profile.impl.PrincipalProfileFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class PrincipalProfileFacadeImplTest extends MysqlTestModelFactory {
    private static final String PROFILE_FIND_BY_ID = "profile.principal.findById";
    private static final String PROFILE_CREATE_OR_UPDATE = "profile.principal.createOrUpdate";
    private static final String PROFILE_DELETE = "profile.principal.deleteById";
    @Autowired
    PersistenceFacade database;

    ProfilePersistenceFacade persistence;
    CommandsFactory<PrincipalProfileCommand> factory;
    PrincipalProfileFacadeImpl facade;
    BusinessMessagePayloadMapper payloadMapper;

    @BeforeEach
    void setUp() {
        payloadMapper = spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
        persistence = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistence));
        facade = spy(new PrincipalProfileFacadeImpl(factory, payloadMapper));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldAllPartsBeReady() {
        assertThat(database).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindProfileById_ProfileExists() {
        PrincipalProfile profile = persistPrincipal();
        Long id = profile.getId();

        Optional<PrincipalProfile> entity = facade.findPrincipalProfileById(id);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profile, true);
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindProfileById_ProfileNotExist() {
        Long id = 610L;

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(id);

        assertThat(profile).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindProfileById_WrongProfileType() {
        Long id = persistStudent().getId();

        Optional<PrincipalProfile> profile = facade.findPrincipalProfileById(id);

        assertThat(profile).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateProfile_Create() {
        PrincipalProfile profileSource = payloadMapper.toPayload(makePrincipalProfile(null));

        Optional<PrincipalProfile> entity = facade.createOrUpdateProfile(profileSource);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profileSource, false);
        verify(facade).createOrUpdate(profileSource);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(profileSource);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).save(profileSource);
        verify(persistence).saveProfile(profileSource);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateProfile_Update() {
        PrincipalProfile profile = payloadMapper.toPayload(persistPrincipal());
        Long id = profile.getId();

        Optional<PrincipalProfile> entity = facade.createOrUpdateProfile(profile);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profile, false);
        verify(facade).createOrUpdate(profile);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(profile);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(any(PrincipalProfileEntity.class));
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateProfile_Create() {
        Long id = 611L;
        PrincipalProfile profileSource = payloadMapper.toPayload(makePrincipalProfile(id));

        UnableExecuteCommandException thrown =
                assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateProfile(profileSource));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(PROFILE_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(ProfileIsNotFoundException.class);
        assertThat(cause.getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(facade).createOrUpdate(profileSource);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(profileSource);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).save(any(PrincipalProfile.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteProfileById_ProfileExists() {
        PrincipalProfile profile = persistPrincipal();
        Long id = profile.getId();

        facade.deleteById(id);

        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findPrincipalProfileById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteProfile_ProfileExists() {
        PrincipalProfile profile = persistPrincipal();
        Long id = profile.getId();

        facade.delete(profile);

        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findPrincipalProfileById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteProfile_ProfileNotExists() throws ProfileIsNotFoundException {
        Long id = 615L;
        PrincipalProfile profile = makePrincipalProfile(id);

        ProfileIsNotFoundException exception = assertThrows(ProfileIsNotFoundException.class, () -> facade.delete(profile));

        verify(facade).deleteById(id);
        assertThat(exception.getMessage()).isEqualTo("Profile with ID:615 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(PrincipalProfile.class));
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 603L;
        ProfileIsNotFoundException thrown = assertThrows(ProfileIsNotFoundException.class, () -> facade.deleteById(id));

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:603 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(PrincipalProfile.class));
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        PrincipalProfile profile = makePrincipalProfile(id);

        ProfileIsNotFoundException exception = assertThrows(ProfileIsNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteProfileInstance_NullId() {
        PrincipalProfile profile = makePrincipalProfile(null);

        ProfileIsNotFoundException exception = assertThrows(ProfileIsNotFoundException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    // private methods
    private CommandsFactory<PrincipalProfileCommand> buildFactory(ProfilePersistenceFacade persistence) {
        return new PrincipalProfileCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdatePrincipalProfileCommand(persistence, payloadMapper)),
                        spy(new FindPrincipalProfileCommand(persistence)),
                        spy(new DeletePrincipalProfileCommand(persistence, payloadMapper))
                )

        );
    }

    private StudentProfile persistStudent() {
        StudentProfile profile = makeStudentProfile(null);
        StudentProfile entity = database.save(profile).orElse(null);
        assertThat(entity).isNotNull();
        Optional<StudentProfile> dbProfile = database.findStudentProfileById(entity.getId());
        assertProfilesEquals(dbProfile.orElseThrow(), profile, false);
        assertThat(dbProfile).contains(entity);
        return database.toEntity(entity);
    }

    private PrincipalProfile persistPrincipal() {
        PrincipalProfile profile = makePrincipalProfile(null);
        PrincipalProfile entity = database.save(profile).orElse(null);
        assertThat(entity).isNotNull();
        Optional<PrincipalProfile> dbProfile = database.findPrincipalProfileById(entity.getId());
        assertProfilesEquals(dbProfile.orElseThrow(), profile, false);
        assertThat(dbProfile).contains(entity);
        return database.toEntity(entity);
    }
}