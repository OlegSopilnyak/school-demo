package oleg.sopilnyak.test.persistence.sql.repository;

import oleg.sopilnyak.test.persistence.sql.entity.AuthorityPersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityPersonRepository extends JpaRepository<AuthorityPersonEntity, Long> {
}
