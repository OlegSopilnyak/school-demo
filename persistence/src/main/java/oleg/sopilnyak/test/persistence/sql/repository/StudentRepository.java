package oleg.sopilnyak.test.persistence.sql.repository;

import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    Set<StudentEntity> findStudentEntitiesByCourseSetId(Long courseId);

    Set<StudentEntity> findStudentEntitiesByCourseSetEmpty();
}
