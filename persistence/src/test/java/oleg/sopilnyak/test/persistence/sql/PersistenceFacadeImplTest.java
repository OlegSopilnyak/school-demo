package oleg.sopilnyak.test.persistence.sql;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.AuthorityPersonRepository;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class PersistenceFacadeImplTest extends MysqlTestModelFactory {
    @SpyBean
    @Autowired
    private AuthorityPersonRepository authorityPersonRepository;
    @SpyBean
    @Autowired
    private PersonProfileRepository<PersonProfileEntity> personProfileRepository;
    @Autowired
    PersistenceFacade facade;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldLoadContext() {
        assertThat(facade).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldInitDefaultDataset() {

        facade.initDefaultDataset();

        {
            long profileId = facade.findPersonProfileByLogin("bill").orElseThrow().getId();
            assertThat(facade.findAuthorityPersonById(profileId)).isPresent();
        }
        {
            long profileId = facade.findPersonProfileByLogin("hillary").orElseThrow().getId();
            assertThat(facade.findAuthorityPersonById(profileId)).isPresent();
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateAuthorityPersonAccess() throws NoSuchAlgorithmException {
        PrincipalProfile profile = makePrincipalProfile(null);
        Optional<PrincipalProfile> saved = facade.save(profile);
        assertThat(saved).isPresent();
        assertThat(saved.get().getId()).isNotNull();
        long profileId = saved.get().getId();
        AuthorityPerson person = makeCleanAuthorityPerson(0);
        if (person instanceof FakeAuthorityPerson fakeAuthorityPerson) {
            fakeAuthorityPerson.setProfileId(profileId);
        } else {
            fail("Unexpected profile id: " + profileId);
        }
        long personId = facade.save(person).orElseThrow().getId();

        assertThat(facade.updateAuthorityPersonAccess(personId, "username", "password")).isTrue();

        PrincipalProfileEntity updated = facade.findPrincipalProfileById(profileId).map(PrincipalProfileEntity.class::cast).orElseThrow();
        String signature = PrincipalProfileEntity.builder().login("username").build().makeSignatureFor("password");
        assertThat(updated.getSignature()).isEqualTo(signature);
        verify(authorityPersonRepository).findById(personId);
        verify(personProfileRepository, times(2)).findById(profileId);
        verify(personProfileRepository, times(2)).saveAndFlush(updated);
    }
}
