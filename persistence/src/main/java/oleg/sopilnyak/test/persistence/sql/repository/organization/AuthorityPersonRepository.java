package oleg.sopilnyak.test.persistence.sql.repository.organization;

import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityPersonRepository extends JpaRepository<AuthorityPersonEntity, Long> {
    Optional<AuthorityPersonEntity> findByProfileId(Long profileId);
}
