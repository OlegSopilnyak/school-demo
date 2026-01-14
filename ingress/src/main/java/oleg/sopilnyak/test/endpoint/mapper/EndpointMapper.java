package oleg.sopilnyak.test.endpoint.mapper;

import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.dto.BaseProfileDto;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import org.mapstruct.*;

import java.util.Arrays;
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

    /**
     * Convert model-type to DTO
     *
     * @param profile instance to convert
     * @return DTO instance
     */
    @Mapping(source = "profile", target = "extras", qualifiedByName = "toProfileExtras")
    StudentProfileDto toDto(StudentProfile profile);

    /**
     * Convert model-type to DTO
     *
     * @param profile instance to convert
     * @return DTO instance
     */
    @Mapping(source = "profile", target = "extras", qualifiedByName = "toProfileExtras")
    PrincipalProfileDto toDto(PrincipalProfile profile);

    @Named("toProfileExtras")
    default BaseProfileDto.Extra[] toProfileExtras(PersonProfile profile) {
        return Arrays.stream(profile.getExtraKeys())
                .filter(key -> profile.getExtra(key).isPresent())
                .map(key -> new BaseProfileDto.Extra(key, profile.getExtra(key).orElseThrow()))
                .toList().toArray(new BaseProfileDto.Extra[0]);
    }

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
