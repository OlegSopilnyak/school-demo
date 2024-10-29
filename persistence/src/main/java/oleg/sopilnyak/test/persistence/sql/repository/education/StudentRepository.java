package oleg.sopilnyak.test.persistence.sql.repository.education;

import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    Set<StudentEntity> findStudentEntitiesByCourseSetId(Long courseId);

    Set<StudentEntity> findStudentEntitiesByCourseSetEmpty();
}
