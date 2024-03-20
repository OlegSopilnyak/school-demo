package oleg.sopilnyak.test.persistence.sql.repository;

import oleg.sopilnyak.test.persistence.sql.entity.PersonProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonProfileRepository<T extends PersonProfileEntity> extends JpaRepository<T, Long> {
}
