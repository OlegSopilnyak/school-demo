package oleg.sopilnyak.test.persistence.sql.repository.organization;

import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyRepository extends JpaRepository<FacultyEntity, Long> {
}
