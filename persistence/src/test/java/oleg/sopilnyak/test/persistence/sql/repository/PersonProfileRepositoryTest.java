package oleg.sopilnyak.test.persistence.sql.repository;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class PersonProfileRepositoryTest extends MysqlTestModelFactory {
    @Autowired
    PersonProfileRepository repository;

    @Test
    void shouldBeConnectedRepository() {
        assertThat(repository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreateStudentProfileEntity() {
        StudentProfileEntity profile = StudentProfileEntity.builder()
                .photoUrl("photo-url").email("e-mail").phone("phone").location("location")
                .extras(Map.of("key1", "1", "key2", "2"))
                .build();

        repository.saveAndFlush(profile);

        Optional<StudentProfileEntity> entity = repository.findById(profile.getId());
        assertThat(entity).contains(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteStudentProfileEntity() {
        StudentProfileEntity profile = StudentProfileEntity.builder()
                .photoUrl("photo-url").email("e-mail").phone("phone").location("location")
                .extras(Map.of("key-1", "1", "key-2", "2"))
                .build();
        repository.saveAndFlush(profile);
        Long id = profile.getId();
        assertThat(repository.findById(id)).isNotEmpty();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldUpdateStudentProfileExtras() {
        StudentProfileEntity profile = StudentProfileEntity.builder()
                .photoUrl("photo-url").email("e-mail").phone("phone").location("location")
                .extras(new HashMap<>(Map.of("key-1", "1", "key-2", "2")))
                .build();
        repository.saveAndFlush(profile);
        Long id = profile.getId();
        StudentProfileEntity entity = (StudentProfileEntity) repository.findById(id).orElse(null);
        assertThat(entity).isEqualTo(profile);
        StudentProfileEntity another = StudentProfileEntity.builder()
                .id(id).photoUrl("photo-url-2").email("e-mail-2").phone("phone-2").location("location-2")
                .extras(new HashMap<>(Map.of("key-3", "10", "key-4", "20")))
                .build();
        repository.saveAndFlush(another);

        entity = (StudentProfileEntity) repository.findById(id).orElse(null);

        assertThat(entity).isEqualTo(profile).isEqualTo(another);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldCreatePrincipalProfileEntity() {
        PrincipalProfileEntity profile = PrincipalProfileEntity.builder()
                .login("login")
                .photoUrl("photo-url").email("e-mail").phone("phone").location("location")
                .extras(Map.of("key1", "1", "key2", "2"))
                .build();

        repository.saveAndFlush(profile);

        Optional<PrincipalProfileEntity> entity = repository.findById(profile.getId());
        assertThat(entity).contains(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldGetPrincipalProfileByLogin() {
        PrincipalProfileEntity profile = PrincipalProfileEntity.builder()
                .login("login").photoUrl("photo-url").email("e-mail").phone("phone").location("location")
                .extras(Map.of("key1", "1", "key2", "2")).build();
        repository.saveAndFlush(profile);
        assertThat(repository.findById(profile.getId())).contains(profile);

        assertThat(repository.findByLogin("login")).contains(profile);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeletePrincipalProfileEntity() {
        PrincipalProfileEntity profile = PrincipalProfileEntity.builder()
                .login("login")
                .photoUrl("photo-url").email("e-mail").phone("phone").location("location")
                .extras(Map.of("key1", "1", "key2", "2"))
                .build();
        repository.saveAndFlush(profile);
        Long id = profile.getId();
        assertThat(repository.findById(id)).isNotEmpty();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
    }
}