package oleg.sopilnyak.test.persistence.sql.repository.organization;

import oleg.sopilnyak.test.persistence.sql.entity.StudentsGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentsGroupRepository extends JpaRepository<StudentsGroupEntity, Long> {
}
