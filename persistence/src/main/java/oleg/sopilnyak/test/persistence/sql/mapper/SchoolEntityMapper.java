package oleg.sopilnyak.test.persistence.sql.mapper;

import oleg.sopilnyak.test.persistence.sql.entity.*;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
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
    @Named("toCourseEntity")
    @Mapping(target = "faculty", expression = "java(null)")
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentEntities", dependsOn = "id")
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
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCourseEntities", dependsOn = "id")
    StudentEntity toEntity(Student student);

    /**
     * Convert model-type to DTO
     *
     * @param person instance to convert
     * @return DTO instance
     * @see AuthorityPersonEntity#setFaculties(List)
     */
    @Named("toAuthorityPersonEntity")
    @Mapping(source = "faculties", target = "faculties", qualifiedByName = "toFacultyEntities")
    AuthorityPersonEntity toEntity(AuthorityPerson person);

    /**
     * Convert model-type to Entity<BR/>Set ManyToOne field to null
     *
     * @param faculty instance to convert
     * @return Entity instance
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
    @Mapping(target = "leader", source = "leader", qualifiedByName = "toStudentEntity", dependsOn = "students")
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentEntities")
    StudentsGroupEntity toEntity(StudentsGroup group);

    /**
     * Convert model-type to Entity
     *
     * @param profile instance to convert
     * @return Entity instance
     */
    @Mapping(source = "profile", target = "extras", qualifiedByName = "toProfileExtraMap")
    StudentProfileEntity toEntity(StudentProfile profile);

    /**
     * Convert model-type to Entity
     *
     * @param profile instance to convert
     * @return Entity instance
     */
    @Mapping(source = "profile", target = "extras", qualifiedByName = "toProfileExtraMap")
    PrincipalProfileEntity toEntity(PrincipalProfile profile);

    @Named("toCourseEntities")
    default List<Course> toCourses(final List<Course> courses) {
        return isNull(courses) ? List.of() : courses.stream().map(course -> (Course) toEntityOnly(course)).toList();
    }

    @Mapping(target = "students", ignore = true)
    @Mapping(target = "studentSet", expression = "java(null)")
    CourseEntity toEntityOnly(Course course);

    @Named("toStudentEntities")
    default List<Student> toStudents(List<Student> students) {
        return isNull(students) ? List.of() : students.stream().map(student -> (Student) toEntityOnly(student)).toList();
    }

    @Mapping(target = "courses", ignore = true)
    @Mapping(target = "courseSet", expression = "java(null)")
    StudentEntity toEntityOnly(Student student);

    @Named("toFacultyEntities")
    default List<Faculty> toFaculties(List<Faculty> faculties) {
        return isNull(faculties) ? List.of() : faculties.stream().map(faculty -> (Faculty) toEntity(faculty)).toList();
    }

    @Named("toProfileExtraMap")
    default Map<String, String> toProfileExtraMap(PersonProfile profile) {
        return Arrays.stream(profile.getExtraKeys())
                .filter(key -> profile.getExtra(key).isPresent())
                .collect(Collectors.toMap(
                        extraKey -> extraKey, extraKey -> profile.getExtra(extraKey).orElse("unknown"),
                        (existingValue, replacementValue) -> existingValue,
                        HashMap::new
                ));
    }

}

