package oleg.sopilnyak.test.endpoint.mapper;

import oleg.sopilnyak.test.endpoint.dto.*;
import oleg.sopilnyak.test.school.common.model.*;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

import static org.mapstruct.NullValueCheckStrategy.ALWAYS;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

/**
 * MapStruct Mapper: To convert model types to DTOs
 */
@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = IGNORE,
        nullValueCheckStrategy = ALWAYS,
        builder = @Builder(disableBuilder = true)
)
public interface EndpointMapper {
    /**
     * Convert model-type to DTO
     *
     * @param course instance to convert
     * @return DTO instance
     */
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentDtos")
    CourseDto toDto(Course course);

    /**
     * Convert model-type to DTO without  students-list (to avoid recursion)
     *
     * @param course instance to convert
     * @return DTO instance
     * @see #toCoursesDto(List)
     */
    @Named("toShortCourse")
    @Mapping(target = "students", expression = "java(null)")
    CourseDto toDtoShort(Course course);

    /**
     * Convert model-type to DTO
     *
     * @param student instance to convert
     * @return DTO instance
     */
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCourseDtos")
    StudentDto toDto(Student student);
    /**
     * Convert model-type to DTO without  courses-list (to avoid recursion)
     *
     * @param student instance to convert
     * @return DTO instance
     * @see StudentsGroup#getLeader()
     * @see #toDto(StudentsGroup)
     * @see #toStudentsDto(List)
     */
    @Named("toShortStudent")
    @Mapping(target = "courses", expression = "java(null)")
    StudentDto toDtoShort(Student student);

    /**
     * Convert model-type to DTO
     *
     * @param person instance to convert
     * @return DTO instance
     */
    @Mapping(source = "faculties", target = "faculties", qualifiedByName = "toFacultyDtos")
    AuthorityPersonDto toDto(AuthorityPerson person);

    /**
     * Convert model-type to DTO
     *
     * @param faculty instance to convert
     * @return DTO instance
     */
    @Mapping(target = "dean", expression = "java(null)")
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCourseDtos")
    FacultyDto toDto(Faculty faculty);

    /**
     * Convert model-type to DTO without courses
     *
     * @param faculty instance to convert
     * @return DTO instance
     * @see #toFacultyDto(List)
     */
    @Named("toShortFaculty")
    @Mapping(target = "dean", expression = "java(null)")
    @Mapping(target = "courses", expression = "java(null)")
    FacultyDto toDtoShort(Faculty faculty);

    /**
     * Convert model-type to DTO
     *
     * @param group instance to convert
     * @return DTO instance
     */
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentDtos")
    @Mapping(source = "leader", target = "leader", dependsOn = "students", qualifiedByName = "toShortStudent")
    StudentsGroupDto toDto(StudentsGroup group);

    @Named("toCourseDtos")
    default List<Course> toCoursesDto(List<Course> courses) {
        return courses == null ? Collections.emptyList() : courses.stream().map(course -> (Course) toDtoShort(course)).toList();
    }

    @Named("toStudentDtos")
    default List<Student> toStudentsDto(List<Student> students) {
        return students == null ? Collections.emptyList() : students.stream().map(student -> (Student) toDtoShort(student)).toList();
    }

    @Named("toFacultyDtos")
    default List<Faculty> toFacultyDto(List<Faculty> faculties) {
        return faculties == null ? Collections.emptyList() : faculties.stream().map(faculty -> (Faculty) toDtoShort(faculty)).toList();
    }
}
