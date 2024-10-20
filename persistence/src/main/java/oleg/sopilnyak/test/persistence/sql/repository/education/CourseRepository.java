package oleg.sopilnyak.test.persistence.sql.repository.education;

import oleg.sopilnyak.test.persistence.sql.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Set<CourseEntity> findCourseEntitiesByStudentSetId(Long studentId);

    Set<CourseEntity> findCourseEntitiesByStudentSetEmpty();

}