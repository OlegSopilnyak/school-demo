package oleg.sopilnyak.test.service.mapper;

import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.school.common.model.base.BaseType;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.service.message.*;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mapstruct.NullValueCheckStrategy.ALWAYS;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * MapStruct Mapper: To convert model types to Payloads
 */
@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = IGNORE,
        nullValueCheckStrategy = ALWAYS,
        builder = @Builder(disableBuilder = true)
)
public interface BusinessMessagePayloadMapper {
    default BaseType toPayload(BaseType baseType) {
        throw new UnsupportedOperationException("Cannot convert to payload for type:" + baseType.getClass().getSimpleName());
    }

    /**
     * Convert model-type to Payload
     *
     * @param course instance to convert
     * @return Payload instance
     */
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentPayloads")
    @Mapping(target = "original", expression = "java(course)")
    CoursePayload toPayload(Course course);

    /**
     * Convert model-type to Payload without  students-list (to avoid recursion)
     *
     * @param course instance to convert
     * @return Payload instance
     * @see #toCoursesPayload(List)
     */
    @Named("toShortCourse")
    @Mapping(target = "students", expression = "java(null)")
    CoursePayload toPayloadShort(Course course);

    /**
     * Convert model-type to Payload
     *
     * @param student instance to convert
     * @return Payload instance
     */
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCoursePayloads")
    @Mapping(target = "original", expression = "java(student)")
    StudentPayload toPayload(Student student);

    /**
     * Convert model-type to Payload without  courses-list (to avoid recursion)
     *
     * @param student instance to convert
     * @return Payload instance
     * @see StudentsGroup#getLeader()
     * @see #toPayload(StudentsGroup)
     * @see #toStudentsPayload(List)
     */
    @Named("toShortStudent")
    @Mapping(target = "courses", expression = "java(null)")
    StudentPayload toPayloadShort(Student student);

    /**
     * Convert model-type to Payload
     *
     * @param person instance to convert
     * @return Payload instance
     */
    @Mapping(source = "faculties", target = "faculties", qualifiedByName = "toFacultyPayloads")
    @Mapping(target = "original", expression = "java(person)")
    AuthorityPersonPayload toPayload(AuthorityPerson person);

    /**
     * Convert model-type to Payload
     *
     * @param faculty instance to convert
     * @return Payload instance
     */
    @Mapping(target = "dean", expression = "java(null)")
    @Mapping(source = "courses", target = "courses", qualifiedByName = "toCoursePayloads")
    @Mapping(target = "original", expression = "java(faculty)")
    FacultyPayload toPayload(Faculty faculty);

    /**
     * Convert model-type to Payload without courses
     *
     * @param faculty instance to convert
     * @return Payload instance
     * @see #toFacultyPayload(List)
     */
    @Named("toShortFaculty")
    @Mapping(target = "dean", expression = "java(null)")
    @Mapping(target = "courses", expression = "java(null)")
    FacultyPayload toPayloadShort(Faculty faculty);

    /**
     * Convert model-type to Payload
     *
     * @param group instance to convert
     * @return Payload instance
     */
    @Mapping(source = "students", target = "students", qualifiedByName = "toStudentPayloads")
    @Mapping(source = "leader", target = "leader", dependsOn = "students", qualifiedByName = "toShortStudent")
    @Mapping(target = "original", expression = "java(group)")
    StudentsGroupPayload toPayload(StudentsGroup group);

    default PersonProfile toPayload(PersonProfile profile) {
        if (profile instanceof StudentProfile studentProfile) {
            return toPayload(studentProfile);
        } else if (profile instanceof PrincipalProfile principalProfileProfile) {
            return toPayload(principalProfileProfile);
        } else {
            throw new UnsupportedOperationException("Cannot convert to payload for type:" + profile.getClass().getSimpleName());
        }
    }

    /**
     * Convert model-type to Payload
     *
     * @param profile instance to convert
     * @return Payload instance
     */
    @Mapping(source = "profile", target = "extras", qualifiedByName = "toProfileExtras")
    @Mapping(target = "original", expression = "java(profile)")
    StudentProfilePayload toPayload(StudentProfile profile);

    /**
     * Convert model-type to Payload
     *
     * @param profile instance to convert
     * @return Payload instance
     */
    @Mapping(source = "profile", target = "extras", qualifiedByName = "toProfileExtras")
    @Mapping(target = "original", expression = "java(profile)")
    PrincipalProfilePayload toPayload(PrincipalProfile profile);

    @Named("toProfileExtras")
    default BaseProfilePayload.Extra[] toProfileExtras(PersonProfile profile) {
        return isEmpty(profile.getExtraKeys()) ? emptyExtraKeys() : copyExtraKeys(profile);
    }

    @Named("toCoursePayloads")
    default List<Course> toCoursesPayload(List<Course> courses) {
        return courses == null ? Collections.emptyList() : courses.stream().map(course -> (Course) toPayloadShort(course)).toList();
    }

    @Named("toStudentPayloads")
    default List<Student> toStudentsPayload(List<Student> students) {
        return students == null ? Collections.emptyList() : students.stream().map(student -> (Student) toPayloadShort(student)).toList();
    }

    @Named("toFacultyPayloads")
    default List<Faculty> toFacultyPayload(List<Faculty> faculties) {
        return faculties == null ? Collections.emptyList() : faculties.stream().map(faculty -> (Faculty) toPayloadShort(faculty)).toList();
    }

    // private methods
    private BaseProfilePayload.Extra[] emptyExtraKeys() {
        return new BaseProfilePayload.Extra[0];
    }

    private BaseProfilePayload.Extra[] copyExtraKeys(PersonProfile profile) {
        return Arrays.stream(profile.getExtraKeys())
                .filter(key -> profile.getExtra(key).isPresent())
                .map(key -> new BaseProfilePayload.Extra(key, profile.getExtra(key).orElse(null)))
                .toList().toArray(BaseProfilePayload.Extra[]::new);
    }
}
