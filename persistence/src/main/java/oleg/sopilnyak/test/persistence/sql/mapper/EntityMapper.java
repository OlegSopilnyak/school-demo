package oleg.sopilnyak.test.persistence.sql.mapper;

import oleg.sopilnyak.test.persistence.sql.entity.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

import static org.mapstruct.NullValueCheckStrategy.ALWAYS;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

/**
 * Mapper to transform model-types to entities
 */
@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = IGNORE,
        nullValueCheckStrategy = ALWAYS,
        builder = @Builder(disableBuilder = true)
)
public interface EntityMapper {
    /**
     * Convert model-type to Entity
     *
     * @param course instance to convert
     * @return Entity instance
     */
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentEntities")
    CourseEntity toEntity(Course course);

    /**
     * Convert model-type to DTO
     *
     * @param student instance to convert
     * @return DTO instance
     */
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCourseEntities")
    StudentEntity toEntity(Student student);

    @Named("toCourseEntities")
    default List<Course> toCourses(List<Course> courses) {
        return courses == null ? Collections.emptyList() : courses.stream().map(course -> (Course) toEntity(course)).toList();
    }

    @Named("toStudentEntities")
    default List<Student> toStudents(List<Student> students) {
        return students == null ? Collections.emptyList() : students.stream().map(student -> (Student) toEntity(student)).toList();
    }

}

