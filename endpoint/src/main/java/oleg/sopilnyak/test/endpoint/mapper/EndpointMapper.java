package oleg.sopilnyak.test.endpoint.mapper;

import oleg.sopilnyak.test.endpoint.dto.CourseDto;
import oleg.sopilnyak.test.endpoint.dto.StudentDto;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;

import static org.mapstruct.NullValueCheckStrategy.ALWAYS;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

/**
 * MapStruct Mapper: To convert model types to dtos
 */
@Mapper(nullValuePropertyMappingStrategy = IGNORE, nullValueCheckStrategy = ALWAYS)
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
     */
    @Mapping(expression = "java(null)", target = "students")
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
     */
    @Mapping(expression = "java(null)", target = "courses")
    StudentDto toDtoShort(Student student);

    @Named("toCourseDtos")
    default List<Course> toCoursesDto(List<Course> courses) {
        return courses == null ? Collections.emptyList() : courses.stream().map(course -> (Course) toDtoShort(course)).toList();
    }

    @Named("toStudentDtos")
    default List<Student> toStudentsDto(List<Student> students) {
        return students == null ? Collections.emptyList() : students.stream().map(student -> (Student) toDtoShort(student)).toList();
    }

}
