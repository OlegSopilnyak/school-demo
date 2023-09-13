package oleg.sopilnyak.test.persistence.sql.repository;

import oleg.sopilnyak.test.persistence.sql.entity.FacultyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyRepository extends JpaRepository<FacultyEntity, Long> {
}
