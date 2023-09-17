package oleg.sopilnyak.test.persistence.sql.mapper;

import oleg.sopilnyak.test.persistence.sql.entity.*;
import oleg.sopilnyak.test.school.common.model.*;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

import static org.mapstruct.NullValueCheckStrategy.ALWAYS;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

/**
 * Mapper to transform school's model-types to entities
 */
@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = IGNORE,
        nullValueCheckStrategy = ALWAYS,
        builder = @Builder(disableBuilder = true)
)
public interface SchoolEntityMapper {
    /**
     * Convert model-type to Entity<BR/>Set ManyToOne field to null
     *
     * @param course instance to convert
     * @return Entity instance
     * @see FacultyEntity#setCourses(List)
     */
    @Mapping(target = "faculty", expression = "java(null)")
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentEntities")
    CourseEntity toEntity(Course course);

    /**
     * Convert model-type to Entity<BR/>Set ManyToOne field to null
     *
     * @param student instance to convert
     * @return Entity instance
     * @see StudentsGroupEntity#setStudents(List)
     */
    @Named("toStudentEntity")
    @Mapping(target = "group", expression = "java(null)")
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCourseEntities")
    StudentEntity toEntity(Student student);

    /**
     * Convert model-type to DTO
     *
     * @param person instance to convert
     * @return DTO instance
     */
    @Named("toAuthorityPersonEntity")
    @Mapping(source = "faculties", target = "faculties", qualifiedByName = "toFacultyEntities")
    AuthorityPersonEntity toEntity(AuthorityPerson person);

    /**
     * Convert model-type to Entity<BR/>Set ManyToOne field to null
     *
     * @param faculty instance to convert
     * @return Entity instance
     * @see AuthorityPersonEntity#setFaculties(List)
     */
    @Named("toFacultyEntity")
    @Mapping(target = "dean", expression = "java(null)")
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCourseEntities")
    FacultyEntity toEntity(Faculty faculty);

    /**
     * Convert model-type to Entity
     *
     * @param group instance to convert
     * @return Entity instance
     */
    @Mappings({
            @Mapping(target = "leader", source = "leader", qualifiedByName = "toStudentEntity", dependsOn = "students"),
            @Mapping(source = "students", target = "students", qualifiedByName = "toStudentEntities")
    })
    StudentsGroupEntity toEntity(StudentsGroup group);

    @Named("toCourseEntities")
    default List<Course> toCourses(List<Course> courses) {
        return courses == null ? Collections.emptyList() : courses.stream().map(course -> (Course) toEntity(course)).toList();
    }

    @Named("toStudentEntities")
    default List<Student> toStudents(List<Student> students) {
        return students == null ? Collections.emptyList() : students.stream().map(student -> (Student) toEntity(student)).toList();
    }

    @Named("toFacultyEntities")
    default List<Faculty> toFaculties(List<Faculty> faculties) {
        return faculties == null ? Collections.emptyList() : faculties.stream().map(faculty -> (Faculty) toEntity(faculty)).toList();
    }

}

