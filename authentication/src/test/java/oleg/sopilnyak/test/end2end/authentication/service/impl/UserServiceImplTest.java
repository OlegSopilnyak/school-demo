package oleg.sopilnyak.test.end2end.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.configuration.SchoolAuthenticationConfiguration;
import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.authentication.service.impl.AccessTokensStorageImpl;
import oleg.sopilnyak.test.authentication.service.impl.UserServiceImpl;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;

import jakarta.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
        SchoolAuthenticationConfiguration.class,
        PersistenceConfiguration.class//,
//        TestConfig.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class UserServiceImplTest extends MysqlTestModelFactory {
    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    AccessTokensStorage accessTokensStorage;
    @MockitoSpyBean
    @Autowired
    UserService service;

    String username = "username";

    @AfterEach
    void tearDown() {
        deleteEntities(PrincipalProfileEntity.class);
        deleteEntities(AuthorityPersonEntity.class);
    }

    @Test
    void checkAssociatedServices() {
        assertThat(entityMapper).isNotNull();
        assertThat(persistenceFacade).isNotNull();
        assertThat(accessTokensStorage).isNotNull().isInstanceOf(AccessTokensStorageImpl.class);
        assertThat(service).isNotNull().isInstanceOf(UserServiceImpl.class);
    }

    @Test
    void shouldPrepareUserDetails() throws NoSuchAlgorithmException {
        String password = "password";
        PrincipalProfileEntity profileEntity = persist(makePrincipalProfile(null));
        profileEntity.setUsername(username);
        profileEntity.setSignature(profileEntity.makeSignatureFor(password));
        profileEntity.setRole(Role.SUPPORT_STAFF);
        profileEntity.setPermissions(Set.of(Permission.EDU_GET));
        merge(profileEntity);
        Long profileId = profileEntity.getId();
        AuthorityPersonEntity personEntity = persist(makeCleanAuthorityPerson(0));
        personEntity.setProfileId(profileId);
        merge(personEntity);
        Long personId = personEntity.getId();
        Set<String> authorities = Set.of("ROLE_SUPPORT_STAFF", "EDU_GET");

        UserDetailsEntity result = service.prepareUserDetails(username, password);

        // check the result
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo(password);
        assertThat(result.getId()).isEqualTo(personId);
        result.getAuthorities().forEach(granted -> assertThat(authorities).contains(granted.getAuthority()));
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(persistenceFacade).findAuthorityPersonByProfileId(profileId);
    }

    @Test
    void shouldNotPrepareUserDetails_NoProfileByUsername() {
        String password = "password";

        var result = assertThrows(Exception.class, () -> service.prepareUserDetails(username, password));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(UsernameNotFoundException.class);
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(persistenceFacade, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldNotPrepareUserDetails_WrongPasswordInProfile() {
        String password = "password";
        PrincipalProfileEntity profileEntity = persist(makePrincipalProfile(null));
        profileEntity.setUsername(username);
        profileEntity.setRole(Role.SUPPORT_STAFF);
        profileEntity.setPermissions(Set.of(Permission.EDU_GET));
        merge(profileEntity);

        var result = assertThrows(Exception.class, () -> service.prepareUserDetails(username, password));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(SchoolAccessDeniedException.class);
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(persistenceFacade, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldNotPrepareUserDetails_NoPersonForProfile() throws NoSuchAlgorithmException {
        String password = "password";
        PrincipalProfileEntity profileEntity = persist(makePrincipalProfile(null));
        profileEntity.setUsername(username);
        profileEntity.setSignature(profileEntity.makeSignatureFor(password));
        profileEntity.setRole(Role.SUPPORT_STAFF);
        profileEntity.setPermissions(Set.of(Permission.EDU_GET));
        merge(profileEntity);
        Long profileId = profileEntity.getId();

        var result = assertThrows(Exception.class, () -> service.prepareUserDetails(username, password));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(UsernameNotFoundException.class);
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(persistenceFacade).findAuthorityPersonByProfileId(profileId);
    }

    // inner methods
    private AuthorityPersonEntity persist(AuthorityPerson newInstance) {
        AuthorityPersonEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private void merge(AuthorityPerson instance) {
        AuthorityPersonEntity entity = instance instanceof AuthorityPersonEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }
    }

    private PrincipalProfileEntity persist(PrincipalProfile newInstance) {
        PrincipalProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private void merge(PrincipalProfile instance) {
        PrincipalProfileEntity entity = instance instanceof PrincipalProfileEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }
    }
}