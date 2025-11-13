package oleg.sopilnyak.test.persistence.sql.repository;

import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.repository.organization.AuthorityPersonRepository;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@Rollback
class AuthorityPersonRepositoryTest extends MysqlTestModelFactory {
    @Autowired
    AuthorityPersonRepository repository;

    @Test
    void shouldBeConnectedRepository() {
        assertThat(repository).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindAll() {
        AuthorityPersonEntity person1 = createAuthorityPersonEntity(1);
        AuthorityPersonEntity person2 = createAuthorityPersonEntity(2);
        repository.saveAllAndFlush(List.of(person1, person2));
        assertThat(repository.findById(person1.getId())).isNotEmpty();
        assertThat(repository.findById(person2.getId())).isNotEmpty();

        List<AuthorityPerson> staff = repository.findAll().stream().map(AuthorityPerson.class::cast)
                .sorted(Comparator.comparing(AuthorityPerson::getFirstName)).toList();

        assertThat(staff).isNotEmpty();
        assertAuthorityPersonLists(staff, List.of(person1, person2), false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontFindAll() {

        List<AuthorityPerson> staff = repository.findAll().stream().map(AuthorityPerson.class::cast)
                .sorted(Comparator.comparing(AuthorityPerson::getFirstName)).toList();

        assertThat(staff).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldFindById() {
        AuthorityPersonEntity person = createAuthorityPersonEntity(1);
        repository.saveAndFlush(person);
        assertThat(repository.findById(person.getId())).isNotEmpty();

        AuthorityPerson saved = repository.findById(person.getId()).orElse(null);

        assertThat(saved).isNotNull();
        assertAuthorityPersonEquals(saved, person, false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDontFindById() {

        AuthorityPerson saved = repository.findById(100L).orElse(null);

        assertThat(saved).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldSave() {
        AuthorityPersonEntity person = createAuthorityPersonEntity(1);

        AuthorityPersonEntity saved = repository.saveAndFlush(person);

        assertThat(repository.findById(person.getId())).isNotEmpty();
        assertAuthorityPersonEquals(saved, person, false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotSave() {
        AuthorityPersonEntity person = createAuthorityPersonEntity(1);
        person.setId(100L);

        AuthorityPersonEntity saved = repository.saveAndFlush(person);

        assertThat(person.getId()).isNotEqualTo(saved.getId());
        assertThat(repository.findById(person.getId())).isEmpty();
        assertAuthorityPersonEquals(saved, person, false);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldDeleteById() {
        AuthorityPersonEntity person = createAuthorityPersonEntity(1);
        repository.saveAndFlush(person);
        Long id = person.getId();
        assertThat(repository.findById(id)).isNotEmpty();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldNotDeleteById() {
        Long id = 101L;

        try {
            repository.deleteById(id);
        } catch (Exception e) {
            fail("Exception should not have been thrown instead of deleted data not found", e);
        }
    }

    private static AuthorityPersonEntity createAuthorityPersonEntity(int order) {
        CourseEntity course = CourseEntity.builder()
                .name("name-" + order)
                .description("description-" + order)
                .build();
        FacultyEntity faculty = FacultyEntity.builder()
                .name("name-" + order)
                .courseEntitySet(Set.of(course))
                .build();
        return AuthorityPersonEntity.builder()
                .title("title-" + order)
                .firstName("first-name-" + order)
                .lastName("last-name-" + order)
                .gender("gender-" + order)
                .facultyEntitySet(Set.of(faculty))
                .build();
    }

}