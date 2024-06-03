package oleg.sopilnyak.test.end2end.facade.profile;

import oleg.sopilnyak.test.end2end.facade.PersistenceFacadeDelegate;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.FindStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.StudentProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.profile.impl.StudentProfileFacadeImpl;
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
class StudentProfileFacadeImplTest extends MysqlTestModelFactory {
    private static final String PROFILE_FIND_BY_ID = "profile.student.findById";
    private static final String PROFILE_CREATE_OR_UPDATE = "profile.student.createOrUpdate";
    private static final String PROFILE_DELETE = "profile.student.deleteById";

    @Autowired
    PersistenceFacade database;

    ProfilePersistenceFacade persistence;
    CommandsFactory<StudentProfileCommand> factory;
    StudentProfileFacadeImpl facade;
    BusinessMessagePayloadMapper payloadMapper;

    @BeforeEach
    void setUp() {
        payloadMapper = spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
        persistence = spy(new PersistenceFacadeDelegate(database));
        factory = spy(buildFactory(persistence));
        facade = spy(new StudentProfileFacadeImpl(factory, payloadMapper));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldAllPartsBeReady() {
        assertThat(database).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindProfileById_ProfileExists() {
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        Optional<StudentProfile> entity = facade.findStudentProfileById(id);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profile, true);
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindProfileById_ProfileNotExist() {
        Long id = 710L;

        Optional<StudentProfile> faculty = facade.findStudentProfileById(id);

        assertThat(faculty).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotFindProfileById_WrongProfileType() {
        Long id = persistPrincipal().getId();

        Optional<StudentProfile> profile = facade.findStudentProfileById(id);

        assertThat(profile).isEmpty();
        verify(facade).findById(id);
        verify(factory).command(PROFILE_FIND_BY_ID);
        verify(factory.command(PROFILE_FIND_BY_ID)).createContext(id);
        verify(factory.command(PROFILE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateOrUpdateProfile_Create() {
        StudentProfile profileSource = makeStudentProfile(null);

        Optional<StudentProfile> entity = facade.createOrUpdateProfile(profileSource);

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
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        Optional<StudentProfile> entity = facade.createOrUpdateProfile(profile);

        assertThat(entity).isPresent();
        assertProfilesEquals(entity.get(), profile, true);
        verify(facade).createOrUpdate(profile);
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(profile);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotCreateOrUpdateProfile_Create() {
        Long id = 711L;
        StudentProfile profileSource = makeStudentProfile(null);
        if (profileSource instanceof FakeStudentsProfile fake) {
            fake.setId(id);
        }

        UnableExecuteCommandException thrown =
                assertThrows(UnableExecuteCommandException.class, () -> facade.createOrUpdateProfile(profileSource));

        assertThat(thrown.getMessage()).startsWith("Cannot execute command").contains(PROFILE_CREATE_OR_UPDATE);
        Throwable cause = thrown.getCause();
        assertThat(cause).isInstanceOf(NotExistProfileException.class);
        assertThat(cause.getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(factory).command(PROFILE_CREATE_OR_UPDATE);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).createContext(profileSource);
        verify(factory.command(PROFILE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(StudentProfile.class));
        verify(persistence, never()).save(any(StudentProfile.class));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteProfileById_ProfileExists() {
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        facade.deleteById(id);

        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteProfile_ProfileExists() {
        StudentProfile profile = persistStudent();
        Long id = profile.getId();

        facade.delete(profile);

        verify(facade).deleteById(id);
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).deleteProfileById(id);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence).deleteProfileById(id);
        assertThat(persistence.findStudentProfileById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteProfile_ProfileNotExists() {
        Long id = 715L;
        StudentProfile profile = makeStudentProfile(id);

        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(profile));

        verify(facade).deleteById(id);
        assertThat(exception.getMessage()).isEqualTo("Profile with ID:715 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteProfileById_ProfileNotExists() {
        Long id = 703L;

        NotExistProfileException thrown = assertThrows(NotExistProfileException.class, () -> facade.deleteById(id));

        assertThat(thrown.getMessage()).isEqualTo("Profile with ID:703 is not exists.");
        verify(factory).command(PROFILE_DELETE);
        verify(factory.command(PROFILE_DELETE)).createContext(id);
        verify(factory.command(PROFILE_DELETE)).doCommand(any(Context.class));
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(anyLong());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteProfileInstance_NegativeId() {
        Long id = -716L;
        StudentProfile profile = makeStudentProfile(id);

        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    @Test
    void shouldNotDeleteProfileInstance_NullId() {
        StudentProfile profile = makeStudentProfile(null);

        NotExistProfileException exception = assertThrows(NotExistProfileException.class, () -> facade.delete(profile));

        assertThat(exception.getMessage()).startsWith("Wrong ");
        verify(facade, never()).deleteById(anyLong());
        verify(factory, never()).command(PROFILE_DELETE);
    }

    private CommandsFactory<StudentProfileCommand> buildFactory(ProfilePersistenceFacade persistence) {
        return new StudentProfileCommandsFactory(
                Set.of(
                        spy(new CreateOrUpdateStudentProfileCommand(persistence)),
                        spy(new FindStudentProfileCommand(persistence)),
                        spy(new DeleteStudentProfileCommand(persistence))
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