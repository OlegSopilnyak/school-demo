package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import static oleg.sopilnyak.test.service.command.type.core.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AccessCredentialsPayload;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {
        "school.spring.jpa.show-sql=true",
        "spring.liquibase.change-log=classpath:/database/changelog/dbChangelog_main.xml"
})
@SuppressWarnings("unchecked")
class LoginAuthorityPersonCommandTest extends MysqlTestModelFactory {
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistence;
    @MockitoSpyBean
    @Autowired
    AuthenticationFacade authenticationFacade;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @MockitoSpyBean
    @Autowired
    @Qualifier("authorityPersonLogin")
    AuthorityPersonCommand command;


    @AfterEach
    void tearDown() {
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(ReflectionTestUtils.getField(command, "authenticationFacade")).isSameAs(authenticationFacade);
        assertThat(ReflectionTestUtils.getField(command, "payloadMapper")).isSameAs(payloadMapper);
    }

    @Test
    void shouldDoCommand_EntityExists() {
        String username = "test-login";
        String password = "test-password";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow().orElseThrow()).isInstanceOf(AccessCredentialsPayload.class);
        assertThat(context.getUndoParameter().isEmpty()).isFalse();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
        verify(persistence).findPersonProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldDoCommand_AuthorityPersonNotExists() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));
        deleteEntity(AuthorityPersonEntity.class, entity.getId());

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException().getMessage()).isEqualTo(String.format("Person with username: '%s' isn't found!", username));
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
        verify(persistence).findPersonProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldNotDoCommand_PrincipalProfileNotExists() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));
        deleteEntity(PrincipalProfileEntity.class, id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException().getMessage()).isEqualTo(String.format("Profile with login:'%s', is not found", username));
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
        verify(persistence).findPersonProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldNotDoCommand_FindPrincipalProfileThrows() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        String error = "error finding principal profile";
        RuntimeException runtimeException = new RuntimeException(error);
        doThrow(runtimeException).when(persistence).findPrincipalProfileByLogin(username);
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));


        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(runtimeException);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldNotDoCommand_PrincipalProfileWrongPassword() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, "password"));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Wrong password for username: " + username);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, "password");
        verify(persistence).findPersonProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldNotDoCommand_FindAuthorityPersonThrows() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        long id = entity.getProfileId();
        String error = "error finding authority person";
        RuntimeException runtimeException = new RuntimeException(error);
        doThrow(runtimeException).when(persistence).findAuthorityPersonByProfileId(id);
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(runtimeException);
        assertThat(context.getException().getMessage()).isEqualTo(error);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
        verify(persistence).findPersonProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldUndoCommand_SigningOut() {
        String username = "login";
        String password = "pass";
        AuthorityPersonPayload entity = persist();
        setPersonPermissions(entity, username, password);
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));
        command.doCommand(context);
        AccessCredentials credentials = context.getResult().orElseThrow().orElseThrow();
        String token = credentials.getToken();

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getUndoParameter().value()).isEqualTo(token);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(authenticationFacade).signOut(token);
    }

    // private methods
    private AuthorityPersonPayload persist() {
        try {
            PrincipalProfile profile = persist(makePrincipalProfile(null));
            AuthorityPerson person = makeCleanAuthorityPerson(0);
            if (person instanceof FakeAuthorityPerson fake) {
                fake.setProfileId(profile.getId());
            }
            return payloadMapper.toPayload(persist(person));
        } finally {
            reset(payloadMapper);
        }
    }

    private AuthorityPerson persist(AuthorityPerson newInstance) {
        AuthorityPersonEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private void setPersonPermissions(AuthorityPersonPayload person, String username, String password) {
        PrincipalProfileEntity profile = findEntity(PrincipalProfileEntity.class, person.getProfileId());
        profile.setUsername(username);
        try {
            profile.setSignature(profile.makeSignatureFor(password));
            merge(profile);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
        } finally {
            reset(persistence);
        }
    }

    private void merge(PrincipalProfile instance) {
        PrincipalProfileEntity entity = instance instanceof PrincipalProfileEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }
    }

    private PrincipalProfile persist(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }
}