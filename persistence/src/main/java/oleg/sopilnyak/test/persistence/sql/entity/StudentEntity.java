package oleg.sopilnyak.test.persistence.sql.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"courseSet", "group"})
@ToString(exclude = {"courseSet", "group"})

@Entity
@Table(name = "students")
public class StudentEntity implements Student {
    private static SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private Long profileId;
    private String firstName;
    private String lastName;
    private String gender;
    private String description;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(name = "student_course",
            joinColumns = {@JoinColumn(name = "fk_student")},
            inverseJoinColumns = {@JoinColumn(name = "fk_course")}
    )
    private Set<CourseEntity> courseSet;
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private StudentsGroupEntity group;

    /**
     * To get the list of courses, the student is registered to
     *
     * @return list of courses (ordered by course::id)
     */
    @Override
    public List<Course> getCourses() {
        refreshCourseSet();
        return getCourseSet().stream()
                .map(Course.class::cast)
                .sorted(Comparator.comparing(Course::getName))
                .toList();
    }

    /**
     * To replace the student's courses list by new one
     *
     * @param courses new student's courses list
     */
    public void setCourses(List<Course> courses) {
        refreshCourseSet();
        new HashSet<>(getCourseSet()).forEach(this::remove);
        courses.forEach(this::add);
    }

    /**
     * Register the student to the new course
     *
     * @param course new student's course
     * @return true if success
     */
    public boolean add(Course course) {
        refreshCourseSet();
        final Set<CourseEntity> courseEntities = getCourseSet();
        final boolean isExistsCourse =
                courseEntities.stream().anyMatch(ce -> equals(ce, course));

        if (isExistsCourse) {
            // course exists
            return false;
        }

        final CourseEntity courseToAdd = course instanceof CourseEntity ce ? ce : mapper.toEntity(course);
        refresh(courseToAdd);
        courseEntities.add(courseToAdd);
        courseToAdd.getStudentSet().add(this);
        return true;
    }

    /**
     * Unregister the student from the course
     *
     * @param course course to remove
     * @return true if success
     */
    public boolean remove(Course course) {
        refreshCourseSet();
        final Set<CourseEntity> courseEntities = getCourseSet();
        final CourseEntity existsCourse =
                courseEntities.stream().filter(ce -> equals(ce, course)).findFirst().orElse(null);

        if (isNull(existsCourse)) {
            // course does not exist
            return false;
        }

        if (courseEntities.removeIf(c -> c == existsCourse)) {
            existsCourse.getStudentSet().removeIf(s -> s == this);
        }
        return true;
    }

    private void refreshCourseSet() {
        if (isNull(courseSet)) courseSet = new HashSet<>();
    }

    private static void refresh(final CourseEntity course) {
        if (isNull(course.getStudentSet())) course.setStudentSet(new HashSet<>());
    }

    private static boolean equals(Course first, Course second) {
        return !isNull(first) && !isNull(second) &&
                equals(first.getName(), second.getName()) &&
                equals(first.getDescription(), second.getDescription())
                ;
    }

    private static boolean equals(String first, String second) {
        return isNull(first) ? isNull(second) : first.equals(second);
    }

}
